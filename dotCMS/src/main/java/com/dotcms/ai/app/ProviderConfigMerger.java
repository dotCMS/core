package com.dotcms.ai.app;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Merges a partially-masked {@code providerConfig} JSON with the real stored configuration.
 *
 * <p>When a user edits the provider config through the API, they receive a redacted view
 * where credential fields ({@code apiKey}, {@code secretAccessKey}, {@code accessKeyId})
 * are replaced with {@value #MASKED}. If they submit that JSON back without changing the
 * credential values, this class restores the real credentials from the currently stored config
 * before persisting — so credentials are never overwritten with the sentinel value.
 *
 * <p>Merge rules:
 * <ul>
 *   <li>Any credential field ({@code apiKey}, {@code secretAccessKey}, {@code accessKeyId})
 *       equal to {@value #MASKED} in the incoming JSON is replaced with the corresponding
 *       real value from the stored JSON, if present. Non-credential fields are left as-is
 *       even if their value happens to equal {@value #MASKED}.</li>
 *   <li>Nested objects (e.g. {@code chat}, {@code embeddings}, {@code image} sections)
 *       are merged recursively.</li>
 *   <li>All other fields are taken from the incoming JSON as-is.</li>
 *   <li>On any parse error the incoming JSON is returned unchanged.</li>
 * </ul>
 */
public class ProviderConfigMerger {

    public static final String MASKED = "*****";
    public static final Set<String> CREDENTIAL_FIELDS = Set.of("apiKey", "secretAccessKey", "accessKeyId");
    private static final ObjectMapper MAPPER = DotObjectMapperProvider.createDefaultMapper();

    private ProviderConfigMerger() {}

    /**
     * Returns {@code true} if {@code json} contains at least one field value equal to
     * {@value #MASKED} (fast string check, no parsing). Used as a cheap pre-filter before
     * attempting a full merge — not suitable for post-merge validation because it also matches
     * non-credential fields. Use {@link #containsMaskedCredential(String)} for that.
     */
    public static boolean containsMasked(final String json) {
        return StringUtils.isNotBlank(json) && json.contains("\"" + MASKED + "\"");
    }

    /**
     * Returns {@code true} if {@code json} contains at least one credential field
     * ({@code apiKey}, {@code secretAccessKey}, {@code accessKeyId}) whose value equals
     * {@value #MASKED}. Unlike {@link #containsMasked(String)}, this method parses the JSON
     * and restricts the check to {@link #CREDENTIAL_FIELDS}, so non-credential fields whose
     * value happens to equal {@value #MASKED} do not trigger a false positive.
     */
    public static boolean containsMaskedCredential(final String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            return hasMaskedCredential(MAPPER.readTree(json));
        } catch (final Exception e) {
            return false;
        }
    }

    private static boolean hasMaskedCredential(final JsonNode node) {
        if (!node.isObject()) {
            return false;
        }
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final JsonNode value = entry.getValue();
            if (CREDENTIAL_FIELDS.contains(entry.getKey())
                    && value.isTextual() && MASKED.equals(value.asText())) {
                return true;
            }
            if (value.isObject() && hasMaskedCredential(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Merges {@code newJson} with {@code storedJson}, replacing any {@value #MASKED} values
     * in {@code newJson} with the corresponding real values from {@code storedJson}.
     *
     * @param newJson    incoming JSON, potentially containing {@value #MASKED} sentinels
     * @param storedJson currently stored JSON with real credential values; may be blank
     * @return merged JSON string, or {@code newJson} unchanged if {@code storedJson} is blank,
     *         not a JSON object, or a parse error occurs
     */
    public static String merge(final String newJson, final String storedJson) {
        if (StringUtils.isBlank(storedJson)) {
            return newJson;
        }
        try {
            final JsonNode newRoot = MAPPER.readTree(newJson);
            if (!newRoot.isObject()) {
                return newJson;
            }
            final JsonNode storedRoot = MAPPER.readTree(storedJson);
            if (!storedRoot.isObject()) {
                Logger.warn(ProviderConfigMerger.class,
                        "Stored providerConfig is not a JSON object; skipping merge to avoid persisting sentinel values");
                return newJson;
            }
            mergeNode((ObjectNode) newRoot, storedRoot);
            return MAPPER.writeValueAsString(newRoot);
        } catch (final Exception e) {
            Logger.warn(ProviderConfigMerger.class,
                    "Failed to merge providerConfig, using incoming value as-is: " + e.getMessage());
            return newJson;
        }
    }

    private static void mergeNode(final ObjectNode incoming, final JsonNode stored) {
        if (stored == null || !stored.isObject()) {
            return;
        }
        final Iterator<Map.Entry<String, JsonNode>> fields = incoming.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final String key = entry.getKey();
            final JsonNode incomingValue = entry.getValue();

            if (incomingValue.isTextual() && MASKED.equals(incomingValue.asText())
                    && CREDENTIAL_FIELDS.contains(key)) {
                final JsonNode storedValue = stored.get(key);
                if (storedValue != null && !storedValue.isNull()) {
                    incoming.set(key, storedValue);
                }
            } else if (incomingValue.isObject()) {
                final JsonNode storedChild = stored.get(key);
                if (storedChild != null && storedChild.isObject()) {
                    mergeNode((ObjectNode) incomingValue, storedChild);
                }
            }
        }
    }

}
