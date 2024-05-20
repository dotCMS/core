package com.dotcms.rest.api.v2.languages;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLangVarsCount.class)
@JsonDeserialize(as = ImmutableLangVarsCount.class)
public interface LangVarsCount {
    int total();

    int count();
}
