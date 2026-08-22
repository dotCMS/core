package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes a single configurable {@link ProviderConfig} property for one provider/capability
 * combination, so a client can render the right input without hardcoding per-provider knowledge.
 *
 * @param name           the {@link ProviderConfig} property name, e.g. {@code apiKey}, {@code maxRetries}
 * @param type           the field's primitive type
 * @param required       whether this field must always be set for the given provider/capability
 * @param hint           short human-readable guidance (e.g. cross-field dependencies); empty if none
 * @param requiredUnless when non-empty, the name of a sibling field whose presence satisfies this
 *                       field's requirement (e.g. Azure's {@code model} is required unless
 *                       {@code deploymentName} is set, and vice versa). Only meaningful when
 *                       {@code required} is {@code false} — a client should treat this field as
 *                       required unless the named sibling has a value. Empty when there's no such
 *                       either-or relationship.
 */
public record ProviderField(
        @JsonProperty("name") String name,
        @JsonProperty("type") ProviderFieldType type,
        @JsonProperty("required") boolean required,
        @JsonProperty("hint") String hint,
        @JsonProperty("requiredUnless") String requiredUnless) {

    public ProviderField {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ProviderField name is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("ProviderField type is required");
        }
        hint = hint == null ? "" : hint;
        requiredUnless = requiredUnless == null ? "" : requiredUnless;
    }

    public static ProviderField required(final String name, final ProviderFieldType type) {
        return new ProviderField(name, type, true, "", "");
    }

    public static ProviderField required(final String name, final ProviderFieldType type, final String hint) {
        return new ProviderField(name, type, true, hint, "");
    }

    public static ProviderField optional(final String name, final ProviderFieldType type) {
        return new ProviderField(name, type, false, "", "");
    }

    public static ProviderField optional(final String name, final ProviderFieldType type, final String hint) {
        return new ProviderField(name, type, false, hint, "");
    }

    /**
     * An optional field that's effectively required unless the named sibling field is set (e.g.
     * Azure's {@code model}/{@code deploymentName} pair — either one is enough).
     *
     * @param name           the field name
     * @param type           the field's primitive type
     * @param requiredUnless the sibling field name whose presence satisfies this requirement
     * @param hint           short human-readable guidance
     */
    public static ProviderField optionalUnless(final String name, final ProviderFieldType type,
                                               final String requiredUnless, final String hint) {
        return new ProviderField(name, type, false, hint, requiredUnless);
    }

}
