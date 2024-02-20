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

        // Compare by id first.
        var result = findById(localLanguage.id(), serverContents);

        if (result.isEmpty()) {

            // If not found by id, compare by ISO code.
            result = findByISOCode(localLanguage.isoCode(), serverContents);
        }

        return result;
    }

    @ActivateRequestContext
    @Override
    public Optional<Language> localContains(Language serverContent, List<Language> localLanguages) {

        // Compare by id first.
        var result = findById(serverContent.id(), localLanguages);

        if (result.isEmpty()) {

            // If not found by id, compare by ISO code.
            result = findByISOCode(serverContent.isoCode(), localLanguages);
        }

        return result;
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

    /**
     * Finds a Language object in the given list based on the specified id.
     *
     * @param id        the id of the Language object to be found
     * @param languages the list of Language objects to search in
     * @return an Optional containing the found Language object, or an empty Optional if no match is
     * found
     */
    private Optional<Language> findById(Optional<Long> id, List<Language> languages) {

        if (id.isPresent()) {
            for (var language : languages) {
                if (language.id().isPresent() && language.id().get().equals(id.get())) {
                    return Optional.of(language);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a Language object in the given list based on the specified ISO code.
     *
     * @param isoCode   the ISO code of the Language object to be found
     * @param languages the list of Language objects to search in
     * @return an Optional containing the found Language object, or an empty Optional if no match is
     * found
     */
    private Optional<Language> findByISOCode(String isoCode, List<Language> languages) {

        for (var language : languages) {
            if (language.isoCode().equalsIgnoreCase(isoCode)) {
                return Optional.of(language);
            }
        }

        return Optional.empty();
    }

}
