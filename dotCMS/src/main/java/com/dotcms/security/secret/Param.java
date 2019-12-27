package com.dotcms.security.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Param extends AbstractProperty<String>{

    private String label;

    private String hint;

    private Param(final String value, final boolean hidden, final Type type, final String label, final String hint) {
        super(value, hidden, type);
        this.label = label;
        this.hint = hint;
    }

    public String getLabel() {
        return label;
    }

    public String getHint() {
        return hint;
    }

    @JsonCreator
    public static Param newParam(@JsonProperty("value") final String value,
            @JsonProperty("hidden") final boolean hidden,
            @JsonProperty("type") final Type type,
            @JsonProperty("label") final String label,
            @JsonProperty("hint") final String hint) {
        return new Param(value, hidden, type, label, hint);
    }

}
