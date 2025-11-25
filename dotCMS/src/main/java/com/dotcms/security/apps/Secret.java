package com.dotcms.security.apps;

import com.dotmarketing.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Arrays;
import java.util.Optional;

/**
 * This is an implementation of a Secret
 * Class used to collect secrets destined to be stored into safe keeping.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = Secret.Builder.class)
public final class Secret extends AbstractProperty<char[]> {

    private static final long serialVersionUID = 1L;

    public Secret(final char[] value,
                  final Boolean hidden,
                  final Type type,
                  final String envVar,
                  final Boolean envShow,
                  final char[] envValue) {
        super(value, hidden, type, envVar, envShow);
        setEnvVarValue(envValue);
    }

    public void destroy() {
        Arrays.fill(value, (char) 0);
        Optional.ofNullable(envVarValue).ifPresent(value -> Arrays.fill(value, (char) 0));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Secret Builder class.
     */
    @JsonPOJOBuilder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {

        private char[] value;
        private Boolean hidden;
        private Type type;
        @JsonProperty("envvar")
        private String envVar;
        @JsonProperty("envshow")
        private Boolean envShow;
        private char[] envValue;

        private Builder() {
        }

        public Builder withValue(final char[] value) {
            this.value = StringUtils.defensiveCopy(value);
            return this;
        }

        public Builder withValue(final String value) {
            return withValue(StringUtils.toCharArray(value));
        }

        public Builder withHidden(final Boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder withType(final Type type) {
            this.type = type;
            return this;
        }

        public Builder withEnvVar(final String envVar) {
            this.envVar = envVar;
            return this;
        }

        public Builder withEnvShow(final Boolean envShow) {
            this.envShow = envShow;
            return this;
        }

        public Builder withEnvValue(final char[] envValue) {
            this.envValue = StringUtils.defensiveCopy(envValue);
            return this;
        }

        public Builder withEnvValue(final String envValue) {
            return withEnvValue(StringUtils.toCharArray(envValue));
        }

        public Secret build() {
            return new Secret(value, hidden, type, envVar, envShow, envValue);
        }

    }

}
