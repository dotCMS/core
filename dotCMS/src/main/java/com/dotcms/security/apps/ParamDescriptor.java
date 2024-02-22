package com.dotcms.security.apps;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * This is an implementation of a Parameter
 * Class used to collect properties or secrets set from the front-end
 * This is mostly used to pass values from the front-end into the Resource.
 */
public final class ParamDescriptor extends AbstractProperty<Object>{

    private final String label;

    private final String hint;

    private final Boolean required;

    private ParamDescriptor(final Object value,
                            final Boolean hidden,
                            final Type type,
                            final String envVar,
                            final Boolean envShow,
                            final String envValue,
                            final String label,
                            final String hint,
                            final Boolean required) {
        super(value, hidden, type, envVar, envShow);
        Optional.ofNullable(envValue).ifPresent(ev -> setEnvValue(ev.toCharArray()));
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

    @Override
    public boolean isHidden() {
        return super.isHidden();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isEditable() {
        return isEnvShow() || !UtilMethods.isSet(String.valueOf(value));
    }

    @JsonCreator
    public static ParamDescriptor newParam(
            @JsonProperty("value") final Object value,
            @JsonProperty("hidden") final Boolean hidden,
            @JsonProperty("type") final Type type,
            @JsonProperty("envvar") final String envVar,
            @JsonProperty("envshow") final Boolean envShow,
            @JsonProperty("label") final String label,
            @JsonProperty("hint") final String hint,
            @JsonProperty("required") final Boolean required) {
        return new ParamDescriptor(value, hidden, type, envVar, envShow, null, label, hint, required);
    }

    public static ParamDescriptor newParam(
            final Object value,
            final Boolean hidden,
            final Type type,
            final String label,
            final String hint,
            final Boolean required) {
        return newParam(value, hidden, type, null, null, label, hint, required);
    }

}
