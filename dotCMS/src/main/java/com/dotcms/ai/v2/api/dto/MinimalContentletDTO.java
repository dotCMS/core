package com.dotcms.ai.v2.api.dto;

public class MinimalContentletDTO {

    private final String contentId;
    private final long languageId;
    private final String title;

    private MinimalContentletDTO(final Builder builder) {

        this.contentId = builder.contentId;
        this.languageId = builder.languageId;
        this.title = builder.title;
    }

    public String getContentId() {
        return contentId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public String getTitle() {
        return title;
    }

    public static class Builder {

        private String contentId;
        private long languageId;
        private String title;

        public Builder contentId(String contentId) {
            this.contentId = contentId;
            return this;
        }

        public Builder languageId(long languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public MinimalContentletDTO build() {
            return new MinimalContentletDTO(this);
        }
    }
}
