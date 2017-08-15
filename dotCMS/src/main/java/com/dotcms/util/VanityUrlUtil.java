package com.dotcms.util;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.util.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.dotcms.util.CollectionsUtils.map;
import static com.liferay.util.StringUtil.replaceAll;
import static java.util.stream.IntStream.rangeClosed;

/**
 * This class provide some utility methods to interact with
 * Vanity URLs
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlUtil {


    public static final char EXPRESSION_REPLACEMENT_PREFIX = '$';

    private static final Map<Integer, String []> expressionReplacementCacheMap =
                            new ConcurrentHashMap<>(map ( 1,  new String[] { EXPRESSION_REPLACEMENT_PREFIX+"1" } ,

                                    2, new String[] { EXPRESSION_REPLACEMENT_PREFIX+"1", EXPRESSION_REPLACEMENT_PREFIX+"2" },

                                    3, new String[] { EXPRESSION_REPLACEMENT_PREFIX+"1", EXPRESSION_REPLACEMENT_PREFIX+"2",
                                            EXPRESSION_REPLACEMENT_PREFIX+"3" },

                                    4, new String[] { EXPRESSION_REPLACEMENT_PREFIX+"1", EXPRESSION_REPLACEMENT_PREFIX+"2",
                                            EXPRESSION_REPLACEMENT_PREFIX+"3", EXPRESSION_REPLACEMENT_PREFIX+"4" },

                                    5, new String[] { EXPRESSION_REPLACEMENT_PREFIX+"1", EXPRESSION_REPLACEMENT_PREFIX+"2",
                                            EXPRESSION_REPLACEMENT_PREFIX+"3", EXPRESSION_REPLACEMENT_PREFIX+"4",
                                            EXPRESSION_REPLACEMENT_PREFIX+"5" }));


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
                                                      final String [] matches) {

        CachedVanityUrl cachedVanityUrlToReturn = cachedVanityUrl;

        if (null != cachedVanityUrl && null != matches && matches.length > 0 &&
                -1 != cachedVanityUrl.getForwardTo().indexOf(EXPRESSION_REPLACEMENT_PREFIX)) {

            // Replace the expressions on the vanity forward.
            final StringBuilder builder =
                    new StringBuilder(cachedVanityUrl.getForwardTo());
            replaceAll(builder, getReplacementArray(matches.length), matches);

            // check if there was any replacement already
            final String newForwardTo = builder.toString();
            if (!cachedVanityUrl.getForwardTo().equals(newForwardTo)) {

                cachedVanityUrlToReturn =
                        new CachedVanityUrl(newForwardTo, cachedVanityUrl);
            }
        }

        return cachedVanityUrlToReturn;
    } // processExpressions

    /**
     * Get the array with the list of string to replace, usually a sequence of $1, $2, $3, etc...
     * @param matchesLength {@link Integer}
     * @return String array
     */
    private static String [] getReplacementArray (final int matchesLength) {

        if (!expressionReplacementCacheMap.containsKey(matchesLength)) {

            expressionReplacementCacheMap.put(matchesLength, buildReplacementArray (matchesLength));
        }

        return expressionReplacementCacheMap.get(matchesLength);
    } // getReplacementArray.

    /**
     * Builds a replacement array
     * @param matchesLength {@link Integer}
     * @return String array
     */
    private static String [] buildReplacementArray (final int matchesLength) {

        final String [] replacementArray = new String [matchesLength];

        rangeClosed(1, matchesLength).forEach(i ->
                replacementArray [i - 1] =  EXPRESSION_REPLACEMENT_PREFIX + String.valueOf(i) );

        return replacementArray;
    } // buildReplaceArray.
} // E:O:F:VanityUrlUtil.