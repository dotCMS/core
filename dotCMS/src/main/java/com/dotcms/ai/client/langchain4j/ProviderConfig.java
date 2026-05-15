package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable representation of a single provider section in the {@code providerConfig} JSON.
 *
 * <p>Each section (chat, embeddings, image) in the JSON maps to one instance of this class.
 * Unknown fields are ignored to allow forward-compatible configuration.
 *
 * <p>Common fields (all providers):
 * <ul>
 *   <li>{@code provider} – identifier: {@code openai}, {@code azure_openai}, {@code bedrock}, {@code vertex_ai}</li>
 *   <li>{@code model} – model name or ID</li>
 *   <li>{@code maxTokens} – max output tokens</li>
 *   <li>{@code temperature} – sampling temperature (0.0–2.0)</li>
 *   <li>{@code maxRetries} – retry attempts on transient failures</li>
 *   <li>{@code timeout} – request timeout in seconds</li>
 * </ul>
 *
 * <p>OpenAI / Azure OpenAI:
 * <ul>
 *   <li>{@code apiKey}</li>
 *   <li>{@code size} – image size, e.g. {@code 1024x1024} (image only)</li>
 *   <li>{@code dimensions} – embedding vector size (embeddings only); required for models like {@code text-embedding-3-small/large}</li>
 *   <li>{@code endpoint} – Azure base URL</li>
 *   <li>{@code deploymentName} – Azure deployment name</li>
 *   <li>{@code apiVersion} – Azure API version, e.g. {@code 2024-02-01}</li>
 * </ul>
 *
 * <p>AWS Bedrock:
 * <ul>
 *   <li>{@code region}</li>
 *   <li>{@code accessKeyId}</li>
 *   <li>{@code secretAccessKey}</li>
 * </ul>
 *
 * <p>Google Vertex AI:
 * <ul>
 *   <li>{@code projectId}</li>
 *   <li>{@code location}</li>
 * </ul>
 */
@Value.Immutable
@JsonSerialize(as = ImmutableProviderConfig.class)
@JsonDeserialize(as = ImmutableProviderConfig.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ProviderConfig {

    @Nullable String provider();

    /**
     * Model name(s). Accepts a single name ({@code "gpt-4o"}) or a comma-separated fallback
     * list ({@code "gpt-4o,gpt-4o-mini"}). Use {@link #allModels()} to iterate over the list.
     */
    @Nullable String model();

    /**
     * Returns the ordered list of model names parsed from {@link #model()}.
     * If {@code model} is blank or null, returns an empty list.
     */
    default List<String> allModels() {
        final String m = model();
        if (m == null || m.isBlank()) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<>();
        for (final String part : m.split("\\s*,\\s*")) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return List.copyOf(result);
    }

    @Nullable Integer maxTokens();
    @Nullable Integer maxCompletionTokens();
    @Nullable Double temperature();
    @Nullable Integer maxRetries();
    @Nullable Integer timeout();

    // OpenAI / Azure OpenAI
    @Value.Redacted @Nullable String apiKey();
    @Nullable String size();
    @Nullable Integer dimensions();
    @Nullable String endpoint();
    @Nullable String deploymentName();
    @Nullable String apiVersion();

    // AWS Bedrock
    @Nullable String region();
    @Value.Redacted @Nullable String accessKeyId();
    @Value.Redacted @Nullable String secretAccessKey();

    // Google Vertex AI
    @Nullable String projectId();
    @Nullable String location();

}
