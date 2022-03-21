package com.dotcms.security.multipart;

/**
 * This interface applies a Secure check over a file name
 * @author jsanca
 */
public interface SecureFileValidator {

    /**
     * Most throw an exception when not valid
     * @param fileName String
     */
    void validate (String fileName);
}
