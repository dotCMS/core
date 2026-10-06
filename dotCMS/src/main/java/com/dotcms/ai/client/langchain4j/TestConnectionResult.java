package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outcome of a {@link ProviderConnectionTester} run for one provider/capability combination.
 * 
 * @param success whether the provider accepted the request
 * @param message human-readable detail — a confirmation on success, the provider/validation error on failure
 */
public record TestConnectionResult(
        @JsonProperty("success") boolean success,
        @JsonProperty("message") String message) {
}
