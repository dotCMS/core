package com.dotcms.rest.api.v2.languages;

import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LanguageVariablesHelper {

    private final LanguageVariableAPI languageVariableAPI;

    private final LanguageAPI languageAPI;


    public LanguageVariablesHelper(LanguageVariableAPI languageVariableAPI, LanguageAPI languageAPI) {
        this.languageVariableAPI = languageVariableAPI;
        this.languageAPI = languageAPI;
    }

    public LanguageVariablesHelper () {
       this(APILocator.getLanguageVariableAPI(), APILocator.getLanguageAPI());
    }

    LanguageVariablePageView view(final PaginationContext context, final User user)
            throws DotDataException {

        final int offset =  context.getPage();
        final int limit = context.getPerPage();
        final String orderBy = LanguageVariableAPI.ORDER_BY_KEY;
        //LangVarKey-> languageCode -> LanguageVariable.Value
        final LinkedHashMap<String, Map<String,LanguageVariableView>> table = new LinkedHashMap<>();
        final List<Language> allLanguages = languageAPI.getLanguages().stream()
                .sorted(Comparator.comparing(Language::getLanguage))
                .collect(Collectors.toList());
        final int count = languageVariableAPI.countLiveVariables();
        final Map<String, List<LanguageVariable>> variablesByLanguageMap = languageVariableAPI.findVariablesForPagination(offset, limit, orderBy, allLanguages);
        variablesByLanguageMap.forEach((key,variables) -> buildVariablesTable(key, variables, allLanguages, table));
        table.forEach((k,v) -> Logger.debug(this, "Key: " + k ));
        return ImmutableLanguageVariablePageView.builder().variables(table).total(count).build();
    }


    void buildVariablesTable(final String key, final List<LanguageVariable> variables, final List<Language> languages, final Map<String, Map<String, LanguageVariableView>> table) {

            table.compute(key, (k, v) -> {

                final Map<Long, LanguageVariable> byLangIdMap = variables.stream()
                        .collect(Collectors.toMap(LanguageVariable::getLanguageId, Function.identity()));

                if (v == null) {
                    v = new LinkedHashMap<>(languages.size());
                }
                for(Language current:languages){
                    final LanguageVariable variable = byLangIdMap.get(current.getId());
                    if(variable == null){
                        continue;
                    }
                    v.put(current.getIsoCode(),
                            ImmutableLanguageVariableView.builder()
                                    .inode(variable.getInode())
                                    .identifier(variable.getIdentifier())
                                    .key(variable.getKey())
                                    .value(variable.getValue())
                                    .build());
                }
                return v;
            });
    }



}
