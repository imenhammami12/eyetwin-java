package com.eyetwin.entities.Community;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatSummaryResult {

    private String title;
    private String overview;
    private List<String> keyPoints = new ArrayList<>();
    private List<String> actionItems = new ArrayList<>();
    private List<String> openQuestions = new ArrayList<>();
    private int missedCount;
    private int fromMessageId;
    private int toMessageId;
    private boolean cached;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public List<String> getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints;
    }

    public List<String> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<String> actionItems) {
        this.actionItems = actionItems;
    }

    public List<String> getOpenQuestions() {
        return openQuestions;
    }

    public void setOpenQuestions(List<String> openQuestions) {
        this.openQuestions = openQuestions;
    }

    public int getMissedCount() {
        return missedCount;
    }

    public void setMissedCount(int missedCount) {
        this.missedCount = missedCount;
    }

    public int getFromMessageId() {
        return fromMessageId;
    }

    public void setFromMessageId(int fromMessageId) {
        this.fromMessageId = fromMessageId;
    }

    public int getToMessageId() {
        return toMessageId;
    }

    public void setToMessageId(int toMessageId) {
        this.toMessageId = toMessageId;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("title", title != null ? title : "");
        json.put("overview", overview != null ? overview : "");
        json.put("key_points", new JSONArray(keyPoints != null ? keyPoints : new ArrayList<>()));
        json.put("action_items", new JSONArray(actionItems != null ? actionItems : new ArrayList<>()));
        json.put("open_questions", new JSONArray(openQuestions != null ? openQuestions : new ArrayList<>()));
        json.put("missed_count", missedCount);
        json.put("from_message_id", fromMessageId);
        json.put("to_message_id", toMessageId);
        return json;
    }

    public static ChatSummaryResult fromJsonObject(JSONObject json) {
        ChatSummaryResult result = new ChatSummaryResult();
        result.setTitle(json.optString("title", ""));
        result.setOverview(json.optString("overview", ""));
        result.setKeyPoints(toList(json.optJSONArray("key_points")));
        result.setActionItems(toList(json.optJSONArray("action_items")));
        result.setOpenQuestions(toList(json.optJSONArray("open_questions")));
        result.setMissedCount(json.optInt("missed_count", 0));
        result.setFromMessageId(json.optInt("from_message_id", 0));
        result.setToMessageId(json.optInt("to_message_id", 0));
        return result;
    }

    public String buildPlainText() {
        StringBuilder sb = new StringBuilder();

        if (title != null && !title.isBlank()) {
            sb.append(title).append("\n");
        }
        if (overview != null && !overview.isBlank()) {
            sb.append(overview).append("\n");
        }

        appendSection(sb, "Key points", keyPoints);
        appendSection(sb, "Action items", actionItems);
        appendSection(sb, "Open questions", openQuestions);

        return sb.toString().trim();
    }

    private static void appendSection(StringBuilder sb, String title, List<String> items) {
        if (items == null || items.isEmpty()) return;
        sb.append("\n").append(title).append(":\n");
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
    }

    private static List<String> toList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            list.add(array.optString(i, ""));
        }
        return list;
    }
}