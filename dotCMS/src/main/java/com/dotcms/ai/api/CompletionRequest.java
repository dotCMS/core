package com.dotcms.ai.api;

import com.dotcms.ai.config.AiModelConfig;

public class CompletionRequest {

    private final AiModelConfig chatModelConfig;
    private final String vendorModelPath;
    private final float temperature;
    private final String prompt;

    private CompletionRequest(final Builder builder) {

        this.chatModelConfig = builder.chatModelConfig;
        this.vendorModelPath = builder.vendorModelPath;
        this.temperature = builder.temperature;
        this.prompt = builder.prompt;
    }

    public String getVendorModelPath() {
        return vendorModelPath;
    }

    public float getTemperature() {
        return temperature;
    }

    public AiModelConfig getChatModelConfig() {
        return this.chatModelConfig;
    }

    public String getPrompt() {
        return prompt;
    }

    @Override
    public String toString() {
        return "CompletionRequest{" +
                "chatModelConfig=" + chatModelConfig +
                ", vendorModelPath='" + vendorModelPath + '\'' +
                ", temperature=" + temperature +
                ", prompt='" + prompt + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSystemPrompt() {
        return null; // todo: fill this thig
    }

    public static final class Builder {
        private AiModelConfig chatModelConfig;
        private String vendorModelPath;
        private float temperature;
        private String prompt;


        public Builder chatModelConfig(AiModelConfig modelConfig) {
            this.chatModelConfig = modelConfig;
            return this;
        }

        public Builder vendorModelPath(String vendorModelPath) {
            this.vendorModelPath = vendorModelPath;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public CompletionRequest build() {
            return new CompletionRequest(this);
        }
    }
}
