package com.buildyz;

/** Desteklenen yapay zeka sağlayıcıları. Gemini, Groq ve OpenRouter ücretsiz katman sunar (limitli). */
enum Provider {
    ANTHROPIC("anthropic", "Anthropic Claude", "https://api.anthropic.com/v1/messages",
            "claude-sonnet-5-5", "https://console.anthropic.com/settings/keys"),
    GEMINI("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            "gemini-2.5-flash", "https://aistudio.google.com/apikey"),
    GROQ("groq", "Groq", "https://api.groq.com/openai/v1/chat/completions",
            "openai/gpt-oss-120b", "https://console.groq.com/keys"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/chat/completions",
            "google/gemma-4-31b-it:free", "https://openrouter.ai/keys");

    final String id, label, url, defaultModel, keyUrl;

    Provider(String id, String label, String url, String defaultModel, String keyUrl) {
        this.id = id;
        this.label = label;
        this.url = url;
        this.defaultModel = defaultModel;
        this.keyUrl = keyUrl;
    }

    boolean free() {
        return this != ANTHROPIC;
    }

    /** Anahtarın başlangıcından sağlayıcıyı tanır. Tanıyamazsa null. */
    static Provider detect(String key) {
        if (key == null) return null;
        if (key.startsWith("sk-ant-")) return ANTHROPIC;
        if (key.startsWith("sk-or-")) return OPENROUTER;
        if (key.startsWith("gsk_")) return GROQ;
        if (key.startsWith("AIza")) return GEMINI;
        return null;
    }
}
