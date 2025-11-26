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
                    " creators generate and rewrite content in their content management system."),
    TEXT_PROMPT("textPrompt", "Use Descriptive writing style."),
    IMAGE_PROMPT("imagePrompt", "Use 16:9 aspect ratio."),
    IMAGE_SIZE("imageSize", "1024x1024"),
    TEXT_MODEL_NAMES("textModelNames", null),
    TEXT_MODEL_TOKENS_PER_MINUTE("textModelTokensPerMinute", "180000"),
    TEXT_MODEL_API_PER_MINUTE("textModelApiPerMinute", "3500"),
    TEXT_MODEL_MAX_TOKENS("textModelMaxTokens", "16384"),
    TEXT_MODEL_COMPLETION("textModelCompletion", "true"),
    IMAGE_MODEL_NAMES("imageModelNames", null),
    IMAGE_MODEL_TOKENS_PER_MINUTE("imageModelTokensPerMinute", "0"),
    IMAGE_MODEL_API_PER_MINUTE("imageModelApiPerMinute", "50"),
    IMAGE_MODEL_MAX_TOKENS("imageModelMaxTokens", "0"),
    IMAGE_MODEL_COMPLETION("imageModelCompletion",  StringPool.FALSE),
    EMBEDDINGS_MODEL_NAMES("embeddingsModelNames", null),
    EMBEDDINGS_MODEL_TOKENS_PER_MINUTE("embeddingsModelTokensPerMinute", "1000000"),
    EMBEDDINGS_MODEL_API_PER_MINUTE("embeddingsModelApiPerMinute", "3000"),
    EMBEDDINGS_MODEL_MAX_TOKENS("embeddingsModelMaxTokens", "8191"),
    EMBEDDINGS_MODEL_COMPLETION("embeddingsModelCompletion",  StringPool.FALSE),
    EMBEDDINGS_SPLIT_AT_TOKENS("com.dotcms.ai.embeddings.split.at.tokens", "512"),
    EMBEDDINGS_MINIMUM_TEXT_LENGTH_TO_INDEX("com.dotcms.ai.embeddings.minimum.text.length", "64"),
    EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX("com.dotcms.ai.embeddings.minimum.file.size", "1024"),
    EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED("com.dotcms.ai.embeddings.build.for.file.extensions", "pdf,doc,docx,txt,html"),
    EMBEDDINGS_SEARCH_DEFAULT_THRESHOLD("com.dotcms.ai.embeddings.search.default.threshold", ".25"),
    EMBEDDINGS_THREADS("com.dotcms.ai.embeddings.threads", "3"),
    EMBEDDINGS_THREADS_MAX("com.dotcms.ai.embeddings.threads.max", "6"),
    EMBEDDINGS_THREADS_QUEUE("com.dotcms.ai.embeddings.threads.queue", "10000"),
    EMBEDDINGS_CACHE_TTL_SECONDS("com.dotcms.ai.embeddings.cache.ttl.seconds", "600"),
    EMBEDDINGS_CACHE_SIZE("com.dotcms.ai.embeddings.cache.size", "1000"),
    EMBEDDINGS_DB_DELETE_OLD_ON_UPDATE("com.dotcms.ai.embeddings.delete.old.on.update", "true"),
    DEBUG_LOGGING("com.dotcms.ai.debug.logging", StringPool.FALSE),
    COMPLETION_TEMPERATURE("com.dotcms.ai.completion.default.temperature", "1"),
    COMPLETION_ROLE_PROMPT(
            "com.dotcms.ai.completion.role.prompt",
            "You are a helpful assistant with a descriptive writing style."),
    COMPLETION_TEXT_PROMPT(
            "com.dotcms.ai.completion.text.prompt",
            "Answer this question\\n\\\"$!{prompt}?\\\"\\n\\nby using only the information in" +
                    " the following text:\\n\"\"\"\\n$!{supportingContent} \\n\"\"\"\\n"),
    LISTENER_INDEXER("listenerIndexer", "{}"),
    AI_MODELS_CACHE_TTL("com.dotcms.ai.models.supported.ttl", "28800"),
    AI_MODELS_CACHE_SIZE("com.dotcms.ai.models.supported.size", "64");

    public static final String APP_KEY = "dotAI";

    public final String key;
    public final String defaultValue;

    AppKeys(final String key, final String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

}
