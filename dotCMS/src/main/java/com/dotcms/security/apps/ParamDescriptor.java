package com.dotcms.security.apps;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * This is an implementation of a Parameter
 * Class used to collect properties or secrets set from the front-end
 * This is mostly used to pass values from the front-end into the Resource.
 */
public class ParamDescriptor extends AbstractProperty<Object>{

    private final String label;

    private final String hint;

    private final Boolean required;

    /**
     * Private Constructor
     * @param value
     * @param hidden
     * @param type
     * @param label
     * @param hint
     * @param required
     */
    private ParamDescriptor(final Object value, final Boolean hidden, final Type type, final String label, final String hint, final Boolean required) {
        super(value, hidden, type);
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

    @JsonCreator
    public static ParamDescriptor newParam(
            @JsonProperty("value") final Object value,
            @JsonProperty("hidden") final Boolean hidden,
            @JsonProperty("type") final Type type,
            @JsonProperty("label") final String label,
            @JsonProperty("hint") final String hint,
            @JsonProperty("required") final Boolean required) {
        return new ParamDescriptor(value, hidden, type, label, hint, required);
    }

}
