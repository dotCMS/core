package com.dotcms.ai.rest.forms;

import com.dotcms.ai.app.AiAppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.hibernate.validator.constraints.Length;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = CompletionsForm.Builder.class)
public class CompletionsForm {

    private static final Map<String, String> OPERATORS = Map.of(
            "distance",
            "<->",
            "cosine",
            "<=>",
            "innerProduct",
            "<#>");

    @Length(min = 1, max = 4096)
    public final String prompt;
    @Min(1)
    @Max(1000)
    public final int searchLimit;
    @Min(0)
    public final int searchOffset;
    @Min(128)
    public final int responseLengthTokens;
    public final long language;
    public final boolean stream;
    public final String fieldVar;
    public final String indexName;
    public final String[] contentType;
    public final float threshold;
    @Min(0)
    @Max(2)
    public final float temperature;
    public final String model;
    public final String operator;
    public final String site;
    public final User user;
    public final Map<String, Object> responseFormat;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CompletionsForm)) return false;
        CompletionsForm that = (CompletionsForm) o;
        return searchLimit == that.searchLimit &&
                searchOffset == that.searchOffset &&
                responseLengthTokens == that.responseLengthTokens &&
                language == that.language &&
                stream == that.stream &&
                Float.compare(threshold, that.threshold) == 0 &&
                Float.compare(temperature, that.temperature) == 0 &&
                Objects.equals(prompt, that.prompt) &&
                Objects.equals(fieldVar, that.fieldVar) &&
                Objects.equals(indexName, that.indexName) &&
                Objects.equals(model, that.model) &&
                Objects.equals(operator, that.operator) &&
                Objects.equals(site, that.site) &&
                contentType.length > 0 ? Arrays.equals(contentType, that.contentType) : Boolean.TRUE;
    }

    @Override
    public String toString() {
        return "CompletionsForm{" +
                "prompt='" + prompt + '\'' +
                ", searchLimit=" + searchLimit +
                ", searchOffset=" + searchOffset +
                ", responseLengthTokens=" + responseLengthTokens +
                ", language=" + language +
                ", stream=" + stream +
                ", fieldVar='" + fieldVar + '\'' +
                ", indexName='" + indexName + '\'' +
                ", threshold=" + threshold +
                ", temperature=" + temperature +
                ", model='" + model + '\'' +
                ", operator='" + operator + '\'' +
                ", site='" + site + '\'' +
                ", contentType=" + Arrays.toString(contentType) +
                ", user=" + user +
                '}';
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(prompt, searchLimit, searchOffset, responseLengthTokens, language, stream, fieldVar, indexName, threshold, temperature, model, operator, site);
        result = 31 * result + Arrays.hashCode(contentType);
        return result;
    }

    private CompletionsForm(final Builder builder) {
        this.prompt = validateBuilderQuery(builder.prompt);
        this.searchLimit = builder.searchLimit;
        this.fieldVar = builder.fieldVar;
        this.responseLengthTokens = builder.responseLengthTokens;
        this.stream = builder.stream;
        this.indexName = builder.indexName;

        this.threshold = builder.threshold;
        this.operator = OPERATORS.getOrDefault(builder.operator, "<=>");
        this.site = UtilMethods.isSet(builder.site) ? builder.site : null;
        this.language = validateLanguage(builder.language);
        this.searchOffset = builder.searchOffset;
        this.contentType = UtilMethods.isSet(builder.contentType) ? AiAppConfig.SPLITTER.split(builder.contentType) : new String[0];
        if (builder.temperature <= 0) {
            this.temperature = ConfigService.INSTANCE.config().getConfigFloat(AppKeys.COMPLETION_TEMPERATURE);
        } else {
            this.temperature = builder.temperature >= 2 ? 2 : builder.temperature;
        }
        this.model = UtilMethods.isSet(builder.model) ? builder.model : ConfigService.INSTANCE.config().getModel().getCurrentModel();
        this.user = builder.user;
        this.responseFormat = builder.responseFormat;
    }

    private String validateBuilderQuery(final String query) {
        if (UtilMethods.isEmpty(query)) {
            throw new IllegalArgumentException("query/prompt cannot be null");
        }
        return String.join(" ", query.trim().split("\\s+"));
    }

    private long validateLanguage(final String language) {
        return Try.of(() -> Long.parseLong(language))
                .recover(x -> APILocator.getLanguageAPI().getLanguage(language).getId())
                .getOrElseTry(() -> APILocator.getLanguageAPI().getDefaultLanguage().getId());
    }

    public static Builder copy(final CompletionsForm form) {
        return new Builder()
                .temperature(form.temperature)
                .site(form.site)
                .searchLimit(form.searchLimit)
                .contentType(String.join(",", form.contentType))
                .fieldVar(form.fieldVar)
                .searchOffset(form.searchOffset)
                .model(form.model)
                .responseLengthTokens(form.responseLengthTokens)
                .language(form.language)
                .prompt(form.prompt)
                .operator(form.operator)
                .indexName(form.indexName)
                .threshold(form.threshold)
                .stream(form.stream)
                .user(form.user)
                .responseFormat(form.responseFormat);
    }

    public static final class Builder {

        @JsonSetter(nulls = Nulls.SKIP)
        private String prompt;
        @JsonSetter(nulls = Nulls.SKIP)
        private int searchLimit = 50;
        @JsonSetter(nulls = Nulls.SKIP)
        private String language;
        @JsonSetter(nulls = Nulls.SKIP)
        private int searchOffset = 0;
        @JsonSetter(nulls = Nulls.SKIP)
        private int responseLengthTokens = 0;
        @JsonSetter(nulls = Nulls.SKIP)
        private boolean stream = false;
        @JsonSetter(nulls = Nulls.SKIP)
        private String fieldVar;
        @JsonSetter(nulls = Nulls.SKIP)
        private String indexName = "default";
        @JsonSetter(nulls = Nulls.SKIP)
        private String model ;
        @JsonSetter(nulls = Nulls.SKIP)
        private String contentType;
        @JsonSetter(nulls = Nulls.SKIP)
        private float threshold = .25f;
        @JsonSetter(nulls = Nulls.SKIP)
        private float temperature = 0;
        @JsonSetter(nulls = Nulls.SKIP)
        private String operator = "cosine";
        @JsonSetter(nulls = Nulls.SKIP)
        private String site;
        @JsonSetter(nulls = Nulls.SKIP)
        private User user;
        @JsonSetter(nulls = Nulls.SKIP)
        private Map<String, Object> responseFormat;

        public Builder prompt(String queryOrPrompt) {
            this.prompt = queryOrPrompt;
            return this;
        }

        public Builder searchLimit(int searchLimit) {
            this.searchLimit = searchLimit;
            return this;
        }

        public Builder language(long language) {
            this.language = String.valueOf(language);
            return this;
        }

        public Builder language(String language) {
            this.language = String.valueOf(language);
            return this;
        }

        public Builder searchOffset(int searchOffset) {
            this.searchOffset = searchOffset;
            return this;
        }

        public Builder responseLengthTokens(int responseLengthTokens) {
            this.responseLengthTokens = responseLengthTokens;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder fieldVar(String fieldVar) {
            this.fieldVar = fieldVar;
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

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder threshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder site(String site) {
            this.site = site;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder responseFormat(Map<String, Object> responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public CompletionsForm build() {
            return new CompletionsForm(this);
        }

    }

}
