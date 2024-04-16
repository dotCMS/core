package com.dotcms.api.client.util;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.language.Language;
import com.dotcms.model.site.SiteView;

/**
 * The NamingUtils class provides utility methods for generating file names based on different
 * objects.
 */
public class NamingUtils {

    private NamingUtils() {
        //Hide public constructor
    }

    /**
     * Retrieves the file name for the given Language object.
     *
     * @param language The Language object for which the file name is to be retrieved.
     * @return The file name for the given Language object.
     */
    public static String languageFileName(final Language language) {
        return language.isoCode();
    }

    /**
     * Retrieves the file name for the given ContentType.
     *
     * @param contentType The ContentType object for which the file name is to be retrieved.
     * @return The file name for the given ContentType object.
     */
    public static String contentTypeFileName(final ContentType contentType) {
        return contentType.variable();
    }

    /**
     * Retrieves the file name for the given SiteView object.
     *
     * @param site The SiteView object for which the file name is to be retrieved.
     * @return The file name for the given SiteView object.
     */
    public static String siteFileName(final SiteView site) {
        return site.hostName();
    }

}
