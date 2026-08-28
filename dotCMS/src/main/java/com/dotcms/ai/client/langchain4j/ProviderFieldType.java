package com.dotcms.ai.client.langchain4j;

/**
 * The primitive type of a {@link ProviderField}, used by a form-rendering client to pick the
 * right input control without hardcoding per-field knowledge.
 */
public enum ProviderFieldType {
    STRING,
    NUMBER,
    SECRET
}
