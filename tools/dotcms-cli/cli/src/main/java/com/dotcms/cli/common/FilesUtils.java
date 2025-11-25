package com.dotcms.cli.common;

import com.dotcms.model.language.Language;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
    public static void fallbackDefaultLanguage(
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

    static final int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
    static {
        Arrays.sort(illegalChars);
    }

    /**
     * @see <a href=https://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars>remove filename special chars</a>
     * Cleans the specified file name by removing any illegal characters.
     * @param badFileName the file name to clean
     * @return the cleaned file name
     */
    public static String cleanFileName(final String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < badFileName.length(); i++) {
            int c = badFileName.charAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append((char)c);
            }
        }
        return cleanName.toString();
    }

    /**
     * Checks if the specified directory is not empty.
     * @param path the directory to check
     * @return true if the directory is not empty, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean isDirectoryNotEmpty(Path path) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            return directoryStream.iterator().hasNext();
        }
    }

}
