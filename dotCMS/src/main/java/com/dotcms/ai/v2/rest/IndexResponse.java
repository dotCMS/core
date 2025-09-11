package com.dotcms.ai.v2.rest;

import java.util.Objects;

public final class IndexResponse {

    private final int chunksIndexed;

    private IndexResponse(Builder builder) {
        this.chunksIndexed = builder.chunksIndexed;
    }

    public static IndexResponse of(int n) {
        return builder().withChunksIndexed(n).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getChunksIndexed() {
        return chunksIndexed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexResponse that = (IndexResponse) o;
        return chunksIndexed == that.chunksIndexed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunksIndexed);
    }

    @Override
    public String toString() {
        return "IndexResponse{" +
                "chunksIndexed=" + chunksIndexed +
                '}';
    }

    public static final class Builder {
        private int chunksIndexed;

        private Builder() {}

        public Builder withChunksIndexed(int chunksIndexed) {
            this.chunksIndexed = chunksIndexed;
            return this;
        }

        public IndexResponse build() {
            return new IndexResponse(this);
        }
    }
}
