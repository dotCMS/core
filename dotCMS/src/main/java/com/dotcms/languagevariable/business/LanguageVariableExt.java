package com.dotcms.languagevariable.business;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableLanguageVariableExt.class)
@JsonDeserialize(as = ImmutableLanguageVariableExt.class)
@Value.Immutable
public interface LanguageVariableExt extends LanguageVariable {
    long languageId();
}
