package com.dotcms.common;

import com.dotcms.model.config.Workspace;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocationUtils {

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

    /**
     * Creates the local path for the given asset data.
     *
     * @param workspace  the workspace path
     * @param status     the status
     * @param language   the language
     * @param siteName   the site name
     * @param folderPath the folder path
     * @param assetName  the asset name
     * @return the local path
     */
    public static Path LocalPathFromAssetData(final String workspace, final String status, final String language,
                                              final String siteName, String folderPath, final String assetName) {

        return Paths.get(workspace, Workspace.FILES_NAMESPACE, status.toLowerCase(), language.toLowerCase(),
                siteName, folderPath, assetName);
    }

}
