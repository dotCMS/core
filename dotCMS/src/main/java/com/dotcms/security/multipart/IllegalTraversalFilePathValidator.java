package com.dotcms.security.multipart;

import com.dotmarketing.util.SecurityLogger;

/**
 * Validates any traversal directory on the file name
 * @author jsanca
 */
public class IllegalTraversalFilePathValidator implements SecureFileValidator {


    @Override
    public void validate(final String fileName) {

        if (null == fileName || fileName.indexOf("/") != -1 || fileName.indexOf("\\") != -1 || fileName.indexOf("..") != -1) {

            SecurityLogger.logInfo(this.getClass(), "The filename: '" + fileName + "' is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }
    }
}
