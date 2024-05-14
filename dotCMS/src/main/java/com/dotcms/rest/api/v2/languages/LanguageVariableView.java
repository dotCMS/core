package com.dotcms.rest.api.v2.languages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonInclude(Include.NON_NULL)
@JsonSerialize(as = ImmutableLanguageVariableView.class)
@JsonDeserialize(as = ImmutableLanguageVariableView.class)
/*
 * Small json View object for LanguageVariable
 */
public interface LanguageVariableView {
    String identifier();
    String value();

}
