package com.eyetwin.services.Community;

import com.eyetwin.config.PiperTtsConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PiperTextToSpeechService {

    public File synthesizeToFile(String text) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text is empty.");
        }

        VoiceSelection voice = selectVoice(text);

        File modelFile = new File(voice.modelPath());
        File configFile = new File(voice.configPath());

        if (!modelFile.exists()) {
            throw new IllegalStateException("Piper model not found: " + modelFile.getAbsolutePath());
        }

        if (!configFile.exists()) {
            throw new IllegalStateException("Piper config not found: " + configFile.getAbsolutePath());
        }

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "eyetwin-audio");
        Files.createDirectories(tempDir);

        File outputFile = tempDir.resolve("tts-" + System.currentTimeMillis() + ".wav").toFile();

        List<String> command = new ArrayList<>();
        command.add(PiperTtsConfig.getCommand());
        command.add("--model");
        command.add(modelFile.getAbsolutePath());
        command.add("--config");
        command.add(configFile.getAbsolutePath());
        command.add("--output_file");
        command.add(outputFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(text);
            writer.newLine();
        }

        StringBuilder processOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IllegalStateException("Piper TTS failed:\n" + processOutput);
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IllegalStateException("Piper created no audio file.");
        }

        return outputFile;
    }

    private VoiceSelection selectVoice(String text) {
        String language = detectLanguage(text);

        return switch (language) {
            case "ar" -> new VoiceSelection(
                    PiperTtsConfig.getArabicModelPath(),
                    PiperTtsConfig.getArabicConfigPath()
            );
            case "fr" -> new VoiceSelection(
                    PiperTtsConfig.getFrenchModelPath(),
                    PiperTtsConfig.getFrenchConfigPath()
            );
            default -> new VoiceSelection(
                    PiperTtsConfig.getEnglishModelPath(),
                    PiperTtsConfig.getEnglishConfigPath()
            );
        };
    }

    private String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "en";
        }

        int arabicCount = 0;
        int frenchHintCount = 0;
        int latinCount = 0;

        String lower = text.toLowerCase();

        for (char ch : lower.toCharArray()) {
            if (isArabicChar(ch)) {
                arabicCount++;
            } else if (Character.isLetter(ch)) {
                latinCount++;
                if (isFrenchHintChar(ch)) {
                    frenchHintCount++;
                }
            }
        }

        if (arabicCount > 0) {
            return "ar";
        }

        if (looksFrench(lower, frenchHintCount, latinCount)) {
            return "fr";
        }

        return "en";
    }

    private boolean isArabicChar(char ch) {
        return (ch >= '\u0600' && ch <= '\u06FF')
                || (ch >= '\u0750' && ch <= '\u077F')
                || (ch >= '\u08A0' && ch <= '\u08FF')
                || (ch >= '\uFB50' && ch <= '\uFDFF')
                || (ch >= '\uFE70' && ch <= '\uFEFF');
    }

    private boolean isFrenchHintChar(char ch) {
        return "àâçéèêëîïôùûüÿœæ".indexOf(ch) >= 0;
    }

    private boolean looksFrench(String text, int frenchHintCount, int latinCount) {
        if (frenchHintCount > 0) {
            return true;
        }

        if (latinCount == 0) {
            return false;
        }

        String[] frenchWords = {
                "bonjour", "salut", "merci", "s'il", "vous", "nous", "je", "tu", "il", "elle",
                "est", "suis", "avec", "pour", "pas", "oui", "non", "bien", "très", "canal",
                "message", "français", "francaise", "comment", "pourquoi", "quoi", "ici"
        };

        int hits = 0;
        for (String word : frenchWords) {
            if (text.contains(word)) {
                hits++;
            }
        }

        return hits >= 2;
    }

    private record VoiceSelection(String modelPath, String configPath) {
    }
}