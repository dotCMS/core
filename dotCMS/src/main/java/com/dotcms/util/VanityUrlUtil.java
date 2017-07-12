package com.dotcms.util;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class provide some utility methods to interact with
 * Vanity URLs
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlUtil {

    private static final HostAPI hostAPI = APILocator.getHostAPI();

    private VanityUrlUtil() {

    }

    /**
     * Generate the sanitized cache key
     *
     * @param hostId The current host Identifier
     * @param uri The current URI
     * @param languageId The current languageId
     * @return String with the sanitized key name
     */
    public static String sanitizeKey(final String hostId, final String uri, final long languageId) {
        return hostId + "|" + fixURI(uri).replace('/', '|') + "|lang_" + languageId;
    }

    /**
     * Generate the sanitized cache key
     *
     * @param vanityUrl The vanity Url contentlet
     * @return String with the sanitized key name
     */
    public static String sanitizeKey(final Contentlet vanityUrl)
            throws DotDataException, DotSecurityException {
        Host host = hostAPI.find(vanityUrl.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                APILocator.systemUser(), false);
        return sanitizeKey(host.getIdentifier(),
                fixURI(vanityUrl.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)),
                vanityUrl.getLanguageId());
    }

    /**
     * Fix the URI passed if the URI doesn't beging with a "/"
     *
     * @param uri The URI to fix
     * @return The fixed uri
     */
    public static String fixURI(final String uri) {
        String modifiedUri = "";
        if (!uri.startsWith("/")) {
            modifiedUri = "/" + uri;
        } else {
            modifiedUri = uri;
        }
        return modifiedUri;
    }

    /**
     * Generate the sanitized cache key for the second cache
     *
     * @param hostId The current host Identifier
     * @param languageId The current languageId
     * @return String with the sanitized key name
     */
    public static String sanitizeSecondCacheKey(final String hostId, final long languageId) {
        return hostId + "| lang_" + languageId;
    }

    /**
     * Generate the sanitized cache key
     *
     * @param vanityUrl The vanity Url contentlet
     * @return String with the sanitized key name
     */
    public static String sanitizeSecondCachedKey(final Contentlet vanityUrl)
            throws DotDataException, DotSecurityException {
        Host host = hostAPI.find(vanityUrl.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                APILocator.systemUser(), false);
        return sanitizeSecondCacheKey(host.getIdentifier(), vanityUrl.getLanguageId());
    }

    /**
     * Validates if the regular expression is valid
     *
     * @param regex Regular expression string
     * @return true if is a valid pattern, false if not
     */
    public static boolean isPatternValid(String regex) {
        boolean isValid = false;
        try {
            Pattern.compile(regex);
            isValid = true;
        } catch (PatternSyntaxException e) {
            Logger.error(VanityUrlUtil.class,
                    "Error cause by invalid Pattern syntax. URI:" + regex, e);
        }
        return isValid;
    }
}
