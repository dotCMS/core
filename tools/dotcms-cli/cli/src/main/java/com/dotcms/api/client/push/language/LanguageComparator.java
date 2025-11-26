package com.dotcms.api.client.push.language;

import com.dotcms.api.client.push.ContentComparator;
import com.dotcms.model.language.Language;
import java.io.File;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;

@Dependent
public class LanguageComparator implements ContentComparator<Language> {

    @Override
    public Class<Language> type() {
        return Language.class;
    }

    @ActivateRequestContext
    @Override
    public Optional<Language> findMatchingServerContent(File localFile,
            Language localLanguage, List<Language> serverContents) {

        // Compare by ISO code first.
        var result = findByISOCode(localLanguage.isoCode(), serverContents);

        if (result.isEmpty() && localLanguage.id().isPresent()) {

            // If not found by ISO code, compare by id
            result = findById(localLanguage.id(), serverContents);
        }

        // If nothing was found let's use the file name as a last resort, this is useful because the
        // file name is the ISO code, and someone might have changed the ISO code in the json file,
        // confusing the validation process, processing it as a new language.
        if (result.isEmpty() && !getFileName(localFile).equalsIgnoreCase(localLanguage.isoCode())) {
            // The ISO changed
            result = findByISOCode(getFileName(localFile), serverContents);
        }

        return result;
    }

    @ActivateRequestContext
    @Override
    public boolean existMatchingLocalContent(Language serverContent, List<File> localFiles,
            List<Language> localLanguages) {

        // Compare by ISO code first.
        var result = findByISOCode(serverContent.isoCode(), localLanguages);

        if (result.isEmpty()) {

            // If not found by ISO code, compare by id
            result = findById(serverContent.id(), localLanguages);
        }

        // If nothing was found let's use the file name as a last resort, this is useful because the
        // file name is the ISO code, and someone might have changed the ISO code in the json file,
        // confusing the validation process, processing it as the language does not exist locally
        // but only in the server.
        if (result.isEmpty()) {
            for (var file : localFiles) {
                if (getFileName(file).equalsIgnoreCase(serverContent.isoCode())) {
                    // The ISO changed
                    return true;
                }
            }
        }

        return result.isPresent();
    }

    @ActivateRequestContext
    @Override
    public boolean contentEquals(Language localLanguage, Language serverContent) {

        // Comparing the local and server content in order to determine if we need to update or
        // not the content
        return equals(localLanguage, serverContent);
    }

    /**
     * Checks if two Language objects are equal based on their language name and ISO code.
     *
     * @param toCompare the Language object to compare
     * @param another   the Language object to compare against
     * @return true if the two Language objects are equal, false otherwise
     */
    private boolean equals(@Nullable Language toCompare, @Nullable Language another) {

        if (toCompare == another) {
            return true;
        }

        if (toCompare == null || another == null) {
            return false;
        }

        return toCompare.isoCode().equalsIgnoreCase(another.isoCode());
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
