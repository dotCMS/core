package com.dotcms.ai.model;


import com.dotcms.ai.app.AppConfig;
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
    public final String moderation;
    public final String imageFormat;
    public final String background;
    public final Integer quality;


    public AIImageRequestDTO(final Builder builder) {
        this.numberOfImages = builder.numberOfImages;
        this.model = builder.model;
        this.prompt = builder.prompt;
        this.size = builder.size;
        this.background = builder.background;
        this.imageFormat = builder.imageFormat;
        this.moderation = builder.moderation;
        this.quality=builder.quality;


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
        private AppConfig appConfig = ConfigService.INSTANCE.config();
        @JsonSetter(nulls = Nulls.SKIP)
        private String prompt;
        @JsonSetter(nulls = Nulls.SKIP)
        private int numberOfImages = 1;
        @JsonSetter(nulls = Nulls.SKIP)
        private String size = appConfig.getImageSize();
        @JsonSetter(nulls = Nulls.SKIP)
        private String model = appConfig.getImageModel().getCurrentModel();
        @JsonSetter(nulls = Nulls.SKIP)
        private  String moderation;
        @JsonSetter(nulls = Nulls.SKIP)
        private  String imageFormat;
        @JsonSetter(nulls = Nulls.SKIP)
        private  String background;
        @JsonSetter(nulls = Nulls.SKIP)
        private  Integer quality;

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

        public Builder moderation(String moderation) {
            this.moderation = moderation;
            return this;
        }

        public Builder background(String background) {
            this.background = background;
            return this;
        }


        public Builder quality(Integer quality) {
            this.quality = quality;
            return this;
        }

        public Builder imageFormat(String imageFormat) {
            this.imageFormat = imageFormat;
            return this;
        }


    }

}
