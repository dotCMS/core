package com.dotcms.ai.api.embeddings;

import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.contenttype.model.field.Field;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class EmbeddingIndexRequest {

    private final String identifier;
    private final long languageId;
    private final String vendorModelPath;
    private final String indexName;
    private final AiModelConfig modelConfig;
    private final String velocityTemplate;
    private final Set<Field> fields;
    private final String userId; // author of the action

    private EmbeddingIndexRequest(final Builder builder) {
        this.identifier = Objects.requireNonNull(builder.identifier, "identifier cannot be null");
        this.languageId = builder.languageId;
        this.vendorModelPath = Objects.requireNonNull(builder.vendorModelPath, "vendorModelPath cannot be null");
        this.indexName = builder.indexName != null ? builder.indexName : "default";
        this.modelConfig = Objects.requireNonNull(builder.modelConfig, "modelConfig cannot be null");
        this.velocityTemplate = builder.velocityTemplate;
        this.fields = builder.fields;
        this.userId = builder.userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getVelocityTemplate() {
        return velocityTemplate;
    }

    public Set<Field> getFields() {
        return fields;
    }

    // Getters
    public String getIdentifier() {
        return identifier;
    }

    public long getLanguageId() {
        return languageId;
    }

    public String getVendorModelPath() {
        return vendorModelPath;
    }

    public String getIndexName() {
        return indexName;
    }

    public AiModelConfig getModelConfig() {
        return modelConfig;
    }

    @Override
    public String toString() {
        return "EmbeddingIndexRequest{" +
                "identifier='" + identifier + '\'' +
                ", languageId=" + languageId +
                ", embeddingProviderKey='" + vendorModelPath + '\'' +
                ", indexName='" + indexName + '\'' +
                ", modelConfig=" + modelConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingIndexRequest that = (EmbeddingIndexRequest) o;
        return languageId == that.languageId &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(vendorModelPath, that.vendorModelPath) &&
                Objects.equals(indexName, that.indexName) &&
                Objects.equals(modelConfig, that.modelConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, languageId, vendorModelPath, indexName, modelConfig);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder of() {
        return new Builder()
                .withIdentifier(this.identifier)
                .withLanguageId(this.languageId)
                .withVendorModelPath(this.vendorModelPath)
                .withIndexName(this.indexName)
                .withModelConfig(this.modelConfig);
    }

    public static final class Builder {
        private String identifier;
        private long languageId;
        private String vendorModelPath;
        private String indexName;
        private AiModelConfig modelConfig;
        private String velocityTemplate;
        private Set<Field> fields = new HashSet<>();
        private String userId;

        private Builder() {}

        public Builder withUser(User user) {
            return withUserId(user.getUserId());
        }

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withVelocityTemplate(String velocityTemplate) {
            this.velocityTemplate = velocityTemplate;
            return this;
        }

        public Builder withIdentifier(Field... fields) {
            return withIdentifier(Set.of(fields));
        }

        public Builder withIdentifier(Collection<Field> fields) {
            this.fields.addAll(fields);
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withLanguageId(long languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder withVendorModelPath(String vendorModelPath) {
            this.vendorModelPath = vendorModelPath;
            return this;
        }

        public Builder withIndexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder withModelConfig(AiModelConfig modelConfig) {
            this.modelConfig = modelConfig;
            return this;
        }

        public EmbeddingIndexRequest build() {
            return new EmbeddingIndexRequest(this);
        }
    }
}
