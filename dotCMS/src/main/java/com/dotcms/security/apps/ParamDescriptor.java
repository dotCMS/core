package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is an implementation of a Parameter
 * Class used to collect properties or secrets set from the front-end
 * This is mostly used to pass values from the front-end into the Resource.
 */
public class ParamDescriptor extends AbstractProperty<String>{

    private final String label;

    private final String hint;

    private final boolean required;

    private ParamDescriptor(final String value, final boolean hidden, final Type type, final String label, final String hint, final boolean required) {
        super(value, hidden, type);
        this.label = label;
        this.hint = hint;
        this.required = required;
    }

    public String getLabel() {
        return label;
    }

    public String getHint() {
        return hint;
    }

    public boolean isRequired() {
        return required;
    }

    @JsonCreator
    public static ParamDescriptor newParam(@JsonProperty("value") final String value,
            @JsonProperty("hidden") final boolean hidden,
            @JsonProperty("type") final Type type,
            @JsonProperty("label") final String label,
            @JsonProperty("hint") final String hint,
            @JsonProperty("required") final boolean required) {
        return new ParamDescriptor(value, hidden, type, label, hint, required);
    }

}
