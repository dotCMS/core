package com.dotcms.util;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * This class provide some utility methods to interact with
 * Vanity URLs
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlUtil {

    private final static HostAPI hostAPI = APILocator.getHostAPI();

    /**
     * Generate the sanitized cache key
     *
     * @param hostId The current host Identifier
     * @param uri The current URI
     * @param languageId The current languageId
     * @return String with the sanitized key name
     */
    public static String sanitizeKey(final String hostId, String uri, final long languageId) {
        return hostId + "|" + fixURI(uri).replace('/', '|') + "|lang_" + languageId;
    }

    /**
     * Generate the sanitized cache key
     *
     * @param vanityUrl The vanity Url contentlet
     * @return String with the sanitized key name
     */
    public static String sanitizeKey(final Contentlet vanityUrl)
            throws DotDataException, DotRuntimeException, DotSecurityException {
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
    public static String fixURI(String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return uri;
    }
}
