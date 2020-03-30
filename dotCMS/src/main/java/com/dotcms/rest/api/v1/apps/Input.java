package com.dotcms.rest.api.v1.apps;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class Input {

    @NotNull
    private char [] value;

    private boolean hidden;

    public Input(final char[] value, final boolean hidden) {
        this.value = value;
        this.hidden = hidden;
    }

    @JsonCreator
    public static Input newInputParam(@JsonProperty("value") final char[] value,
            @JsonProperty("hidden") final boolean hidden) {
        return new Input(value, hidden);
    }

    public static Input newInputParam(final char[] value) {
        return new Input(value, false);
    }

    public char[] getValue() {
        return value;
    }

    public boolean isHidden() {
        return hidden;
    }

    void destroySecret(){
        Arrays.fill(value, (char)0);
    }

}
