package com.dotcms.security.multipart;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import io.vavr.Lazy;
import org.apache.commons.io.FilenameUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Refuses file names whose extension is on the configured block list
 * ({@code MULTI_PART_ILLEGAL_FILE_EXTENSIONS}). Extensions are compared without regard to letter
 * case, as file systems and servlet containers treat them, so {@code .JSP} and {@code .jsp} are
 * the same extension. The default list covers the types the servlet container would execute or
 * the operating system would run.
 * @author jsanca
 */
public class IllegalFileExtensionsValidator implements SecureFileValidator {

    private static final String ILLEGAL_EXTENSIONS_PROPERTY = "MULTI_PART_ILLEGAL_FILE_EXTENSIONS";

    private static final String[] DEFAULT_ILLEGAL_EXTENSIONS =
            {".bat", ".exe", ".sh", ".bin", ".jsp", ".jspx", ".jspf", ".swf"};

    /**
     * Read on the first check, not when the validator is built: callers keep a validator in a static
     * field, which is created while the application starts, before every configuration source (the
     * system table among them) can be read.
     */
    private final Lazy<Set<String>> illegalExtensionLowerSet = Lazy.of(IllegalFileExtensionsValidator::loadBlockList);

    /**
     * Reads the block list from the configuration. The value is read with
     * {@link Config#getStringArrayProperty(String, String[])}, which looks in the environment, the
     * system table and the properties files alike, so the list can be set wherever the rest of the
     * configuration is. Without a value the default list applies.
     *
     * @return the configured extensions, lower-cased, or the default list when none is configured
     */
    private static Set<String> loadBlockList() {
        return toBlockList(Config.getStringArrayProperty(ILLEGAL_EXTENSIONS_PROPERTY,null));
    }

    /**
     * Builds the set of blocked extensions from a configured list, ignoring surrounding spaces and
     * letter case in each entry.
     *
     * @param configured the extensions as configured, each with its leading dot (for example
     *                   {@code .jsp}); {@code null} or empty means nothing was configured
     * @return the lower-cased extensions, or the default list when {@code configured} is
     *         {@code null} or empty
     */
    static Set<String> toBlockList(final String[] configured) {
        final String[] source = (null == configured || 0 == configured.length)
                ? DEFAULT_ILLEGAL_EXTENSIONS : configured;
        final Set<String> blocked = new HashSet<>();
        for (final String extension : source) {
            blocked.add(extension.trim().toLowerCase(Locale.ROOT));
        }
        return blocked;
    }

    /**
     * Returns the extension of a file name, ignoring dots and spaces at the end of the name. Some
     * file systems drop those characters when the file is written, so {@code page.jsp.} and
     * {@code page.jsp } end up stored as {@code page.jsp} and have to be judged as that.
     *
     * @param fileName the file name as received
     * @return the extension without its dot, or an empty string when there is none
     */
    private static String extensionOf(final String fileName) {
        int end = fileName.length();
        while (end > 0 && (fileName.charAt(end - 1) == '.' || Character.isWhitespace(fileName.charAt(end - 1)))) {
            end--;
        }
        return FilenameUtils.getExtension(fileName.substring(0, end));
    }

    /**
     * Checks the extension of a file name against the block list.
     *
     * @param fileName the file name as received
     * @throws IllegalArgumentException if the name is missing or its extension is blocked; the
     *         message names the file so a caller that reports per item (a CSV import row, a job
     *         submission) can say which one was refused
     */
    @Override
    public void validate(final String fileName) {

        if (null == fileName) {

            SecurityLogger.logInfo(this.getClass(), "The filename: null is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }

        final String fileExtension = extensionOf(fileName).toLowerCase(Locale.ROOT);
        if (this.illegalExtensionLowerSet.get().contains('.' + fileExtension)) {

            SecurityLogger.logInfo(this.getClass(), "The filename: '" + fileName + "' is invalid");
            throw new IllegalArgumentException(
                    "The file name '" + fileName + "' has an extension that is not allowed");
        }
    }
}
