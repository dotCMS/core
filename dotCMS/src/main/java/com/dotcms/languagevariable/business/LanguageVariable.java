package com.dotcms.languagevariable.business;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableLanguageVariable.class)
@JsonDeserialize(as = ImmutableLanguageVariable.class)
@Value.Immutable
/*
 * Minimalist interface for LanguageVariable
 * We only keep the bare minimum to track the key, value and identifier
 * And keep a small memory footprint in cache
 */
public interface LanguageVariable extends Comparable<LanguageVariable> {

    String identifier();

    String key();

    String value();

    default int compareTo(LanguageVariable o) {
        return this.key().compareTo(o.key());
    }

}
