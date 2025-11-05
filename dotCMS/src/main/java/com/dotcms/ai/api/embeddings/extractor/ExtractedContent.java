package com.dotcms.ai.api.embeddings.extractor;

import java.util.Objects;

public final class ExtractedContent {

    private final String inode;
    private final String identifier;
    private final long language;
    private final String host;
    private final String variant;
    private final String contentType;
    private final String title;
    private final String text;

    private ExtractedContent(Builder builder) {
        this.inode = Objects.requireNonNull(builder.inode, "inode cannot be null");
        this.identifier = Objects.requireNonNull(builder.identifier, "identifier cannot be null");
        this.language = builder.language;
        this.host = Objects.requireNonNull(builder.host, "host cannot be null");
        this.variant = Objects.requireNonNull(builder.variant, "variant cannot be null");
        this.contentType = Objects.requireNonNull(builder.contentType, "contentType cannot be null");
        this.title = Objects.requireNonNull(builder.title, "title cannot be null");
        this.text = Objects.requireNonNull(builder.text, "text cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExtractedContent of(String inode, String identifier, long language, String host, String variant, String contentType, String title, String text) {
        return builder()
                .withInode(inode)
                .withIdentifier(identifier)
                .withLanguage(language)
                .withHost(host)
                .withVariant(variant)
                .withContentType(contentType)
                .withTitle(title)
                .withText(text)
                .build();
    }
    // Getters
    public String getInode() {
        return inode;
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getLanguage() {
        return language;
    }

    public String getHost() {
        return host;
    }

    public String getVariant() {
        return variant;
    }

    public String getContentType() {
        return contentType;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    // equals(), hashCode(), toString()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtractedContent that = (ExtractedContent) o;
        return language == that.language &&
                Objects.equals(inode, that.inode) &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(host, that.host) &&
                Objects.equals(variant, that.variant) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(title, that.title) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inode, identifier, language, host, variant, contentType, title, text);
    }

    @Override
    public String toString() {
        return "ExtractedContent{" +
                "inode='" + inode + '\'' +
                ", identifier='" + identifier + '\'' +
                ", language=" + language +
                ", host='" + host + '\'' +
                ", variant='" + variant + '\'' +
                ", contentType='" + contentType + '\'' +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

    // Builder Class
    public static final class Builder {
        private String inode;
        private String identifier;
        private long language;
        private String host;
        private String variant = "DEFAULT"; // Valor por defecto
        private String contentType;
        private String title;
        private String text;

        private Builder() {}

        public Builder withInode(String inode) {
            this.inode = inode;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withLanguage(long language) {
            this.language = language;
            return this;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withVariant(String variant) {
            this.variant = variant;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withText(String text) {
            this.text = text;
            return this;
        }

        public ExtractedContent build() {
            return new ExtractedContent(this);
        }
    }
}
