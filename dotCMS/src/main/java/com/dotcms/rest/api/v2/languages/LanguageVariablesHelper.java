package com.dotcms.rest.api.v2.languages;

import com.dotcms.languagevariable.business.LanguageVariableExt;
import com.dotcms.languagevariable.business.LanguageVariable;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class to manage Language Variables in the REST API

 */
public class LanguageVariablesHelper {

    private final LanguageVariableAPI languageVariableAPI;

    private final LanguageAPI languageAPI;

    /**
     * Constructor
     * @param languageVariableAPI the LanguageVariableAPI
     * @param languageAPI the LanguageAPI
     */
     LanguageVariablesHelper(LanguageVariableAPI languageVariableAPI, LanguageAPI languageAPI) {
        this.languageVariableAPI = languageVariableAPI;
        this.languageAPI = languageAPI;
    }

    /**
     * Default constructor
     */
     LanguageVariablesHelper () {
       this(APILocator.getLanguageVariableAPI(), APILocator.getLanguageAPI());
    }

    /**
     * View Language Variables
     * @param context the PaginationContext
     * @param renderNulls whether to render nulls
     * @return the LanguageVariablePageView
     * @throws DotDataException if an error occurs
     */
     LanguageVariablePageView view(final PaginationContext context, final boolean renderNulls)
            throws DotDataException {

        final int offset =  context.getPage();
        final int limit = context.getPerPage();
        //LangVarKey-> languageCode -> LanguageVariable.Value
        final LinkedHashMap<String, Map<String,LanguageVariableView>> table = new LinkedHashMap<>();
        final List<Language> allLanguages = languageAPI.getLanguages().stream()
                .collect(Collectors.toUnmodifiableList());
        final int count = languageVariableAPI.countVariablesByKey();
        final Map<String, List<LanguageVariableExt>> variablesGroupedByKey = languageVariableAPI.findVariablesGroupedByKey(offset, limit, null);
        variablesGroupedByKey.forEach((key,variables) -> buildVariablesTable(key, variables, allLanguages, table, renderNulls));
        table.forEach((k,v) -> Logger.debug(this, "Key: " + k ));
        return ImmutableLanguageVariablePageView.builder().variables(table).total(count).build();
    }

    /**
     * Builds the variables table
     * @param key the key
     * @param variables the variables
     * @param languages the languages
     * @param table the table
     * @param renderNulls whether to render nulls
     */
    void buildVariablesTable(final String key, final List<LanguageVariableExt> variables,
            final List<Language> languages,
            final Map<String, Map<String, LanguageVariableView>> table,
            final boolean renderNulls)
    //LangVarKey-> languageCode -> LanguageVariable.Value
    {
        table.compute(key, (k, v) -> {
            //for easy access of LanguageVariable by languageId
            final Map<Long, LanguageVariable> byLangIdMap = variables.stream()
                    .collect(
                            Collectors.toMap(LanguageVariableExt::languageId, Function.identity(), (v1, v2) -> v1));
            //if no value for the key, we create a new map
            if (v == null) {
                v = new LinkedHashMap<>(languages.size());
            }
            //Now let's iterate over all languages and populate the map
            for (final Language current : languages) {
                final LanguageVariable variable = byLangIdMap.get(current.getId());
                if (null != variable) {
                    v.put(current.getIsoCode(),
                            ImmutableLanguageVariableView.builder()
                                    .identifier(variable.identifier())
                                    .value(variable.value())
                                    .build());
                } else {
                    if (renderNulls) {
                        v.put(current.getIsoCode(), null);
                    }
                }
            }
            return v;
        });
    }

}
