package com.dotcms.rest.api.v2.languages;

import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class LanguageVariablesHelper {

    static final int FIRST_PAGE_INDEX = 1;
    static final int PER_PAGE_DEFAULT = 10;

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

        String orderBy = "c.contentlet_as_json->'fields'->'key'->'value'";
        //LangVarKey-> languageCode -> LanguageVariable.Value
        Map<String, Map<String,LanguageVariableView>> table = new TreeMap<>(Collections.reverseOrder());
        final List<Language> allLanguages = languageAPI.getLanguages().stream()
                .sorted(Comparator.comparing(Language::getLanguage))
                .collect(Collectors.toList());
        final Map<Long, List<LanguageVariable>> variablesByLanguageMap = languageVariableAPI.findVariablesForPagination(offset, limit, orderBy, allLanguages);
        for (final Language language : allLanguages) {
            final long languageId = language.getId();
            Logger.debug(this, "Processing language: " + languageId + " with tag : " + language + " offset: " + offset + " limit: " + limit);
            final List<LanguageVariable> variables = variablesByLanguageMap.get(languageId);
            if(null != variables) {
                variables.forEach(v -> Logger.info(this,   "Lang:: " +languageId+ " LanguageVariable: " + v.getKey() + " , " + v.getValue())  );
                variables.sort(Comparator.comparing(LanguageVariable::getKey));
                buildVariablesTable(table, language, variables, allLanguages);
            }
        }
        return ImmutableLanguageVariablePageView.builder().variables(table).build();
    }


    void buildVariablesTable(Map<String, Map<String, LanguageVariableView>> table, Language current,
            List<LanguageVariable> variables, List<Language> languages) {

        for (LanguageVariable variable : variables) {
            final String key = variable.getKey();
            table.compute(key, (k, v) -> {
                if (v == null) {
                    v = new HashMap<>();
                    //seed the table with all languages
                    for (Language language : languages) {
                        v.put(language.getIsoCode(), null);
                    }
                }
                v.put(current.getIsoCode(),
                        ImmutableLanguageVariableView.builder()
                                .inode(variable.getInode())
                                .identifier(variable.getIdentifier())
                                .key(variable.getKey())
                                .value(variable.getValue())
                                .build());
                return v;
            });
        }

    }



}
