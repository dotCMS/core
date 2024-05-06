package com.dotcms.ai.db;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JsonDeserialize(builder = EmbeddingsDTO.Builder.class)
public class EmbeddingsDTO implements Serializable {
    public final Float[] embeddings;
    public final String identifier;
    public final String inode;
    public final long language;
    public final String title;
    public final String[] contentType;

    public final String extractedText;
    public final String host;
    public final String indexName;
    public final String operator;
    public final int limit;
    public final int tokenCount;
    public final int offset;
    public final float threshold;
    public final String query;
    public final String[] showFields;
    public final User user;
    public final String[] excludeIdentifiers;
    public final String[] excludeInodes;
    @Min(0)
    @Max(2)
    public final float temperature;
    private final String[] operators = {"<->", "<=>", "<#>"};

    public static final String ALL_INDICES = "all";

    private EmbeddingsDTO(Builder builder) {
        this.embeddings = (builder.embeddings == null) ? new Float[0] : builder.embeddings.toArray(new Float[0]);
        this.identifier = builder.identifier;
        this.inode = builder.inode;
        this.language = builder.language;
        this.title = builder.title;
        this.contentType = UtilMethods.isSet(builder.contentType) ? builder.contentType.trim().split("[\\s+,]") : new String[0];

        this.extractedText = builder.extractedText;
        this.temperature = builder.temperature > 0 ? 0 : builder.temperature > 2 ? 2 : builder.temperature;

        this.host = builder.host;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.threshold = builder.threshold;
        this.operator = (Arrays.asList(operators).contains(builder.operator)) ? builder.operator : "<=>";
        this.indexName = UtilMethods.isSet(builder.indexName) ? builder.indexName : "default";
        this.tokenCount = builder.tokenCount;
        this.query = builder.query;
        this.user = builder.user;
        this.showFields = UtilMethods.isSet(builder.showFields) ? new String[0] : builder.showFields;
        this.excludeIdentifiers = UtilMethods.isSet(builder.excludeIdentifiers) ?  builder.excludeIdentifiers : new String[0] ;
        this.excludeInodes = UtilMethods.isSet(builder.excludeInodes) ? builder.excludeInodes:  new String[0] ;
    }

    public static Builder from(CompletionsForm form) {
        return new Builder()
                .withContentType(String.join(",",form.contentType))
                .withHost(form.site)
                .withQuery(form.prompt)
                .withIndexName(form.indexName)
                .withLimit(form.searchLimit)
                .withOffset(form.searchOffset)
                .withOperator(form.operator)
                .withThreshold(form.threshold)
                .withTemperature(form.temperature)
                .withTokenCount(form.responseLengthTokens);

    }

    public static Builder from(Map<String, Object> form) {


        return new Builder()
                .withContentType((String) form.get("contentType"))
                .withLanguage(Try.of(() -> Long.parseLong((String) form.get("language"))).getOrElse(APILocator.getLanguageAPI().getDefaultLanguage().getId()))
                .withHost((String) form.get("host"))
                .withQuery((String) form.get("query"))
                .withIndexName((String) form.get("indexName"))
                .withLimit(Try.of(() -> Integer.parseInt((String) form.get("limit"))).getOrElse(100))
                .withOffset(Try.of(() -> Integer.parseInt((String) form.get("offset"))).getOrElse(0))
                .withOperator((String) form.get("operator"))
                .withExcludeIndentifiers(Try.of(() -> ((String) form.get("excludeIdentifiers")).split("\\s+,")).getOrNull())
                .withExcludeInodes(Try.of(() -> ((String) form.get("excludeInodes")).split("\\s+,")).getOrNull())
                .withTemperature(Try.of(() -> Float.parseFloat(form.get("temperature").toString())).getOrElse(1f))
                .withThreshold((Try.of(() -> Float.parseFloat((String) form.get("threshold"))).getOrElse(.25f)));

    }

    public static Builder copy(EmbeddingsDTO values) {
        return new Builder()

                .withEmbeddings((values.embeddings == null) ? List.of() : Arrays.asList(values.embeddings))
                .withIdentifier(values.identifier)
                .withInode(values.inode)
                .withIndexName(values.indexName)
                .withLanguage(values.language)
                .withTitle(values.title)
                .withContentType(String.join("," ,values.contentType))
                .withExtractedText(values.extractedText)
                .withHost(values.host)
                .withLimit(values.limit)
                .withOffset(values.offset)
                .withThreshold(values.threshold)
                .withOperator(values.operator)
                .withIndexName(values.indexName)
                .withTokenCount(values.tokenCount)
                .withShowFields(values.showFields)
                .withExcludeIndentifiers(values.excludeIdentifiers)
                .withExcludeInodes(values.excludeInodes)
                .withTemperature(values.temperature)
                .withExcludeInodes(values.excludeInodes)
                .withUser(values.user)
                .withQuery(values.query);

    }

    @Override
    public String toString() {
        return "EmbeddingsDTO{" +
                "embeddings=" + Arrays.toString(embeddings) +
                ", identifier='" + identifier + '\'' +
                ", inode='" + inode + '\'' +
                ", language=" + language +
                ", title='" + title + '\'' +
                ", contentType='" + Arrays.toString(contentType) + '\'' +
                ", extractedText='" + extractedText + '\'' +
                ", host='" + host + '\'' +
                ", indexName='" + indexName + '\'' +
                ", operator='" + operator + '\'' +
                ", limit=" + limit +
                ", tokenCount=" + tokenCount +
                ", offset=" + offset +
                ", threshold=" + threshold +
                ", query='" + query + '\'' +
                ", showFields=" + Arrays.toString(showFields) +
                ", user=" + user +
                ", excludeIdentifiers=" + Arrays.toString(excludeIdentifiers) +
                ", excludeInodes=" + Arrays.toString(excludeInodes) +
                ", temperature=" + temperature +
                ", operators=" + Arrays.toString(operators) +
                '}';
    }

    public static final class Builder implements Serializable {

        @JsonProperty(defaultValue = ".25f")
        public float threshold = ConfigService.INSTANCE.config().getConfigFloat(AppKeys.EMBEDDINGS_SEARCH_DEFAULT_THRESHOLD);
        @JsonProperty(defaultValue = "<=>")
        public String operator = "<=>";
        @JsonProperty
        int tokenCount = 0;
        @JsonProperty
        private List<Float> embeddings;
        @JsonProperty
        private String identifier;
        @JsonProperty
        private String inode;
        @JsonProperty
        private long language;
        @JsonProperty
        private String title;
        @JsonProperty
        private String contentType;
        @JsonProperty(defaultValue = "default")
        private String indexName = "default";
        @JsonProperty
        private String extractedText;
        @JsonProperty
        private String host;
        @JsonProperty(defaultValue = "100")
        private int limit = 100;
        @JsonProperty(defaultValue = "0")
        private int offset = 0;
        @JsonProperty
        private String query;
        @JsonSetter(nulls = Nulls.SKIP)
        private float temperature = 1f;

        @JsonProperty(defaultValue = "")
        private String[] excludeIdentifiers;

        @JsonProperty(defaultValue = "")
        private String[] excludeInodes;

        private User user;
        @JsonProperty(defaultValue = "")
        private String[] showFields;


        public Builder withEmbeddings(List<Float> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withExcludeIndentifiers(String[] exclude) {
            if (exclude != null && exclude.length > 0) {
                this.excludeIdentifiers = exclude;
            }
            return this;
        }
        public Builder withExcludeInodes(String[] exclude) {
            if (exclude != null && exclude.length > 0) {
                this.excludeInodes = exclude;
            }
            return this;
        }

        public Builder withTemperature(float temperature) {
            this.temperature = temperature > 0 ? 0 : temperature > 2 ? 2 : temperature;
            return this;
        }

        public Builder withQuery(String query) {
            this.query = query;
            return this;
        }

        public Builder withIndexName(String indexName) {
            this.indexName = UtilMethods.isSet(indexName) ? indexName : "default";
            return this;
        }

        public Builder withOperator(String distanceOperator) {
            this.operator = distanceOperator;
            return this;
        }

        public Builder withShowFields(String[] showFields) {
            this.showFields = showFields;
            return this;
        }

        public Builder withUser(User user) {
            this.user = user;
            return this;
        }

        public Builder withThreshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder withExtractedText(String extractedText) {
            this.extractedText = extractedText;
            return this;
        }

        public Builder withInode(String inode) {
            this.inode = inode;
            return this;
        }

        public Builder withLanguage(long language) {
            this.language = language;
            return this;
        }

        public Builder withLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder withTokenCount(int tokenCount) {
            this.tokenCount = tokenCount;
            return this;
        }

        public Builder withOffset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }


        public EmbeddingsDTO build() {
            return new EmbeddingsDTO(this);
        }
    }
}
