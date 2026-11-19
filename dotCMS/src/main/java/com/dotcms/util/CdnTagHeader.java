package com.dotcms.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;

/** Writes the cache tag used by CDNs to purge pages and files by resource identifier. */
public final class CdnTagHeader {

    private static final String HEADER_PROPERTY = "cdn.tag.header";
    private static final String DEFAULT_HEADER = "cdn-tag";
    private static final String DISABLED = "NONE";
    private static final Pattern HTTP_TOKEN = Pattern.compile("[!#$%&'*+\\-.^_`|~0-9A-Za-z]+");

    private CdnTagHeader() {
    }

    /**
     * Tags a delivered resource with its stable identifier, not its version inode.
     * Use {@value #DISABLED} to suppress the header. A blank or invalid name falls back
     * to {@value #DEFAULT_HEADER}.
     *
     * @param response the response serving the resource
     * @param identifier the resource identifier, or null for a file without one
     */
    public static void set(final HttpServletResponse response, final String identifier) {
        if (!UtilMethods.isSet(identifier)) {
            return;
        }

        final String header = Config.getStringProperty(HEADER_PROPERTY, DEFAULT_HEADER);
        if (DISABLED.equalsIgnoreCase(header)) {
            return;
        }

        response.setHeader(HTTP_TOKEN.matcher(header).matches() ? header : DEFAULT_HEADER, identifier);
    }
}
