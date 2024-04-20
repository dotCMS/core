package com.dotcms.rest.api.v2.languages;

import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import com.dotmarketing.util.Logger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LanguageVariablesHelper {

    private final LanguageVariableAPI languageVariableAPI;

    private final LanguageAPI languageAPI;


     LanguageVariablesHelper(LanguageVariableAPI languageVariableAPI, LanguageAPI languageAPI) {
        this.languageVariableAPI = languageVariableAPI;
        this.languageAPI = languageAPI;
    }

     LanguageVariablesHelper () {
       this(APILocator.getLanguageVariableAPI(), APILocator.getLanguageAPI());
    }

     LanguageVariablePageView view(final PaginationContext context, final boolean renderNulls)
            throws DotDataException {

        final int offset =  context.getPage();
        final int limit = context.getPerPage();
        final String orderBy = LanguageVariableAPI.ORDER_BY_KEY;
        //LangVarKey-> languageCode -> LanguageVariable.Value
        final LinkedHashMap<String, Map<String,LanguageVariableView>> table = new LinkedHashMap<>();
        final List<Language> allLanguages = languageAPI.getLanguages().stream()
                .collect(Collectors.toUnmodifiableList());
        final int count = languageVariableAPI.countLiveVariables();
        final Map<String, List<LanguageVariable>> variablesByLanguageMap = languageVariableAPI.findVariablesForPagination(offset, limit, orderBy);
        variablesByLanguageMap.forEach((key,variables) -> buildVariablesTable(key, variables, allLanguages, table, renderNulls));
        table.forEach((k,v) -> Logger.debug(this, "Key: " + k ));
        return ImmutableLanguageVariablePageView.builder().variables(table).total(count).build();
    }


    void buildVariablesTable(final String key, final List<LanguageVariable> variables,
            final List<Language> languages,
            final Map<String, Map<String, LanguageVariableView>> table,
            final boolean renderNulls)
    //LangVarKey-> languageCode -> LanguageVariable.Value
    {
        table.compute(key, (k, v) -> {
            //for easy access of LanguageVariable by languageId
            final Map<Long, LanguageVariable> byLangIdMap = variables.stream()
                    .collect(
                            Collectors.toMap(LanguageVariable::getLanguageId, Function.identity()));
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
                                    .inode(variable.getInode())
                                    .identifier(variable.getIdentifier())
                                    .value(variable.getValue())
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
