package com.dotcms.util;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.util.Logger;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.liferay.util.StringUtil.GROUP_REPLACEMENT_PREFIX;
import static com.liferay.util.StringUtil.replaceAllGroups;

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

    /**
     * If there is any expression on the cachedVanityUrl to be replaced (anything that starts with $), will be replaced.
     * @param cachedVanityUrl {@link CachedVanityUrl}
     * @param matches {@link String} array
     * @return {@link CachedVanityUrl}
     */
    public static CachedVanityUrl processExpressions (final CachedVanityUrl cachedVanityUrl,
                                                      final String... matches) {

        CachedVanityUrl cachedVanityUrlToReturn = cachedVanityUrl;

        if (null != cachedVanityUrl && !cachedVanityUrl.getVanityUrlId().equals("CACHE_404_VANITY_URL") && null != matches && matches.length > 0 &&
                -1 != cachedVanityUrl.getForwardTo().indexOf(GROUP_REPLACEMENT_PREFIX)) {

            // Replace the expressions on the vanity forward.
            final StringBuilder builder =
                    new StringBuilder(cachedVanityUrl.getForwardTo());
            replaceAllGroups(builder, matches);

            // check if there was any replacement already
            final String newForwardTo = builder.toString();
            if (!cachedVanityUrl.getForwardTo().equals(newForwardTo)) {

                cachedVanityUrlToReturn =
                        new CachedVanityUrl(newForwardTo, cachedVanityUrl);
            }
        }

        return cachedVanityUrlToReturn;
    } // processExpressions

} // E:O:F:VanityUrlUtil.