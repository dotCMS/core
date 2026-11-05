package com.dotcms.security.multipart;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Refuses file names whose extension is on the configured block list
 * ({@code MULTI_PART_ILLEGAL_FILE_EXTENSIONS}). Extensions are compared without regard to letter
 * case, as file systems and servlet containers treat them, so {@code .JSP} and {@code .jsp} are
 * the same extension. The default list covers the types the servlet container would execute or
 * the operating system would run.
 * <p>
 * The configured list is read on every check, so a value set or removed through the system table
 * applies to the next upload without a restart. The read is a cache hit in the normal case, and
 * the set of extensions is only rebuilt, and logged, when the configured value changes.
 * @author jsanca
 */
public class IllegalFileExtensionsValidator implements SecureFileValidator {

    private static final String ILLEGAL_EXTENSIONS_PROPERTY = "MULTI_PART_ILLEGAL_FILE_EXTENSIONS";

    private static final Set<String> DEFAULT_ILLEGAL_EXTENSIONS =
            Set.of(".bat", ".exe", ".sh", ".bin", ".jsp", ".jspx", ".jspf", ".swf");

    /**
     * The block list last built, kept with the configured value it was built from so a check can
     * tell whether the configuration changed since.
     */
    private static final class BlockList {

        private final String[] configured;
        private final Set<String> extensions;

        private BlockList(final String[] configured, final Set<String> extensions) {
            this.configured = configured;
            this.extensions = extensions;
        }
    }

    private volatile BlockList current;

    /**
     * Returns the block list for the configuration as it is now. The value is read with
     * {@link Config#getStringArrayProperty(String, String[])}, which looks in the environment, the
     * system table and the properties files alike, so the list can be set wherever the rest of the
     * configuration is. The set is rebuilt only when the configured value differs from the one the
     * last set was built from, and each rebuild is logged so a change, or a value that leaves the
     * default list in force, can be seen.
     *
     * @return the blocked extensions, lower-cased and with their leading dot
     */
    private Set<String> blockList() {
        final String[] configured = Config.getStringArrayProperty(ILLEGAL_EXTENSIONS_PROPERTY, null);
        BlockList list = this.current;
        if (null == list || !Arrays.equals(list.configured, configured)) {
            final Set<String> normalized = normalize(configured);
            if (null != configured && normalized.isEmpty()) {
                Logger.warn(this, ILLEGAL_EXTENSIONS_PROPERTY + " is set to "
                        + Arrays.toString(configured) + " but names no extension; the default block list applies: "
                        + DEFAULT_ILLEGAL_EXTENSIONS);
            } else {
                Logger.info(this, "Blocked upload extensions"
                        + (null == configured ? " (default): " : " (configured): ")
                        + (normalized.isEmpty() ? DEFAULT_ILLEGAL_EXTENSIONS : normalized));
            }
            list = new BlockList(configured, normalized.isEmpty() ? DEFAULT_ILLEGAL_EXTENSIONS : normalized);
            this.current = list;
        }
        return list.extensions;
    }

    /**
     * Builds the set of blocked extensions from a configured list. Each entry is read without
     * surrounding spaces and without regard to letter case, and gets its leading dot whether or not
     * it was written with one ({@code jsp}, {@code .jsp} and {@code *.jsp} all mean the same);
     * entries that name no extension are dropped.
     *
     * @param configured the extensions as configured; {@code null} means nothing was configured
     * @return the lower-cased extensions, each with its leading dot, or the default list when
     *         {@code configured} is {@code null} or names no extension at all
     */
    static Set<String> toBlockList(final String[] configured) {
        final Set<String> normalized = normalize(configured);
        return normalized.isEmpty() ? DEFAULT_ILLEGAL_EXTENSIONS : normalized;
    }

    /**
     * Normalizes each configured entry to a lower-cased extension with one leading dot, dropping
     * the entries that name no extension.
     *
     * @param configured the extensions as configured, or {@code null}
     * @return the usable entries, possibly none
     */
    private static Set<String> normalize(final String[] configured) {
        final Set<String> blocked = new HashSet<>();
        if (null != configured) {
            for (final String entry : configured) {
                final String extension = normalize(entry);
                if (!extension.isEmpty()) {
                    blocked.add(extension);
                }
            }
        }
        return blocked;
    }

    /**
     * Normalizes one configured entry: surrounding spaces, letter case and any leading dots or
     * asterisks are ignored, so {@code " .Php "}, {@code "php"} and {@code "*.php"} all give
     * {@code ".php"}.
     *
     * @param entry the entry as configured
     * @return the extension with one leading dot, or an empty string when the entry names none
     */
    private static String normalize(final String entry) {
        if (null == entry) {
            return "";
        }
        final String trimmed = entry.trim().toLowerCase(Locale.ROOT);
        int start = 0;
        while (start < trimmed.length() && (trimmed.charAt(start) == '.' || trimmed.charAt(start) == '*')) {
            start++;
        }
        final String extension = trimmed.substring(start).trim();
        return extension.isEmpty() ? "" : '.' + extension;
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
     * Checks the extension of a file name against the block list as configured at the time of the
     * check.
     *
     * @param fileName the file name as received
     * @throws IllegalArgumentException if the name is missing, carries a null character, or its
     *         extension is blocked; the message names the file so a caller that reports per item
     *         (a CSV import row, a job submission) can say which one was refused
     */
    @Override
    public void validate(final String fileName) {

        if (null == fileName) {

            SecurityLogger.logInfo(this.getClass(), "The filename: null is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }

        // a null character cannot be part of a stored name (the database refuses it, and some
        // libraries cut the name there), so the name is refused up front rather than failing later
        if (fileName.indexOf('\0') >= 0) {

            final String shown = fileName.replace("\0", "\\0");
            SecurityLogger.logInfo(this.getClass(), "The filename: '" + shown + "' is invalid");
            throw new IllegalArgumentException(
                    "The file name '" + shown + "' contains a character that is not allowed");
        }

        final String fileExtension = extensionOf(fileName).toLowerCase(Locale.ROOT);
        if (this.blockList().contains('.' + fileExtension)) {

            SecurityLogger.logInfo(this.getClass(), "The filename: '" + fileName + "' is invalid");
            throw new IllegalArgumentException(
                    "The file name '" + fileName + "' has an extension that is not allowed");
        }
    }
}
