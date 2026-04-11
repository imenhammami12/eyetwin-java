package com.eyetwin.services;

import com.eyetwin.config.ConfigLoader;

public final class GroqConfig {
    private GroqConfig() {}

    public static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    public static final String API_KEY = ConfigLoader.get("GROQ_API_KEY");
    public static final String MODEL   = "llama-3.3-70b-versatile";
}