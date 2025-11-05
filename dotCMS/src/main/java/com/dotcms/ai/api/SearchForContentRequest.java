package com.dotcms.ai.api;

import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.db.EmbeddingsDTO;

public class SearchForContentRequest {

    private final AiModelConfig chatModelConfig;
    private final String vendorModelPath;
    private final float temperature;
    private final String prompt;
    private final EmbeddingsDTO searcher;

    private SearchForContentRequest(final SearchForContentRequest.Builder builder) {

        this.chatModelConfig = builder.chatModelConfig;
        this.vendorModelPath = builder.vendorModelPath;
        this.temperature = builder.temperature;
        this.prompt = builder.prompt;
        this.searcher = builder.searcher;
    }

    public EmbeddingsDTO getSearcher() {
        return searcher;
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
        return "SearchForContentRequest{" +
                "chatModelConfig=" + chatModelConfig +
                ", vendorModelPath='" + vendorModelPath + '\'' +
                ", temperature=" + temperature +
                ", prompt='" + prompt + '\'' +
                ", searcher=" + searcher +
                '}';
    }

    public static SearchForContentRequest.Builder builder() {
        return new SearchForContentRequest.Builder();
    }

    public String getSystemPrompt() {
        return null; // todo: fill this thig
    }

    public static final class Builder {
        private AiModelConfig chatModelConfig;
        private String vendorModelPath;
        private Float temperature;
        private String prompt;
        private EmbeddingsDTO searcher;

        public SearchForContentRequest.Builder searcher(EmbeddingsDTO searcher) {
            this.searcher = searcher;
            return this;
        }

        public SearchForContentRequest.Builder chatModelConfig(AiModelConfig modelConfig) {
            this.chatModelConfig = modelConfig;
            return this;
        }

        public SearchForContentRequest.Builder vendorModelPath(String vendorModelPath) {
            this.vendorModelPath = vendorModelPath;
            return this;
        }

        public SearchForContentRequest.Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public SearchForContentRequest.Builder temperature(Float temperature) {
            this.temperature = temperature;
            return this;
        }

        public SearchForContentRequest build() {
            return new SearchForContentRequest(this);
        }
    }
}
