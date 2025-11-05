package com.dotcms.ai.api.embeddings;

import java.util.Objects;
import java.util.Optional;

public final class SearchMatch {

    private final String id;
    private final double score;
    private final String title;
    private final String snippet;
    private final String identifier;
    private final String contentType;
    private final String language;
    private final String host;
    private final String variant;
    private final String url;

    private SearchMatch(final Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "ID cannot be null");
        this.score = builder.score;
        this.title = Objects.requireNonNull(builder.title, "Title cannot be null");
        this.snippet = Objects.requireNonNull(builder.snippet, "Snippet cannot be null");
        this.identifier = builder.identifier;
        this.contentType = builder.contentType;
        this.language = builder.language;
        this.host = builder.host;
        this.variant = builder.variant;
        this.url = builder.url;
    }

    // Getters
    public String getId() {
        return id;
    }

    public double getScore() {
        return score;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public Optional<String> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    public Optional<String> getLanguage() {
        return Optional.ofNullable(language);
    }

    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public Optional<String> getVariant() {
        return Optional.ofNullable(variant);
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    // equals(), hashCode(), toString()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchMatch that = (SearchMatch) o;
        return Double.compare(that.score, score) == 0 &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(snippet, that.snippet) &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(language, that.language) &&
                Objects.equals(host, that.host) &&
                Objects.equals(variant, that.variant) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, score, title, snippet, identifier, contentType, language, host, variant, url);
    }

    @Override
    public String toString() {
        return "SearchMatch{" +
                "id='" + id + '\'' +
                ", score=" + score +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", identifier='" + identifier + '\'' +
                ", contentType='" + contentType + '\'' +
                ", language='" + language + '\'' +
                ", host='" + host + '\'' +
                ", variant='" + variant + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    // Builder Class
    public static final class Builder {
        private String id;
        private double score;
        private String title;
        private String snippet;
        private String identifier;
        private String contentType;
        private String language;
        private String host;
        private String variant;
        private String url;

        private Builder() {}

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withScore(double score) {
            this.score = score;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withSnippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withLanguage(String language) {
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

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public SearchMatch build() {
            return new SearchMatch(this);
        }
    }
}
