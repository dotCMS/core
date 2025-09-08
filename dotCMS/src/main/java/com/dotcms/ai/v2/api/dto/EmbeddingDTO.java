package com.dotcms.ai.v2.api.dto;

import java.util.Objects;

/**
 * Immutable DTO mapping the dot_embeddings row.
 * The raw vector is represented as a float[] for in-process usage.
 */
public final class EmbeddingDTO {

    private final Long id;
    private final Long metadataId;
    private final String modelName;
    private final Integer dimensions;
    private final float[] embedding; // keep small; avoid exposing huge arrays in logs

    private EmbeddingDTO(Builder b) {
        this.id = b.id;
        this.metadataId = b.metadataId;
        this.modelName = b.modelName;
        this.dimensions = b.dimensions;
        this.embedding = b.embedding;
    }

    /** Returns a pre-populated builder from an existing DTO (copy-for-modify). */
    public static Builder of(EmbeddingDTO dto) {
        return new Builder()
                .id(dto.id).metadataId(dto.metadataId).modelName(dto.modelName)
                .dimensions(dto.dimensions).embedding(dto.embedding);
    }

    public Long getId() { return id; }
    public Long getMetadataId() { return metadataId; }
    public String getModelName() { return modelName; }
    public Integer getDimensions() { return dimensions; }
    public float[] getEmbedding() { return embedding; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingDTO)) return false;
        EmbeddingDTO that = (EmbeddingDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(metadataId, that.metadataId) &&
                Objects.equals(modelName, that.modelName) &&
                Objects.equals(dimensions, that.dimensions);
        // Note: we intentionally skip deep array comparison for performance/safety
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, metadataId, modelName, dimensions);
    }

    @Override
    public String toString() {
        return "EmbeddingDTO{" +
                "id=" + id +
                ", metadataId=" + metadataId +
                ", modelName='" + modelName + '\'' +
                ", dimensions=" + dimensions +
                ", embedding=" + (embedding == null ? "null" : ("float[" + embedding.length + "]")) +
                '}';
    }

    /** Builder for {@link EmbeddingDTO}. */
    public static final class Builder {
        private Long id;
        private Long metadataId;
        private String modelName;
        private Integer dimensions;
        private float[] embedding;

        public Builder id(Long v){ this.id=v; return this; }
        public Builder metadataId(Long v){ this.metadataId=v; return this; }
        public Builder modelName(String v){ this.modelName=v; return this; }
        public Builder dimensions(Integer v){ this.dimensions=v; return this; }
        public Builder embedding(float[] v){ this.embedding=v; return this; }

        public EmbeddingDTO build(){ return new EmbeddingDTO(this); }
    }
}
