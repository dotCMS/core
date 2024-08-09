package com.dotcms.ai.domain;

import java.io.OutputStream;

public class AIResponse {

    private final String response;
    private final OutputStream output;

    private AIResponse(final String response, final OutputStream output) {
        this.response = response;
        this.output = output;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getResponse() {
        return response;
    }

    public OutputStream getOutput() {
        return output;
    }

    public static class Builder {

        private String response;
        private OutputStream output;

        public Builder response(final String response) {
            this.response = response;
            return this;
        }

        public Builder output(final OutputStream output) {
            this.output = output;
            return this;
        }

        public AIResponse build() {
            return new AIResponse(response, output);
        }

    }
}
