package com.dotcms.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.dotmarketing.util.Logger;

/**
 * This class provide some utility methods to interact with
 * Vanity URLs
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlUtil {

    private VanityUrlUtil() {

    }

    /**
     * Validates if the regular expression is valid
     *
     * @param regex Regular expression string
     * @return true if is a valid pattern, false if not
     */
    public static boolean isValidRegex(final String regex) {
        boolean isValid = false;
        try {
            Pattern.compile(regex);
            isValid = true;
        } catch (PatternSyntaxException e) {
            Logger.error(VanityUrlUtil.class,
                    "Error cause by invalid Pattern syntax. URI:" + regex, e);
        }
        return isValid;
    } // isValidRegex.



} // E:O:F:VanityUrlUtil.