package com.dotcms.security.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Secret {

    private final char[] value;
    private final boolean hidden;
    private final SecretType secretType;

    private Secret(final char[] value, final SecretType secretType, final boolean hidden) {
        this.value = value;
        this.secretType = secretType;
        this.hidden = hidden;
    }

    @JsonIgnore
    public String getString() {
        return String.valueOf(value);
    }

    @JsonIgnore
    public boolean getBoolean() {
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean isHidden() {
        return hidden;
    }

    public SecretType getSecretType() {
        return secretType;
    }

    @JsonCreator
    public static Secret newSecret(@JsonProperty("value") final char[] value,
            @JsonProperty("secretType") final SecretType secretType,
            @JsonProperty("hidden") final boolean hidden) {
        return new Secret(value, secretType, hidden);
    }

}
