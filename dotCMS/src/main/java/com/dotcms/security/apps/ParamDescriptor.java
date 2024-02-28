package com.dotcms.security.apps;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This is an implementation of a Parameter
 * Class used to collect properties or secrets set from the front-end
 * This is mostly used to pass values from the front-end into the Resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = ParamDescriptor.Builder.class)
public final class ParamDescriptor extends AbstractProperty<Object>{

    private final String label;

    private final String hint;

    private final Boolean required;

    private ParamDescriptor(final Object value,
                            final Boolean hidden,
                            final Type type,
                            final String envVar,
                            final Boolean envShow,
                            final String label,
                            final String hint,
                            final Boolean required) {
        super(value, hidden, type, envVar, envShow);
        this.label = label;
        this.hint = hint;
        this.required = required;
    }

    /**
     * Label getter
     * @return
     */
    public String getLabel() {
        return label;
    }

    /**
     * Hint getter
     * @return
     */
    public String getHint() {
        return hint;
    }

    /**
     * required getter
     * @return
     */
    public boolean isRequired() {
        return UtilMethods.isSet(required) ? required : false ;
    }

    /**
     * required getter
     * @return
     */
    public Boolean getRequired() {
        return required;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * ParamDescriptor Builder class
     */
    @JsonPOJOBuilder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {

        private Object value;
        private Boolean hidden;
        private Type type;
        @JsonProperty("envvar")
        private String envVar;
        @JsonProperty("envshow")
        private Boolean envShow;
        private String label;
        private String hint;
        private Boolean required;

        private Builder() {
        }

        public Builder withValue(final Object value) {
            this.value = value;
            return this;
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

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withHint(final String hint) {
            this.hint = hint;
            return this;
        }

        public Builder withRequired(final Boolean required) {
            this.required = required;
            return this;
        }

        public ParamDescriptor build() {
            return new ParamDescriptor(value, hidden, type, envVar, envShow, label, hint, required);
        }

    }

}
