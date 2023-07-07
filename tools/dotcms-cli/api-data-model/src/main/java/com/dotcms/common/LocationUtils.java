package com.dotcms.common;

import java.net.URI;
import java.net.URISyntaxException;

public class LocationUtils {

    public static final String LOCATION_FILES = "files";

    private LocationUtils() {
        //Hide public constructor
    }

    /**
     * Checks if the given URL is a folder.
     *
     * @param url the URL to check
     * @return true if the URL is a folder, false otherwise
     */
    public static boolean URLIsFolder(final String url) {

        final URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        return URIIsFolder(uri);
    }

    /**
     * Checks if the given URI is a folder.
     *
     * @param uri the URL to check
     * @return true if the URI is a folder, false otherwise
     */
    public static boolean URIIsFolder(final URI uri) {

        var path = uri.getPath();

        if (path.endsWith("/")) {
            return true;
        } else {
            int lastSlashIndex = path.lastIndexOf('/');
            int dotIndex = path.lastIndexOf('.');

            return dotIndex <= lastSlashIndex || dotIndex >= uri.toString().length() - 1;
        }
    }

}
