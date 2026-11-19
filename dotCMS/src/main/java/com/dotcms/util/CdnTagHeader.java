package com.dotcms.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import javax.servlet.http.HttpServletResponse;

/** Writes the cache tag used by CDNs to purge pages and files by resource identifier. */
public final class CdnTagHeader {

    private static final String HEADER_PROPERTY = "cdn.tag.header";
    private static final String HEADER_NAME = "cdn-tag";
    private static final String DISABLED = "NONE";

    private CdnTagHeader() {
    }

    /**
     * Tags a delivered resource with its stable identifier, not its version inode.
     * The header name is always {@value #HEADER_NAME}. Set {@value #HEADER_PROPERTY}
     * to {@value #DISABLED} to suppress it.
     *
     * @param response the response serving the resource
     * @param identifier the resource identifier, or null for a file without one
     */
    public static void set(final HttpServletResponse response, final String identifier) {
        if (!UtilMethods.isSet(identifier)) {
            return;
        }

        if (DISABLED.equalsIgnoreCase(Config.getStringProperty(HEADER_PROPERTY, HEADER_NAME))) {
            return;
        }

        response.setHeader(HEADER_NAME, identifier);
    }
}
