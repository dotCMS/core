package com.dotcms.languagevariable.business;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableLanguageVariable.class)
@JsonDeserialize(as = ImmutableLanguageVariable.class)
@Value.Immutable
public interface LanguageVariable extends Comparable<LanguageVariable> {

    String identifier();

    String key();

    String value();

    default int compareTo(LanguageVariable o) {
        return this.key().compareTo(o.key());
    }

}
