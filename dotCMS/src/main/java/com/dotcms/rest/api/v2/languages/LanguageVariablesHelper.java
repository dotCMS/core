package com.dotcms.rest.api.v2.languages;

import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.rest.PaginationContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String orderBy = "contentlet.contentlet_as_json->'fields'->'key'->'value'";

        //LangVarKey-> languageCode -> LanguageVariable.Value
        Map<String, Map<String,LanguageVariableView>> table = new HashMap<>();

        final List<Language> languages = languageAPI.getLanguages();
        for (final Language language : languages) {
            final List<LanguageVariable> variables = languageVariableAPI.findLanguageVariables(language.getId(), context.getPage(), context.getPerPage(), orderBy);
            buildVariablesTable(table, language, variables, languages);
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
