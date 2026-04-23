package com.eyetwin.tools;

import java.io.File;
import java.util.Locale;
import java.util.Set;

public final class CommunityFileValidator {

    private static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "pdf", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "txt", "zip"
    );

    private CommunityFileValidator() {
    }

    public static void validate(File file) {
        if (file == null) {
            throw new IllegalArgumentException("No file selected.");
        }

        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Selected file does not exist.");
        }

        if (file.length() <= 0) {
            throw new IllegalArgumentException("Empty file is not allowed.");
        }

        if (file.length() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large. Max size is 10 MB.");
        }

        String ext = getExtension(file.getName());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("This file type is not allowed.");
        }
    }

    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}