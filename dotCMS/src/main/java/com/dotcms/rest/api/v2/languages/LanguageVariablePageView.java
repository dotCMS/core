package com.dotcms.rest.api.v2.languages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.LinkedHashMap;
import java.util.Map;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableLanguageVariablePageView.class)
@JsonDeserialize(as = ImmutableLanguageVariablePageView.class)
@Value.Immutable
/*
 *  This is a view object that represents a page of LanguageVariableView objects.
 */
public interface LanguageVariablePageView {

    /**
     * The variables table
     * @return the variables table
     */
    LinkedHashMap<String, Map<String,LanguageVariableView>> variables();
    /**
     * The total number of unique variables
     * @return the total number of unique variables
     */
    int total();
}
