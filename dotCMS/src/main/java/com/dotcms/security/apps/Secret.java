package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

/**
 * This is an implementation of a Secret
 * Class used to collect secrets destined to be stored into safe keeping.
 */
public final class Secret extends AbstractProperty<char[]> {

    private Secret(final char[] value, final Type type, final boolean hidden) {
        super(value, hidden, type);
    }

    @JsonCreator
    public static Secret newSecret(@JsonProperty("value") final char[] value,
            @JsonProperty("type") final Type type,
            @JsonProperty("hidden") final boolean hidden) {
        return new Secret(value, type, hidden);
    }

    public void destroy(){
        Arrays.fill(value, (char) 0);
    }

}
