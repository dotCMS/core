package com.dotcms.languagevariable.business;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableLanguageVariableExt.class)
@JsonDeserialize(as = ImmutableLanguageVariableExt.class)
@Value.Immutable
/**
 * This is a slightly more complex version of the LanguageVariable interface that includes the languageId.
 * This is useful when you need to know the language of the variable. e.g. for grouping purposes.
 * This object does not make into cache and is used for pagination purposes.
 */
public interface LanguageVariableExt extends LanguageVariable {
    long languageId();
}
