package com.dotcms.rest.api.v2.languages;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class LanguageView extends Language {

    private final boolean defaultLanguage;

    public LanguageView(final Language language){
        super(language.getId(),
                language.getLanguageCode(),
                UtilMethods.isSet(language.getCountryCode()) ? language.getCountryCode() : "",
                UtilMethods.isSet(language.getLanguage()) ? language.getLanguage() : "",
                UtilMethods.isSet(language.getCountry()) ? language.getCountry() : "");
        this.defaultLanguage = isDefault(language);
    }

    public boolean isDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Checks if the language passed is the default language of the system
     * @param language language to check if is default
     * @return boolean true is the language is the default one
     */
    private boolean isDefault(final Language language){
        return language.getId() == APILocator.getLanguageAPI().getDefaultLanguage().getId();
    }

}
