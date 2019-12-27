package com.dotcms.security.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

}
