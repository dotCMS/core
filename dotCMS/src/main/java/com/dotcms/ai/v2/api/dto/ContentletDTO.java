package com.dotcms.ai.v2.api.dto;

import java.util.Map;

public class ContentletDTO {

    private final String contentId;
    private final long languageId;
    private final String title;
    private final String body;
    private final Map<String, Object> meta;

    private ContentletDTO(final Builder builder) {

        this.contentId = builder.contentId;
        this.languageId = builder.languageId;
        this.title = builder.title;
        this.body = builder.body;
        this.meta = builder.meta;
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

    public String getBody() {
        return body;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public static class Builder {

        private String contentId;
        private long languageId;
        private String title;
        private String body;
        private Map<String, Object> meta;

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

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public ContentletDTO build() {
            return new ContentletDTO(this);
        }
    }
}
