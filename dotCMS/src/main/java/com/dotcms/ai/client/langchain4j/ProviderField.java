package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes a single configurable {@link ProviderConfig} property for one provider/capability
 * combination, so a client can render the right input without hardcoding per-provider knowledge.
 *
 * @param name     the {@link ProviderConfig} property name, e.g. {@code apiKey}, {@code maxRetries}
 * @param type     the field's primitive type
 * @param required whether this field must be set for the given provider/capability
 * @param hint     short human-readable guidance (e.g. cross-field dependencies); empty if none
 */
public record ProviderField(
        @JsonProperty("name") String name,
        @JsonProperty("type") ProviderFieldType type,
        @JsonProperty("required") boolean required,
        @JsonProperty("hint") String hint) {

    public ProviderField {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ProviderField name is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("ProviderField type is required");
        }
        hint = hint == null ? "" : hint;
    }

    public static ProviderField required(final String name, final ProviderFieldType type) {
        return new ProviderField(name, type, true, "");
    }

    public static ProviderField required(final String name, final ProviderFieldType type, final String hint) {
        return new ProviderField(name, type, true, hint);
    }

    public static ProviderField optional(final String name, final ProviderFieldType type) {
        return new ProviderField(name, type, false, "");
    }

    public static ProviderField optional(final String name, final ProviderFieldType type, final String hint) {
        return new ProviderField(name, type, false, hint);
    }

}
