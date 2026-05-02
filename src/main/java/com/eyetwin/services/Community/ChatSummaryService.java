package com.eyetwin.services.Community;

import com.eyetwin.config.AISummaryConfig;
import com.eyetwin.entities.Community.ChannelReadState;
import com.eyetwin.entities.Community.ChatSummaryResult;
import com.eyetwin.entities.Community.Message;
import com.eyetwin.entities.Community.MessageAttachment;
import com.eyetwin.tools.DatabaseConfig;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChatSummaryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MessageServiceImpl messageService = new MessageServiceImpl();
    private final ChannelReadStateService readStateService = new ChannelReadStateService();

    private OllamaChatSummaryClient ollamaClient;

    private Connection getConnection() {
        return DatabaseConfig.getConnection();
    }

    private OllamaChatSummaryClient getOllamaClient() {
        if (ollamaClient == null) {
            ollamaClient = new OllamaChatSummaryClient();
        }
        return ollamaClient;
    }

    public int getMissedCount(int userId, int channelId) throws SQLException {
        ChannelReadState state = readStateService.findByUserAndChannel(userId, channelId);
        int lastSeenId = state != null && state.getLastSeenMessageId() != null ? state.getLastSeenMessageId() : 0;
        return messageService.countMessagesAfter(channelId, lastSeenId);
    }

    public void markSeenUpToLatest(int userId, int channelId) throws SQLException {
        Message latest = messageService.findLatestMessageInChannel(channelId);
        Integer latestId = latest != null ? latest.getId() : null;
        readStateService.upsertLastSeen(userId, channelId, latestId);
    }

    public ChatSummaryResult summarizeMissedMessages(int userId, int channelId, String channelName) throws Exception {
        ChannelReadState state = readStateService.findByUserAndChannel(userId, channelId);
        int lastSeenId = state != null && state.getLastSeenMessageId() != null ? state.getLastSeenMessageId() : 0;

        List<Message> missedMessages = lastSeenId > 0
                ? messageService.findMessagesAfter(channelId, lastSeenId)
                : messageService.findByChannel(channelId);

        missedMessages = filterMessagesForSummary(missedMessages);

        if (missedMessages.isEmpty()) {
            return null;
        }

        int fromMessageId = missedMessages.get(0).getId();
        int toMessageId = missedMessages.get(missedMessages.size() - 1).getId();
        int missedCount = missedMessages.size();

        if (AISummaryConfig.isCacheEnabled()) {
            ChatSummaryResult cached = findCachedSummary(userId, channelId, fromMessageId, toMessageId);
            if (cached != null) {
                cached.setCached(true);
                return cached;
            }
        }

        ChatSummaryResult result;

        List<List<Message>> chunks = splitIntoChunks(missedMessages);

        if (chunks.size() == 1) {
            String transcript = buildTranscript(chunks.get(0));
            result = getOllamaClient().summarizeMissedTranscript(channelName, missedCount, transcript);
        } else {
            List<ChatSummaryResult> chunkSummaries = new ArrayList<>();

            for (List<Message> chunk : chunks) {
                String transcript = buildTranscript(chunk);
                ChatSummaryResult chunkSummary = getOllamaClient()
                        .summarizeMissedTranscript(channelName, chunk.size(), transcript);
                chunkSummaries.add(chunkSummary);
            }

            String chunkDigest = buildChunkDigest(chunkSummaries);
            result = getOllamaClient().summarizeChunkSummaries(channelName, missedCount, chunkDigest);
        }

        result.setMissedCount(missedCount);
        result.setFromMessageId(fromMessageId);
        result.setToMessageId(toMessageId);
        result.setCached(false);

        if (AISummaryConfig.isCacheEnabled()) {
            saveCachedSummary(userId, channelId, result);
        }

        return result;
    }

    public ChatSummaryResult summarizeChannelForAdmin(int channelId, String channelName) throws Exception {
        List<Message> channelMessages = messageService.findByChannel(channelId);
        channelMessages = filterMessagesForSummary(channelMessages);

        if (channelMessages.isEmpty()) {
            return null;
        }

        int fromMessageId = channelMessages.get(0).getId();
        int toMessageId = channelMessages.get(channelMessages.size() - 1).getId();
        int totalCount = channelMessages.size();

        ChatSummaryResult result;

        List<List<Message>> chunks = splitIntoChunks(channelMessages);

        if (chunks.size() == 1) {
            String transcript = buildTranscript(chunks.get(0));
            result = getOllamaClient().summarizeMissedTranscript(channelName, totalCount, transcript);
        } else {
            List<ChatSummaryResult> chunkSummaries = new ArrayList<>();

            for (List<Message> chunk : chunks) {
                String transcript = buildTranscript(chunk);
                ChatSummaryResult chunkSummary = getOllamaClient()
                        .summarizeMissedTranscript(channelName, chunk.size(), transcript);
                chunkSummaries.add(chunkSummary);
            }

            String chunkDigest = buildChunkDigest(chunkSummaries);
            result = getOllamaClient().summarizeChunkSummaries(channelName, totalCount, chunkDigest);
        }

        result.setMissedCount(totalCount);
        result.setFromMessageId(fromMessageId);
        result.setToMessageId(toMessageId);
        result.setCached(false);

        return result;
    }

    private List<Message> filterMessagesForSummary(List<Message> source) {
        List<Message> filtered = new ArrayList<>();
        for (Message message : source) {
            if (message == null) continue;
            if (message.isIs_deleted()) continue;

            String content = message.getContent() == null ? "" : message.getContent().trim();
            boolean hasText = !content.isBlank();
            boolean hasAttachments = message.hasAttachments();

            if (!hasText && !hasAttachments) continue;
            filtered.add(message);
        }
        return filtered;
    }

    private List<List<Message>> splitIntoChunks(List<Message> messages) {
        List<List<Message>> chunks = new ArrayList<>();

        List<Message> currentChunk = new ArrayList<>();
        int currentChars = 0;

        for (Message message : messages) {
            String line = formatMessageLine(message);
            if (line.isBlank()) continue;

            boolean chunkIsFullByCount = currentChunk.size() >= AISummaryConfig.getChunkMaxMessages();
            boolean chunkIsFullByChars = currentChars + line.length() > AISummaryConfig.getChunkMaxChars();

            if (!currentChunk.isEmpty() && (chunkIsFullByCount || chunkIsFullByChars)) {
                chunks.add(new ArrayList<>(currentChunk));
                currentChunk.clear();
                currentChars = 0;
            }

            currentChunk.add(message);
            currentChars += line.length();
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    private String buildTranscript(List<Message> messages) {
        StringBuilder sb = new StringBuilder();

        for (Message message : messages) {
            String line = formatMessageLine(message);
            if (!line.isBlank()) {
                sb.append(line).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private String formatMessageLine(Message message) {
        if (message == null || message.isIs_deleted()) {
            return "";
        }

        StringBuilder line = new StringBuilder();

        if (message.getSentAt() != null) {
            line.append("[").append(DATE_FMT.format(message.getSentAt().toLocalDateTime())).append("] ");
        }

        String sender = message.getSender_name() != null && !message.getSender_name().isBlank()
                ? message.getSender_name()
                : "Unknown";

        line.append(sender).append(": ");

        String content = message.getContent() == null ? "" : message.getContent().replaceAll("\\s+", " ").trim();
        boolean hasText = !content.isBlank();
        boolean hasAttachments = message.hasAttachments();

        if (hasText) {
            line.append(content);
        }

        if (hasAttachments) {
            if (hasText) {
                line.append(" ");
            }
            line.append("(attachments: ").append(formatAttachments(message.getAttachments())).append(")");
        }

        if (!hasText && !hasAttachments) {
            return "";
        }

        return line.toString();
    }

    private String formatAttachments(List<MessageAttachment> attachments) {
        List<String> parts = new ArrayList<>();

        for (MessageAttachment attachment : attachments) {
            if (attachment == null) continue;

            String name = attachment.getOriginalName() != null ? attachment.getOriginalName() : "file";
            String mime = attachment.getMimeType() != null ? attachment.getMimeType() : "unknown";
            parts.add(name + " [" + mime + "]");
        }

        return String.join(", ", parts);
    }

    private String buildChunkDigest(List<ChatSummaryResult> chunkSummaries) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chunkSummaries.size(); i++) {
            ChatSummaryResult chunk = chunkSummaries.get(i);
            sb.append("Chunk ").append(i + 1).append(":\n");
            sb.append("Title: ").append(chunk.getTitle()).append("\n");
            sb.append("Overview: ").append(chunk.getOverview()).append("\n");

            if (chunk.getKeyPoints() != null && !chunk.getKeyPoints().isEmpty()) {
                sb.append("Key points:\n");
                for (String point : chunk.getKeyPoints()) {
                    sb.append("- ").append(point).append("\n");
                }
            }

            if (chunk.getActionItems() != null && !chunk.getActionItems().isEmpty()) {
                sb.append("Action items:\n");
                for (String point : chunk.getActionItems()) {
                    sb.append("- ").append(point).append("\n");
                }
            }

            if (chunk.getOpenQuestions() != null && !chunk.getOpenQuestions().isEmpty()) {
                sb.append("Open questions:\n");
                for (String point : chunk.getOpenQuestions()) {
                    sb.append("- ").append(point).append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private ChatSummaryResult findCachedSummary(int userId, int channelId, int fromMessageId, int toMessageId)
            throws SQLException {

        String sql = """
            SELECT *
            FROM channel_summary_cache
            WHERE user_id = ? AND channel_id = ? AND from_message_id = ? AND to_message_id = ? AND model_name = ?
            LIMIT 1
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, channelId);
        ps.setInt(3, fromMessageId);
        ps.setInt(4, toMessageId);
        ps.setString(5, AISummaryConfig.getModel());

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String summaryJson = rs.getString("summary_json");
            ChatSummaryResult result = ChatSummaryResult.fromJsonObject(new JSONObject(summaryJson));
            result.setCached(true);
            return result;
        }

        return null;
    }

    private void saveCachedSummary(int userId, int channelId, ChatSummaryResult result) throws SQLException {
        String sql = """
            INSERT INTO channel_summary_cache
                (user_id, channel_id, from_message_id, to_message_id, missed_count,
                 summary_title, summary_text, summary_json, model_name, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                missed_count = VALUES(missed_count),
                summary_title = VALUES(summary_title),
                summary_text = VALUES(summary_text),
                summary_json = VALUES(summary_json),
                model_name = VALUES(model_name),
                created_at = VALUES(created_at)
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);

        ps.setInt(1, userId);
        ps.setInt(2, channelId);
        ps.setInt(3, result.getFromMessageId());
        ps.setInt(4, result.getToMessageId());
        ps.setInt(5, result.getMissedCount());
        ps.setString(6, result.getTitle());
        ps.setString(7, result.buildPlainText());
        ps.setString(8, result.toJsonObject().toString());
        ps.setString(9, AISummaryConfig.getModel());
        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));

        ps.executeUpdate();
    }
}