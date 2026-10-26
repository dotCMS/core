package com.dotcms.security.multipart;

import com.dotmarketing.util.Config;
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
 * @author jsanca
 */
public class IllegalFileExtensionsValidator implements SecureFileValidator {

    private final Set<String> illegalExtensionLowerSet =
            new HashSet<>(Arrays.asList(
                    Config.getCustomArrayProperty("MULTI_PART_ILLEGAL_FILE_EXTENSIONS",
                            String::toLowerCase, String.class,
                            ()-> new String [] {".bat",".exe",".sh",".bin",".jsp",".jspx",".jspf",".swf"}))
            );

    /**
     * Checks the extension of a file name against the block list.
     *
     * @param fileName the file name as received
     * @throws IllegalArgumentException if the name is missing or its extension is blocked
     */
    @Override
    public void validate(final String fileName) {

        if (null == fileName) {

            SecurityLogger.logInfo(this.getClass(), "The filename: null is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }

        final String fileExtension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        if (this.illegalExtensionLowerSet.contains('.' + fileExtension)) {

            SecurityLogger.logInfo(this.getClass(), "The filename: '" + fileName + "' is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }
    }
}
