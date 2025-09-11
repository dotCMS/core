package com.dotcms.ai.v2.api;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;

public class ChatRequest {

    private final ModelConfig modelConfig;
    private final String modelProviderKey;
    private final String conversationId;
    private final String prompt;

    private ChatRequest(final Builder builder) {

        this.modelConfig = builder.modelConfig;
        this.modelProviderKey = builder.modelProviderKey;
        this.conversationId = builder.conversationId;
        this.prompt = builder.prompt;
    }

    public String getModelProviderKey() {
        return this.modelProviderKey;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public ModelConfig getModelConfig() {
        return this.modelConfig;
    }

    public String getPrompt() {
        return prompt;
    }


    public static final class Builder {
        private ModelConfig modelConfig;
        private String modelProviderKey;
        private String conversationId;
        private String prompt;


        public Builder modelConfig(ModelConfig modelConfig) {
            this.modelConfig = modelConfig;
            return this;
        }

        public Builder modelProviderKey(String modelProviderKey) {
            this.modelProviderKey = modelProviderKey;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(this);
        }
    }
}
