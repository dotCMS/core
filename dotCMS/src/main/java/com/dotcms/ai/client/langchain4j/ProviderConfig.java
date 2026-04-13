package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

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
 *   <li>{@code embeddingInputType} – Cohere only: {@code search_document} (default) or {@code search_query}</li>
 * </ul>
 *
 * <p>Google Vertex AI (chat only — embeddings and image not supported via LangChain4J):
 * <ul>
 *   <li>{@code projectId} – GCP project ID</li>
 *   <li>{@code location} – GCP region, e.g. {@code us-central1}</li>
 * </ul>
 * <p>Auth is handled automatically via Application Default Credentials (ADC).
 * No API key is required.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableProviderConfig.class)
@JsonDeserialize(as = ImmutableProviderConfig.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ProviderConfig {

    @Nullable String provider();
    @Nullable String model();
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

}
