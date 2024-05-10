package com.dotcms.ai.workflow;

public enum OpenAIParams {
    OVERWRITE_FIELDS("overwriteField"),
    FIELD_TO_WRITE("fieldToWrite"),
    OPEN_AI_PROMPT("openAIPrompt"),
    TEMPERATURE("temperature"),
    MODEL("model"),
    RUN_DELAY("runDelay"),
    DOT_EMBEDDING_TYPES_FIELDS("dotEmbeddingTypes"),
    DOT_EMBEDDING_ACTION("dotEmbeddingAction"),
    DOT_EMBEDDING_INDEX("default"),
    LIMIT_TAGS_TO_HOST("limitTagsToHost");

    public final String key;


    OpenAIParams(String keyIn) {
        key = keyIn;
    }
}
