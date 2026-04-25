package com.eyetwin.services.Community;

import com.eyetwin.entities.Community.MessageModerationResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageModerationService {

    private static final String BAD_WORDS_RESOURCE = "community-badwords.txt";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\S+|\\s+");

    private final Set<String> badWords = loadBadWords();
    private final Set<String> maskedBadWords = buildMaskedBadWords();

    public MessageModerationResult moderate(String content) {
        String original = content == null ? "" : content;

        if (original.isBlank()) {
            return new MessageModerationResult(original, original, new ArrayList<>());
        }

        Matcher matcher = TOKEN_PATTERN.matcher(original);
        StringBuilder masked = new StringBuilder();
        LinkedHashSet<String> matchedTerms = new LinkedHashSet<>();

        while (matcher.find()) {
            String token = matcher.group();

            if (token.isBlank()) {
                masked.append(token);
                continue;
            }

            String normalized = normalizeToken(token);
            String matched = findMatchedBadWord(normalized);

            if (matched != null) {
                masked.append(maskToken(matched));
                matchedTerms.add(matched);
            } else {
                masked.append(token);
            }
        }

        return new MessageModerationResult(original, masked.toString(), new ArrayList<>(matchedTerms));
    }

    public boolean isModeratedContent(String content) {
        return getModeratedTokenCount(content) > 0;
    }

    public boolean isSevereModeratedContent(String content) {
        return getModeratedTokenCount(content) >= 2;
    }

    public int getModeratedTokenCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(content);
        int count = 0;

        while (matcher.find()) {
            String token = matcher.group();

            if (token.isBlank()) {
                continue;
            }

            String normalizedMasked = normalizeMaskedToken(token);
            if (maskedBadWords.contains(normalizedMasked)) {
                count++;
            }
        }

        return count;
    }

    private String findMatchedBadWord(String normalizedToken) {
        if (normalizedToken == null || normalizedToken.isBlank()) {
            return null;
        }

        if (badWords.contains(normalizedToken)) {
            return normalizedToken;
        }

        return null;
    }

    private String normalizeToken(String token) {
        StringBuilder sb = new StringBuilder();

        for (char raw : token.toLowerCase().toCharArray()) {
            char mapped = mapLeetChar(raw);
            if (Character.isLetter(mapped)) {
                sb.append(mapped);
            }
        }

        String normalized = sb.toString();
        normalized = normalized.replaceAll("(.)\\1+", "$1");

        return normalized;
    }

    private String normalizeMaskedToken(String token) {
        StringBuilder sb = new StringBuilder();

        for (char raw : token.toLowerCase().toCharArray()) {
            char mapped = mapLeetChar(raw);
            if (Character.isLetter(mapped) || mapped == '*') {
                sb.append(mapped);
            }
        }

        return sb.toString();
    }

    private char mapLeetChar(char c) {
        return switch (c) {
            case '0' -> 'o';
            case '1', '!' -> 'i';
            case '3' -> 'e';
            case '4', '@' -> 'a';
            case '5', '$' -> 's';
            case '7' -> 't';
            default -> c;
        };
    }

    private String maskToken(String normalizedWord) {
        if (normalizedWord == null || normalizedWord.isBlank()) {
            return "";
        }

        if (normalizedWord.length() == 1) {
            return "*";
        }

        return normalizedWord.charAt(0) + "*".repeat(normalizedWord.length() - 1);
    }

    private Set<String> buildMaskedBadWords() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String word : badWords) {
            set.add(maskToken(word));
        }
        return set;
    }

    private Set<String> loadBadWords() {
        LinkedHashSet<String> set = new LinkedHashSet<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(BAD_WORDS_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Missing resource: " + BAD_WORDS_RESOURCE);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim().toLowerCase();
                    if (!word.isEmpty() && !word.startsWith("#")) {
                        set.add(word);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load bad words list: " + e.getMessage(), e);
        }

        return set;
    }
}