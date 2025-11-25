package com.dotcms.ai.domain;

/**
 * Represents a response from an AI service.
 *
 * <p>
 * This class encapsulates the details of an AI response, including the response content.
 * It provides methods to build and retrieve the response.
 * </p>
 *
 * <p>
 * The class also provides a static instance representing an empty response.
 * </p>
 *
 * @author vico
 */
public class AIResponse {

    public static final AIResponse EMPTY = builder().build();

    private final String response;

    private AIResponse(final Builder builder) {
        this.response = builder.response;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getResponse() {
        return response;
    }

    public static class Builder {

        private String response;

        public Builder withResponse(final String response) {
            this.response = response;
            return this;
        }


        public AIResponse build() {
            return new AIResponse(this);
        }

    }
}
