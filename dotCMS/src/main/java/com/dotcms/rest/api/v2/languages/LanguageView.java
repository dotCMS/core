package com.dotcms.rest.api.v2.languages;

import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class LanguageView extends Language {

    private final boolean defaultLanguage;

    private LangVarsCount variables;

    public LanguageView(final Language language, BooleanSupplier isDefaultSupplier){
        super(language.getId(),
                language.getLanguageCode(),
                UtilMethods.isSet(language.getCountryCode()) ? language.getCountryCode() : "",
                UtilMethods.isSet(language.getLanguage()) ? language.getLanguage() : "",
                UtilMethods.isSet(language.getCountry()) ? language.getCountry() : "");
        this.defaultLanguage = isDefaultSupplier.getAsBoolean();
    }

    public LanguageView(final Language language, BooleanSupplier isDefaultSupplier ,Supplier<LangVarsCount> langVarsCountSupplier){
        this(language, isDefaultSupplier);
        this.variables = langVarsCountSupplier.get();
    }

    public boolean isDefaultLanguage() {
        return defaultLanguage;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public LangVarsCount getVariables() {
        return variables;
    }

}
