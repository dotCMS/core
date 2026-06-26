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
 *   <li>{@code provider} – identifier: {@code openai}, {@code azure_openai}, {@code bedrock}, {@code vertex_ai}, {@code anthropic}</li>
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
 *   <li>{@code accessKeyId} – set together with {@code secretAccessKey}, or omit both to use the
 *       AWS default credential chain (env, profile, container/EKS IRSA)</li>
 *   <li>{@code secretAccessKey}</li>
 *   <li>{@code embeddingInputType} – Cohere only: {@code search_document} (default) or {@code search_query}</li>
 *   <li>{@code timeout} and {@code maxRetries} (common fields above) apply to the Bedrock runtime
 *       clients: {@code timeout} as the per-attempt {@code apiCallAttemptTimeout};
 *       {@code maxRetries} as the SDK retry-strategy {@code maxAttempts} (= {@code max(1, maxRetries + 1)})</li>
 * </ul>
 *
 * <p>Bedrock {@code model} ID forms: use an inference-profile prefix ({@code us.}, {@code eu.},
 * {@code apac.}) for models offered only via cross-region inference profiles
 * (e.g. {@code us.deepseek.r1-v1:0}); use the bare ID for on-demand models
 * (e.g. {@code openai.gpt-oss-120b-1:0}, {@code amazon.titan-embed-text-v2:0}).
 *
 * <p>Anthropic (chat only — Anthropic has no embeddings or image APIs):
 * <ul>
 *   <li>{@code apiKey} – Anthropic API key</li>
 *   <li>{@code model} – e.g. {@code claude-sonnet-4-6}, {@code claude-haiku-4-5}</li>
 *   <li>{@code endpoint} – optional base URL override (proxies/gateways)</li>
 * </ul>
 *
 * <p>Google Vertex AI (chat only — embeddings and image not supported by this integration):
 * <ul>
 *   <li>{@code projectId} – GCP project ID</li>
 *   <li>{@code location} – GCP region, e.g. {@code us-central1}</li>
 *   <li>{@code credentialsJson} – full content of a GCP service account JSON key file.
 *       When set, it is used directly for authentication instead of Application Default Credentials (ADC).
 *       If omitted, ADC is used (useful for GKE / Cloud Run workload identity).</li>
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
    /**
     * Cohere embedding input type. Valid values: {@code search_document} (default), {@code search_query}.
     * Use {@code search_document} when indexing content, {@code search_query} when embedding search queries.
     */
    @Value.Default
    default String embeddingInputType() { return "search_document"; }

    // Google Vertex AI
    @Nullable String projectId();
    @Nullable String location();
    /**
     * Full content of a GCP service account JSON key file (the entire JSON object as a string).
     * When set, used for authentication instead of Application Default Credentials.
     */
    @Value.Redacted @Nullable String credentialsJson();

}
