package com.dotcms.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AssetsUtils {

    private AssetsUtils() {
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
     * Builds a remote URL based on the given parameters.
     *
     * @param site       the site to build the URL for
     * @param folderPath the folder path to build the URL for
     * @return the remote URL
     */
    public static String BuildRemoteURL(final String site, final String folderPath) {
        return BuildRemoteAssetURL(site, folderPath, null);
    }

    /**
     * Builds a remote asset URL based on the given parameters.
     *
     * @param site       the site to build the URL for
     * @param folderPath the folder path to build the URL for
     * @param assetName  the asset name to build the URL for
     * @return the remote asset URL
     */
    public static String BuildRemoteAssetURL(final String site, final String folderPath, final String assetName) {

        // Determine if the folder path is empty or null
        var emptyFolderPath = folderPath == null
                || folderPath.isEmpty()
                || folderPath.equals("/");

        // Remove firsts and last slash from folder path
        var cleanedFolderPath = folderPath;
        if (!emptyFolderPath) {
            cleanedFolderPath = cleanedFolderPath.replaceAll("^/", "");
            cleanedFolderPath = cleanedFolderPath.replaceAll("/$", "");
        }

        // Build the folder path based on the input parameters
        final String remoteFolderPath;
        if (!emptyFolderPath) {
            remoteFolderPath = String.format("//%s/%s", site, cleanedFolderPath);
        } else {
            remoteFolderPath = String.format("//%s/", site);
        }

        if (assetName != null && !assetName.isEmpty()) {
            return String.format("%s/%s", remoteFolderPath, assetName);
        }

        return remoteFolderPath;
    }

    /**
     * Parses the given path and extracts the site, folder path and file name components.
     *
     * @param remotePathToParse the remote path to parse
     * @return an InternalFolderPath object containing the site and folder path
     */
    public static RemotePathStructure ParseRemotePath(String remotePathToParse) {

        if (remotePathToParse == null || remotePathToParse.isEmpty()) {
            var error = "path cannot be null or empty";
            throw new IllegalArgumentException(error);
        }

        final URI uri;
        try {
            uri = new URI(remotePathToParse);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        final String site = uri.getHost();
        if (null == site) {
            var error = String.format(
                    "Unable to determine site from path: [%s]. Site must start with a valid protocol or simply // ",
                    remotePathToParse);
            throw new IllegalArgumentException(error);
        }

        String path = uri.getPath();
        if (null == path) {
            var error = String.format("Unable to determine path: [%s].", remotePathToParse);
            throw new IllegalArgumentException(error);
        }
        if (path.isEmpty()) {
            path = "/";
        }

        final var isFolder = AssetsUtils.URIIsFolder(uri);

        Path dotCMSPath;
        try {
            dotCMSPath = Paths.get(path);
        } catch (InvalidPathException e) {
            var error = String.format("Invalid folder path [%s] provided", remotePathToParse);
            throw new IllegalArgumentException(error, e);
        }

        // Represents the site and folder path components of the parsed path.
        return new RemotePathStructure(site, dotCMSPath, isFolder);
    }

    /**
     * Represents the site, folder path and file name components of the parsed remote path.
     */
    public static class RemotePathStructure {

        /**
         * The site component of the parsed path.
         */
        private final String site;

        /**
         * The folder path component of the parsed path.
         */
        private final Path folderPath;

        /**
         * The file name component of the parsed path.
         */
        private final String fileName;

        /**
         * Constructs an RemotePathStructure object with the given site and folder path.
         *
         * @param site the site component
         * @param path the folder path component
         */
        public RemotePathStructure(String site, Path path, boolean isFolder) {

            this.site = site;

            if (isFolder) {
                this.folderPath = path;
                this.fileName = null;
            } else {
                this.folderPath = path.getParent();
                this.fileName = path.getFileName().toString();
            }
        }

        public String site() {
            return site;
        }

        public Path folderPath() {
            return folderPath;
        }

        public String fileName() {
            return fileName;
        }

        public String folderName() {

            int nameCount = folderPath.getNameCount();

            String folderName = "/";

            if (nameCount > 1) {
                folderName = folderPath.subpath(nameCount - 1, nameCount).toString();
            } else if (nameCount == 1) {
                folderName = folderPath.subpath(0, nameCount).toString();
            }

            return folderName;
        }

    }

}
