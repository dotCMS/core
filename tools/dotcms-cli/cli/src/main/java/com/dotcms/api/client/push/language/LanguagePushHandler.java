package com.dotcms.api.client.push.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.model.language.Language;
import java.io.File;
import java.util.Optional;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

@Dependent
public class LanguagePushHandler implements PushHandler<Language> {

    @Inject
    protected RestClientFactory clientFactory;

    @Override
    public Class<Language> type() {
        return Language.class;
    }

    @Override
    public String title() {
        return "Languages";
    }

    @Override
    public String contentSimpleDisplay(Language language) {
        return String.format(
                "id: [%s] code: [%s]",
                language.id().get(),
                language.isoCode()
        );
    }

    @ActivateRequestContext
    @Override
    public void add(File localFile, Language localLanguage) {

        // Check if the language is missing some required values and trying to set them
        localLanguage = setMissingValues(localLanguage);

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        languageAPI.create(
                Language.builder().from(localLanguage).id(Optional.empty()).build()
        );
    }

    @ActivateRequestContext
    @Override
    public void edit(File localFile, Language localLanguage, Language serverLanguage) {

        // Check if the language is missing some required values and trying to set them
        localLanguage = setMissingValues(localLanguage);

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        languageAPI.update(
                localLanguage.id().map(String::valueOf).orElseThrow(() ->
                        new RuntimeException("Missing language ID")
                ), Language.builder().from(localLanguage).id(Optional.empty()).build()
        );
    }

    @ActivateRequestContext
    @Override
    public void remove(Language serverLanguage) {

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        languageAPI.delete(
                serverLanguage.id().map(String::valueOf).orElseThrow(() ->
                        new RuntimeException("Missing language ID")
                )
        );
    }

    private Language setMissingValues(Language localLanguage) {

        final String isoCode = localLanguage.isoCode();
        if (localLanguage.languageCode().isEmpty()) {
            localLanguage = localLanguage.withLanguageCode(isoCode.split("-")[0]);
        }

        if (localLanguage.countryCode().isEmpty()) {
            if (isoCode.split("-").length > 1) {
                localLanguage = localLanguage.withCountryCode(isoCode.split("-")[1]);
            } else {
                localLanguage = localLanguage.withCountryCode("");
            }
        }

        return localLanguage;
    }

}
