package com.dotcms.rest.api.v2.languages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableLanguageVariablePageView.class)
@JsonDeserialize(as = ImmutableLanguageVariablePageView.class)
@Value.Immutable
public interface LanguageVariablePageView {
    Map<String, Map<String,LanguageVariableView>> variables();

}
