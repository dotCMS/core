package com.dotcms.rest.api.v2.languages;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLangVarsCount.class)
@JsonDeserialize(as = ImmutableLangVarsCount.class)
public interface LangVarsCount extends Serializable {
    int total();

    int count();
}
