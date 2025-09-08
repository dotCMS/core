package com.dotcms.ai.v2.api.embeddings;

import java.util.Objects;

/**
 * Simple input bean representing the unique content metadata key
 * (identifier, language, host, variant). Uses a Builder pattern and
 * provides a static factory method {@code of}.
 */
public final class ContentMetadataInput {

    private final String identifier;
    private final long language;
    private final String host;
    private final String variant;

    private ContentMetadataInput(Builder builder) {
        this.identifier = builder.identifier;
        this.language = builder.language;
        this.host = builder.host;
        this.variant = builder.variant;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Static factory method for convenience.
     */
    public static ContentMetadataInput of(final String identifier,
                                          final long language,
                                          final String host,
                                          final String variant) {
        return builder()
                .identifier(identifier)
                .language(language)
                .host(host)
                .variant(variant)
                .build();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentMetadataInput that = (ContentMetadataInput) o;
        return language == that.language &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(host, that.host) &&
                Objects.equals(variant, that.variant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, language, host, variant);
    }

    @Override
    public String toString() {
        return "ContentMetadataInput{" +
                "identifier='" + identifier + '\'' +
                ", language=" + language +
                ", host='" + host + '\'' +
                ", variant='" + variant + '\'' +
                '}';
    }

    public static final class Builder {
        private String identifier;
        private long language;
        private String host;
        private String variant;

        private Builder() {}

        public Builder identifier(final String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder language(final long language) {
            this.language = language;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder variant(final String variant) {
            this.variant = variant;
            return this;
        }

        public ContentMetadataInput build() {
            return new ContentMetadataInput(this);
        }
    }
}
