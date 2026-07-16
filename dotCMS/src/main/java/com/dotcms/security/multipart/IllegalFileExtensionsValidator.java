package com.dotcms.security.multipart;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Has an invalid set of extension
 * @author jsanca
 */
public class IllegalFileExtensionsValidator implements SecureFileValidator {

    private final Set<String> illegalExtensionLowerSet =
            new HashSet<>(Arrays.asList(
                    Config.getCustomArrayProperty("MULTI_PART_ILLEGAL_FILE_EXTENSIONS",
                            String::toLowerCase, String.class,
                            ()-> new String [] {".bat",".exe",".sh",".bin",".jsp",".swf"}))
            );

    @Override
    public void validate(final String fileName) {

        if (null == fileName) {

            SecurityLogger.logInfo(this.getClass(), "The filename: null is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }

        final  String fileExtension = FilenameUtils.getExtension(fileName);
        if (this.illegalExtensionLowerSet.contains('.'+fileExtension)) {

            SecurityLogger.logInfo(this.getClass(), "The filename: '" + fileName + "' is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }
    }
}
