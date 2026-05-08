package com.dotcms.ai.app;

import com.liferay.util.StringPool;

public enum AppKeys {

    API_KEY("apiKey", null),
    API_URL("apiUrl", "https://api.openai.com/v1/chat/completions"),
    API_IMAGE_URL("apiImageUrl", "https://api.openai.com/v1/images/generations"),
    API_EMBEDDINGS_URL("apiEmbeddingsUrl", "https://api.openai.com/v1/embeddings"),
    ROLE_PROMPT(
            "rolePrompt",
            "You are dotCMSbot, and AI assistant to help content" +
                    " creators generate and rewrite content in their content management system.",
            "rolePrompt"),
    TEXT_PROMPT("textPrompt", "Use Descriptive writing style.", "textPrompt"),
    IMAGE_PROMPT("imagePrompt", "Use 16:9 aspect ratio.", "imagePrompt"),
    IMAGE_SIZE("imageSize", "1024x1024", "imageSize"),
    EMBEDDINGS_SPLIT_AT_TOKENS("com.dotcms.ai.embeddings.split.at.tokens", "512", "embeddingsSplitAtTokens"),
    EMBEDDINGS_MINIMUM_TEXT_LENGTH_TO_INDEX("com.dotcms.ai.embeddings.minimum.text.length", "64", "embeddingsMinimumTextLength"),
    EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX("com.dotcms.ai.embeddings.minimum.file.size", "1024", "embeddingsMinimumFileSize"),
    EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED("com.dotcms.ai.embeddings.build.for.file.extensions", "pdf,doc,docx,txt,html", "embeddingsFileExtensions"),
    EMBEDDINGS_SEARCH_DEFAULT_THRESHOLD("com.dotcms.ai.embeddings.search.default.threshold", ".5", "embeddingsSearchThreshold"),
    EMBEDDINGS_THREADS("com.dotcms.ai.embeddings.threads", "3", "embeddingsThreads"),
    EMBEDDINGS_THREADS_MAX("com.dotcms.ai.embeddings.threads.max", "6", "embeddingsThreadsMax"),
    EMBEDDINGS_THREADS_QUEUE("com.dotcms.ai.embeddings.threads.queue", "10000", "embeddingsThreadsQueue"),
    EMBEDDINGS_CACHE_TTL_SECONDS("com.dotcms.ai.embeddings.cache.ttl.seconds", "600", "embeddingsCacheTtlSeconds"),
    EMBEDDINGS_CACHE_SIZE("com.dotcms.ai.embeddings.cache.size", "1000", "embeddingsCacheSize"),
    EMBEDDINGS_DB_DELETE_OLD_ON_UPDATE("com.dotcms.ai.embeddings.delete.old.on.update", "true", "embeddingsDeleteOldOnUpdate"),
    DEBUG_LOGGING("com.dotcms.ai.debug.logging", StringPool.FALSE, "debugLogging"),
    COMPLETION_TEMPERATURE("com.dotcms.ai.completion.default.temperature", "1", "temperature"),
    COMPLETION_ROLE_PROMPT(
            "com.dotcms.ai.completion.role.prompt",
            "You are a helpful assistant with a descriptive writing style.",
            "completionRolePrompt"),
    COMPLETION_TEXT_PROMPT(
            "com.dotcms.ai.completion.text.prompt",
            "Answer this question\\n\\\"$!{prompt}?\\\"\\n\\nby using only the information in" +
                    " the following text:\\n\"\"\"\\n$!{supportingContent} \\n\"\"\"\\n",
            "completionTextPrompt"),
    LISTENER_INDEXER("listenerIndexer", "{}", "listenerIndexer"),
    PROVIDER_CONFIG("providerConfig", null);

    public static final String APP_KEY = "dotAI";

    public final String key;
    public final String defaultValue;
    /** JSON key in {@code providerConfig.settings}; {@code null} for keys not in settings. */
    public final String settingsKey;

    AppKeys(final String key, final String defaultValue) {
        this(key, defaultValue, null);
    }

    AppKeys(final String key, final String defaultValue, final String settingsKey) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.settingsKey = settingsKey;
    }

}
