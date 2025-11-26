package com.dotcms.api.client.push.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.api.client.util.NamingUtils;
import com.dotcms.model.language.Language;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

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
    public String fileName(final Language language) {
        return NamingUtils.languageFileName(language);
    }

    @Override
    public String contentSimpleDisplay(Language language) {

        if (language.id().isPresent()) {
            return String.format(
                    "id: [%s] code: [%s]",
                    language.id().get(),
                    language.isoCode()
            );
        } else {
            return String.format(
                    "code: [%s]",
                    language.isoCode()
            );
        }
    }

    @ActivateRequestContext
    @Override
    public Language add(File localFile, Language localLanguage, Map<String, Object> customOptions) {

        // Check if the language is missing some required values and trying to set them
        localLanguage = setMissingValues(localLanguage, Optional.empty());

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final var response = languageAPI.create(
                Language.builder().from(localLanguage).id(Optional.empty()).build()
        );

        return response.entity();
    }

    @ActivateRequestContext
    @Override
    public Language edit(File localFile, Language localLanguage, Language serverLanguage,
            Map<String, Object> customOptions) {

        // Check if the language is missing some required values and trying to set them
        localLanguage = setMissingValues(localLanguage, Optional.of(serverLanguage));

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final var response = languageAPI.update(
                localLanguage.id().map(String::valueOf).orElseThrow(() ->
                        new RuntimeException("Missing language ID")
                ), Language.builder().from(localLanguage).id(Optional.empty()).build()
        );

        return response.entity();
    }

    @ActivateRequestContext
    @Override
    public void remove(Language serverLanguage, Map<String, Object> customOptions) {

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        languageAPI.delete(
                serverLanguage.id().map(String::valueOf).orElseThrow(() ->
                        new RuntimeException("Missing language ID")
                )
        );
    }

    /**
     * Sets missing values in the given Language object by filling in the values from the
     * matchingServerLanguage object, if they are missing in the localLanguage object.
     *
     * @param localLanguage          The Language object to update with missing values.
     * @param matchingServerLanguage An optional Language object containing the missing values.
     * @return The updated Language object with missing values filled in.
     */
    private Language setMissingValues(Language localLanguage,
            Optional<Language> matchingServerLanguage) {

        if (localLanguage.id().isEmpty() && matchingServerLanguage.isPresent()) {
            localLanguage = localLanguage.withId(matchingServerLanguage.get().id());
        }

        if (localLanguage.defaultLanguage().isEmpty() && matchingServerLanguage.isPresent()) {
            localLanguage = localLanguage.withDefaultLanguage(
                    matchingServerLanguage.get().defaultLanguage());
        }

        if (localLanguage.language().isEmpty() && matchingServerLanguage.isPresent()) {
            localLanguage = localLanguage.withLanguage(matchingServerLanguage.get().language());
        }

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
