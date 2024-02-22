package com.dotcms.security.apps;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * This is an implementation of a Secret
 * Class used to collect secrets destined to be stored into safe keeping.
 */
public final class Secret extends AbstractProperty<char[]> {

    public Secret(final char[] value,
                  final Boolean hidden,
                  final Type type,
                  final String envVar,
                  final boolean envShow,
                  final char[] envValue) {
        super(value, hidden, type, envVar, envShow);
        setEnvValue(envValue);
    }

    /*@Override
    public boolean isHidden() {
        return (UtilMethods.isSet(hidden) ? hidden : false) || (!isEnvShow() && isEnvValueSet());
    }*/

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isEditable() {
        return isEnvShow() || (!UtilMethods.isSet(String.valueOf(value)) && Objects.isNull(envValue));
    }

    @JsonCreator
    public static Secret newSecret(@JsonProperty("value") final char[] value,
                                   @JsonProperty("hidden") final boolean hidden,
                                   @JsonProperty("type") final Type type,
                                   @JsonProperty("envvar") final String envVar,
                                   @JsonProperty("envshow") final boolean envShow,
                                   @JsonProperty("envvalue") final String envValue) {

        final char[] valueCopy = defensiveCopy(value);
        final char[] envValueCopy = defensiveCopy(Optional.ofNullable(envValue).map(String::toCharArray).orElse(new char[0]));
        return new Secret(valueCopy, hidden, type, envVar, envShow, envValueCopy);
    }

    public static Secret newSecret(final char[] value, final boolean hidden, final Type type) {
        return newSecret(value, hidden, type, null, true, null);
    }

    public void destroy(){
        Arrays.fill(value, (char) 0);
        Optional.ofNullable(envValue).ifPresent(value -> Arrays.fill(value, (char) 0));
    }

    private static char[] defensiveCopy(final char[] value) {
        return Optional.ofNullable(value).map(v -> Arrays.copyOf(v, v.length)).orElse(null);
    }

}
