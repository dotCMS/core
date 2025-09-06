package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.api.provider.Model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ChatRequestForm.Builder.class)
public class ChatRequestForm {

    private final String conversationId; // optional if token present
    private final String prompt;
    private final String modelProvider;

    public ChatRequestForm(Builder builder) {
        this.conversationId = builder.conversationId;
        this.prompt = builder.prompt;
        this.modelProvider = builder.modelProvider;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getModelProvider() {
        return modelProvider;
    }

    public static final class Builder {
        @JsonProperty(required = false) private String conversationId; // not present on create
        @JsonProperty(required = false) private String modelProvider = Model.OPEN_AI_GPT_40.getProviderName(); // not present on create
        @JsonProperty(required = true) private String prompt;

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder modelProvider(String modelProvider) {
            this.modelProvider = modelProvider;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }


        public ChatRequestForm build() {
            return new ChatRequestForm(this);
        }
    }
}
