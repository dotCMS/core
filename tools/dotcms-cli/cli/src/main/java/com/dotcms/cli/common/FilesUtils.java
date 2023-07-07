package com.dotcms.cli.common;

import com.dotcms.model.language.Language;

import java.util.List;
import java.util.Set;

/**
 * Utility class for file-related operations.
 */
public class FilesUtils {

    private FilesUtils() {
        //Hide public constructor
    }

    /**
     * Fallbacks to the default language in case of no languages found scanning the assets.
     *
     * @param languages           the list of available languages
     * @param uniqueLiveLanguages the set of unique live languages
     * @throws RuntimeException if no default language is found in the list of languages
     */
    public static void FallbackDefaultLanguage(
            final List<Language> languages, Set<String> uniqueLiveLanguages) {

        // Get the default language from the list of languages
        var defaultLanguage = languages.stream()
                .filter(language -> {
                    if (language.defaultLanguage().isPresent()) {
                        return language.defaultLanguage().get();
                    }

                    return false;
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No default language found"));

        uniqueLiveLanguages.add(defaultLanguage.isoCode());
    }

}
