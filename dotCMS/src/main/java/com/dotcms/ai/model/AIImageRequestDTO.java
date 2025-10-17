package com.dotcms.ai.model;


import com.dotcms.ai.app.AiAppConfig;
import com.dotcms.ai.app.ConfigService;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AIImageRequestDTO.Builder.class)
public class AIImageRequestDTO {

    private final String prompt;
    private final int numberOfImages;
    private final String size;
    private final String model;


    public AIImageRequestDTO(final Builder builder) {
        this.numberOfImages = builder.numberOfImages;
        this.model = builder.model;
        this.prompt = builder.prompt;
        this.size = builder.size;
    }

    public String getSize() {
        return size;
    }

    public int getNumberOfImages() {
        return numberOfImages;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getModel() {
        return model;
    }

    public static class Builder {
        private AiAppConfig appConfig = ConfigService.INSTANCE.config();
        @JsonSetter(nulls = Nulls.SKIP)
        private String prompt;
        @JsonSetter(nulls = Nulls.SKIP)
        private int numberOfImages = 1;
        @JsonSetter(nulls = Nulls.SKIP)
        private String size = appConfig.getImageSize();
        @JsonSetter(nulls = Nulls.SKIP)
        private String model = appConfig.getImageModel().getCurrentModel();

        public AIImageRequestDTO build() {
            return new AIImageRequestDTO(this);
        }

        public Builder numberOfImages(int numberOfImages) {
            this.numberOfImages = numberOfImages;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }
    }

}
