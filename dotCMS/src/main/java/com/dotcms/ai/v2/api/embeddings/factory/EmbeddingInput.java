package com.dotcms.ai.v2.api.embeddings.factory;

import java.util.Arrays;
import java.util.Objects;

/**
 * Simple input bean for an embedding row.
 * Fields: metadataId, modelName, dimensions, embedding.
 * Uses a Builder pattern and provides a static factory method {@code of}.
 */
public final class EmbeddingInput {

    private final long metadataId;
    private final String modelName;
    private final int dimensions;
    private final float[] embedding;

    private EmbeddingInput(Builder builder) {
        this.metadataId = builder.metadataId;
        this.modelName = builder.modelName;
        this.dimensions = builder.dimensions;
        this.embedding = builder.embedding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EmbeddingInput of(final long metadataId,
                                    final String modelName,
                                    final int dimensions,
                                    final float[] embedding) {
        return builder()
                .metadataId(metadataId)
                .modelName(modelName)
                .dimensions(dimensions)
                .embedding(embedding)
                .build();
    }

    public long getMetadataId() {
        return metadataId;
    }

    public String getModelName() {
        return modelName;
    }

    public int getDimensions() {
        return dimensions;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingInput that = (EmbeddingInput) o;
        return metadataId == that.metadataId &&
                dimensions == that.dimensions &&
                Objects.equals(modelName, that.modelName) &&
                Arrays.equals(embedding, that.embedding);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(metadataId, modelName, dimensions);
        result = 31 * result + Arrays.hashCode(embedding);
        return result;
    }

    @Override
    public String toString() {
        return "EmbeddingInput{" +
                "metadataId=" + metadataId +
                ", modelName='" + modelName + '\'' +
                ", dimensions=" + dimensions +
                ", embedding=" + Arrays.toString(embedding) +
                '}';
    }

    public static final class Builder {
        private long metadataId;
        private String modelName;
        private int dimensions;
        private float[] embedding;

        private Builder() {}

        public Builder metadataId(final long metadataId) {
            this.metadataId = metadataId;
            return this;
        }

        public Builder modelName(final String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder dimensions(final int dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder embedding(final float[] embedding) {
            this.embedding = embedding;
            return this;
        }

        public EmbeddingInput build() {
            return new EmbeddingInput(this);
        }
    }
}
