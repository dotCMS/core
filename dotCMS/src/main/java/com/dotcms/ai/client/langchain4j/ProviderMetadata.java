package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregated, provider-agnostic description of one {@link ModelProviderStrategy}: which
 * capabilities it supports and which {@link ProviderField}s each supported capability needs.
 * Built by {@link LangChain4jModelFactory#listProviderMetadata()} so a client (e.g. the dotAI
 * provider configuration REST endpoint) can render the full provider configuration form without
 * hardcoding per-provider knowledge. Adding a provider to
 * {@link LangChain4jModelFactory#STRATEGIES} makes it appear here automatically.
 *
 * @param provider              the provider identifier, e.g. {@code openai}, {@code azure_openai}
 * @param supportedCapabilities capabilities this provider can serve
 * @param fields                config fields per supported capability
 */
public record ProviderMetadata(
        @JsonProperty("provider") String provider,
        @JsonProperty("supportedCapabilities") Set<Capability> supportedCapabilities,
        @JsonProperty("fields") Map<Capability, List<ProviderField>> fields) {

    public ProviderMetadata {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("ProviderMetadata provider is required");
        }
        supportedCapabilities = supportedCapabilities == null ? Set.of() : Set.copyOf(supportedCapabilities);
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }

}
