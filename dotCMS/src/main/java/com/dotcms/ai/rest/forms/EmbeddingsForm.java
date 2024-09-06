package com.dotcms.ai.rest.forms;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.liferay.portal.util.PortalUtil;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = EmbeddingsForm.Builder.class)
public class EmbeddingsForm {

    @Size(min = 1, max = 4096)
    public final String query;

    @Min(1)
    @Max(1000)
    public final int limit;

    @Min(0)
    public final int offset;
    public final String indexName;
    public final String model;
    public final String velocityTemplate;
    public final String[] fields;
    public final String userId;
    public List<String> fieldsAsList(){
        return Arrays.asList(fields);
    }

    private EmbeddingsForm(Builder builder) {
        this.query = validateBuilderQuery(builder.query);
        this.limit = builder.limit;
        this.indexName = UtilMethods.isSet(builder.indexName) ? builder.indexName : "default";
        this.velocityTemplate = builder.velocityTemplate;
        this.offset = builder.offset;
        this.model = UtilMethods.isSet(builder.model) ? builder.model : ConfigService.INSTANCE.config().getEmbeddingsModel().getCurrentModel();
        this.fields = (builder.fields != null) ? AppConfig.SPLITTER.split(builder.fields.toLowerCase()) : new String[0];
        this.userId= PortalUtil.getUser() != null ? PortalUtil.getUser().getUserId() : APILocator.systemUser().getUserId();
    }

    String validateBuilderQuery(String query) {
        if (UtilMethods.isEmpty(query)) {
            throw new IllegalArgumentException("query cannot be null");
        }
        return String.join(" ", query.trim().split("\\s+"));
    }

    public static final Builder copy(EmbeddingsForm form) {
        return new Builder()
                .limit(form.limit < 1 ? 1000 : form.limit)
                .offset(form.offset < 0 ? 0 : form.offset)
                .model(form.model)
                .query(form.query)
                .fields(String.join(",", form.fields))
                .velocityTemplate(form.velocityTemplate)
                .indexName(form.indexName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(query, limit, offset, indexName, velocityTemplate, model);
        result = 31 * result + Arrays.hashCode(fields);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingsForm)) return false;
        EmbeddingsForm that = (EmbeddingsForm) o;
        return limit == that.limit &&
                offset == that.offset &&
                Objects.equals(query, that.query) &&

                Objects.equals(indexName, that.indexName) &&
                Objects.equals(velocityTemplate, that.velocityTemplate) &&
                fields.length > 0 ? Arrays.equals(fields, that.fields) : Boolean.TRUE &&
                Objects.equals(model, that.model);
    }

    @Override
    public String toString() {
        return "CompletionsForm{" +
                "query='" + query + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", fields='" + String.join(",", fields) + '\'' +
                ", indexName='" + indexName + '\'' +
                ", velocityTemplate='" + velocityTemplate + '\'' +
                '}';
    }

    public static final class Builder {
        @JsonSetter(nulls = Nulls.SKIP)
        public String fields;
        @JsonSetter(nulls = Nulls.SKIP)
        private String query;
        @JsonSetter(nulls = Nulls.SKIP)
        private int limit = 1000;

        @JsonSetter(nulls = Nulls.SKIP)
        private int offset = 0;

        @JsonSetter(nulls = Nulls.SKIP)
        private String indexName = "default";

        @JsonSetter(nulls = Nulls.SKIP)
        private String model;

        @JsonSetter(nulls = Nulls.SKIP)
        private String velocityTemplate;


        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder fields(String fields) {
            this.fields = fields;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder velocityTemplate(String velocityTemplate) {
            this.velocityTemplate = velocityTemplate;
            return this;
        }

        public EmbeddingsForm build() {
            return new EmbeddingsForm(this);
        }

    }

}
