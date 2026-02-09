package com.dotcms.ai.api.embeddings.extractor;

import com.dotcms.contenttype.model.field.Field;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ExtractorContentInput {

    private final String identifier;
    private final long languageId;
    private final String velocityTemplate;
    private final Set<Field> fields;
    private final String userId;

    private ExtractorContentInput(final ExtractorContentInput.Builder builder) {
        this.identifier = Objects.requireNonNull(builder.identifier, "identifier cannot be null");
        this.languageId = builder.languageId;
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

    @Override
    public String toString() {
        return "ExtractorContentInput{" +
                "identifier='" + identifier + '\'' +
                ", languageId=" + languageId +
                ", velocityTemplate='" + velocityTemplate + '\'' +
                ", fields=" + fields +
                ", userId='" + userId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ExtractorContentInput that = (ExtractorContentInput) o;
        return languageId == that.languageId && Objects.equals(identifier, that.identifier) && Objects.equals(velocityTemplate, that.velocityTemplate) && Objects.equals(fields, that.fields) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, languageId, velocityTemplate, fields, userId);
    }

    public static ExtractorContentInput.Builder builder() {
        return new ExtractorContentInput.Builder();
    }

    public ExtractorContentInput.Builder of() {
        return new ExtractorContentInput.Builder()
                .withIdentifier(this.identifier)
                .withLanguageId(this.languageId)
                .withVelocityTemplate(this.velocityTemplate)
                .withFields(this.fields)
                .withUserId(this.userId);
    }

    public static final class Builder {
        private String identifier;
        private long languageId;
        private String velocityTemplate;
        private Set<Field> fields = new HashSet<>();
        private String userId;

        private Builder() {}

        public ExtractorContentInput.Builder withUser(User user) {
            return withUserId(user.getUserId());
        }

        public ExtractorContentInput.Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public ExtractorContentInput.Builder withVelocityTemplate(String velocityTemplate) {
            this.velocityTemplate = velocityTemplate;
            return this;
        }

        public ExtractorContentInput.Builder withFields(Field... fields) {
            return withFields(Set.of(fields));
        }

        public ExtractorContentInput.Builder withFields(Collection<Field> fields) {
            this.fields.addAll(fields);
            return this;
        }

        public ExtractorContentInput.Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public ExtractorContentInput.Builder withLanguageId(long languageId) {
            this.languageId = languageId;
            return this;
        }


        public ExtractorContentInput build() {
            return new ExtractorContentInput(this);
        }
    }
}
