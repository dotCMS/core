package com.dotcms.api.client.push.language;

import com.dotcms.api.client.push.ContentComparator;
import com.dotcms.model.language.Language;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;

@Dependent
public class LanguageComparator implements ContentComparator<Language> {

    @Override
    public Class<Language> type() {
        return Language.class;
    }

    @ActivateRequestContext
    @Override
    public Optional<Language> findMatchingServerContent(Language localLanguage,
            List<Language> serverContents) {

        for (var serverLanguage : serverContents) {
            if (serverLanguage.isoCode().equalsIgnoreCase(localLanguage.isoCode())) {
                return Optional.of(serverLanguage);
            }
        }

        return Optional.empty();
    }

    @ActivateRequestContext
    @Override
    public Optional<Language> localContains(Language serverContent, List<Language> localLanguages) {

        for (var localLanguage : localLanguages) {
            if (localLanguage.isoCode().equalsIgnoreCase(serverContent.isoCode())) {
                return Optional.of(localLanguage);
            }
        }

        return Optional.empty();
    }

    @ActivateRequestContext
    @Override
    public boolean contentEquals(Language localLanguage, Language serverContent) {

        // Validation to make sure the equals method works as expected
        if (localLanguage.defaultLanguage().isEmpty()) {
            localLanguage = localLanguage.withDefaultLanguage(false);
        }

        // Comparing the local and server content in order to determine if we need to update or not the content
        return localLanguage.equals(serverContent);
    }

}
