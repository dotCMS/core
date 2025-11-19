package com.dotcms.ai.api;

import com.dotcms.ai.api.embeddings.EmbeddingIndexRequest;

import java.util.Objects;


public final class SummarizeRequest {

    private final SearchForContentRequest searchForContentRequest;
    private final CompletionRequest completionRequest;

    private SummarizeRequest(final Builder builder) {
        this.searchForContentRequest = builder.searchForContentRequest;
        this.completionRequest = builder.completionRequest;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SummarizeRequest copyOf(SummarizeRequest request) {
        return new Builder()
                .searchForContentRequest(request.getSearchForContentRequest())
                .completionRequest(request.getCompletionRequest())
                .build();
    }

    public SearchForContentRequest getSearchForContentRequest() {
        return searchForContentRequest;
    }

    public CompletionRequest getCompletionRequest() {
        return completionRequest;
    }

    public static class Builder {
        private SearchForContentRequest searchForContentRequest;
        private CompletionRequest completionRequest;

        private Builder() {}

        public Builder searchForContentRequest(SearchForContentRequest searchForContentRequest) {
            this.searchForContentRequest = searchForContentRequest;
            return this;
        }

        public Builder completionRequest(CompletionRequest completionRequest) {
            this.completionRequest = completionRequest;
            return this;
        }

        public SummarizeRequest build() {
            // Opcional: Agregar validaciones aquí si es necesario (ej. que no sean nulos)
            Objects.requireNonNull(searchForContentRequest, "SearchForContentRequest cannot be null");
            Objects.requireNonNull(completionRequest, "CompletionRequest cannot be null");
            return new SummarizeRequest(this);
        }
    }

    // 6. equals y hashCode (Para comparación de objetos)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SummarizeRequest that = (SummarizeRequest) o;
        return Objects.equals(searchForContentRequest, that.searchForContentRequest) &&
                Objects.equals(completionRequest, that.completionRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchForContentRequest, completionRequest);
    }

    // 7. toString (Para logs y depuración)
    @Override
    public String toString() {
        return "SummarizeRequest{" +
                "searchForContentRequest=" + searchForContentRequest +
                ", completionRequest=" + completionRequest +
                '}';
    }
}
