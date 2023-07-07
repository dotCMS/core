package com.dotcms.common;

import com.google.common.base.Strings;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.dotcms.common.LocationUtils.LOCATION_FILES;

public class AssetsUtils {

    private static final String STATUS_LIVE = "live";
    private static final String STATUS_WORKING = "working";

    private AssetsUtils() {
        //Hide public constructor
    }

    /**
     * Converts a boolean live status to a string representation.
     *
     * @param isLive the status to convert
     * @return the string representation of the status
     */
    public static String StatusToString(boolean isLive) {
        return isLive ? STATUS_LIVE : STATUS_WORKING;
    }

    /**
     * Converts a string status to a boolean live representation.
     *
     * @param status the status to convert
     * @return the boolean representation of the status. True if the status is live, false otherwise.
     * @throws IllegalArgumentException if the status is neither "live" nor "working"
     */
    public static boolean StatusToBoolean(final String status) {
        if (status.equalsIgnoreCase(STATUS_LIVE)) {
            return true;
        } else if (status.equalsIgnoreCase(STATUS_WORKING)) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid status: " + status);
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
     * @return an instance of {@link RemotePathStructure} containing the site, folder path, and file name
     * components of the parsed path
     * @throws IllegalArgumentException if the remotePathToParse is null or empty, or if the site component
     *                                  cannot be determined from the path
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

        final var isFolder = LocationUtils.URIIsFolder(uri);

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
     * Parses the root paths based on the workspace and source files.
     *
     * @param workspace the workspace directory
     * @param source    the source file or directory
     * @return a list of root paths
     * @throws IllegalArgumentException if the source path is outside the workspace or does not follow the required
     *                                  structure
     */
    public static List<String> ParseRootPaths(File workspace, File source) {

        var sourcePath = source.toPath();
        var workspacePath = workspace.toPath();

        var workspaceCount = workspacePath.getNameCount();
        var sourceCount = sourcePath.getNameCount();

        if (sourceCount < workspaceCount) {
            throw new IllegalArgumentException("Source path cannot be outside of the workspace");
        }

        // Check if we are inside the workspace but also inside the files folder
        if (sourceCount > workspaceCount + 1) {
            if (!source.getAbsolutePath().startsWith(
                    workspace.getAbsolutePath() + File.separator + LOCATION_FILES + File.separator
            )) {
                throw new IllegalArgumentException("Invalid source path. Source path must be inside the files folder or " +
                        "at the root of the workspace");
            }
        } else if (sourceCount == workspaceCount + 1) {
            if (!source.getName().equals(LOCATION_FILES)) {
                throw new IllegalArgumentException("Invalid source path. Source path must be inside the files folder or " +
                        "at the root of the workspace");
            }
        }

        var rootPaths = new ArrayList<String>();

        if (workspaceCount == sourceCount) {// We are at the root of the workspace
            if (source.exists()) {
                rootPaths.addAll(fromRootFolder(source));
            }
        } else if (workspaceCount + 1 == sourceCount) {// We should be at the files level
            if (source.exists()) {
                rootPaths.addAll(fromFilesFolder(source));
            }
        } else if (workspaceCount + 2 == sourceCount) {// We should be at the status level
            if (source.exists()) {
                rootPaths.addAll(fromStatusFolder(source));
            }
        } else if (workspaceCount + 3 == sourceCount) {// We should be at the language level
            if (source.exists()) {
                rootPaths.addAll(fromLanguageFolder(source));
            }
        } else if (workspaceCount + 4 == sourceCount) {// We should be at the site level
            if (source.exists()) {
                rootPaths.add(fromSiteFolder(source));
            }
        } else {
            rootPaths.add(source.getAbsolutePath());
        }

        return rootPaths;
    }

    /**
     * Retrieves the root paths from the workspace root folder.
     *
     * @param source the source file or directory at the root of the workspace
     * @return a list of root paths
     */
    private static List<String> fromRootFolder(File source) {
        return fromFilesFolder(new File(source, LOCATION_FILES));
    }

    /**
     * Retrieves the root paths from the files folder.
     *
     * @param filesFolder the files folder
     * @return a list of root paths
     */
    private static List<String> fromFilesFolder(File filesFolder) {

        var rootPaths = new ArrayList<String>();

        // From this point we should have two levels of folders, live and working
        File liveFolder = new File(filesFolder, STATUS_LIVE);
        File workingFolder = new File(filesFolder, STATUS_WORKING);

        if (liveFolder.exists()) {
            rootPaths.addAll(fromStatusFolder(liveFolder));
        }

        if (workingFolder.exists()) {
            rootPaths.addAll(fromStatusFolder(workingFolder));
        }

        return rootPaths;
    }

    /**
     * Retrieves the root paths from the status folder.
     *
     * @param statusFolder the status folder
     * @return a list of root paths
     */
    private static List<String> fromStatusFolder(File statusFolder) {

        var rootPaths = new ArrayList<String>();

        // At this level we could have multiple languages
        var languagesFolders = statusFolder.listFiles();
        if (languagesFolders != null) {

            for (var languageFolder : languagesFolders) {

                if (!languageFolder.isDirectory()) {
                    continue;
                }

                rootPaths.addAll(fromLanguageFolder(languageFolder));
            }
        }

        return rootPaths;
    }

    /**
     * Retrieves the root paths from the language folder.
     *
     * @param languageFolder the language folder
     * @return a list of root paths
     */
    private static List<String> fromLanguageFolder(File languageFolder) {

        var rootPaths = new ArrayList<String>();

        // Now, inside each language we could have multiple sites
        var sitesFolders = languageFolder.listFiles();
        if (sitesFolders != null) {

            for (var siteFolder : sitesFolders) {

                if (!siteFolder.isDirectory()) {
                    continue;
                }

                // This is our root path to analyze
                rootPaths.add(fromSiteFolder(siteFolder));
            }
        }

        return rootPaths;
    }

    /**
     * Retrieves the root path from the site folder.
     *
     * @param siteFolder the site folder
     * @return the root path
     */
    private static String fromSiteFolder(File siteFolder) {
        return siteFolder.getAbsolutePath();
    }

    /**
     * Parses the workspace and source paths to extract the structure of a local file or directory path.
     *
     * @param workspace the workspace directory
     * @param source    the source file or directory within the workspace
     * @return an instance of {@link LocalPathStructure} containing the parsed components of the local path
     * @throws IllegalArgumentException if the source path is outside the workspace or does not contain the
     *                                  required structure
     */
    public static LocalPathStructure ParseLocalPath(File workspace, File source) {

        var sourcePath = source.toPath();
        var workspacePath = workspace.toPath();
        var isDirectory = source.isDirectory();

        var pathStructureBuilder = new LocalPathStructure.Builder();
        pathStructureBuilder.withFilePath(sourcePath);
        pathStructureBuilder.withIsDirectory(source.isDirectory());

        var workspaceCount = workspacePath.getNameCount();
        var sourceCount = sourcePath.getNameCount();

        if (sourceCount < workspaceCount) {
            throw new IllegalArgumentException("Source path cannot be outside of the workspace");
        }

        if (workspaceCount + 4 > sourceCount) {
            throw new IllegalArgumentException("Source path does not contain the required structure");
        }

        // Finding the files section
        var sourcePart = sourcePath.getName(workspaceCount);
        if (sourcePart.getFileName().toString().equals(LOCATION_FILES)) {

            // Finding the status section
            var statusPart = sourcePath.getName(++workspaceCount);
            if (!Strings.isNullOrEmpty(statusPart.getFileName().toString())) {

                StatusToBoolean(statusPart.getFileName().toString());
                pathStructureBuilder.withStatus(statusPart.getFileName().toString());

                // Finding the language section
                var languagePart = sourcePath.getName(++workspaceCount);
                if (!Strings.isNullOrEmpty(languagePart.getFileName().toString())) {

                    pathStructureBuilder.withLanguage(languagePart.getFileName().toString());

                    // Finding the site section
                    var site = sourcePath.getName(++workspaceCount);
                    if (!Strings.isNullOrEmpty(site.getFileName().toString())) {

                        pathStructureBuilder.withSite(site.getFileName().toString());

                        var folderPath = File.separator;
                        // Now calculate the folder path
                        for (var i = workspaceCount + 1; i < sourcePath.getNameCount(); i++) {

                            if (!isDirectory && i == sourcePath.getNameCount() - 1) {
                                pathStructureBuilder.withFileName(source.getName());
                                continue;
                            }

                            folderPath = folderPath.
                                    concat(sourcePath.getName(i).getFileName().toString()).
                                    concat(File.separator);
                        }
                        pathStructureBuilder.withFolderPath(folderPath);
                    }
                }
            }
        }

        return pathStructureBuilder.build();
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

    /**
     * Represents the structure of a local file or directory path within the workspace.
     */
    public static class LocalPathStructure {

        private boolean isDirectory;
        private String status;
        private String language;
        private String site;
        private String fileName;
        private String folderPath;
        private Path filePath;

        private LocalPathStructure(Builder builder) {
            this.isDirectory = builder.isDirectory;
            this.status = builder.status;
            this.language = builder.language;
            this.site = builder.site;
            this.fileName = builder.fileName;
            this.folderPath = builder.folderPath;
            this.filePath = builder.filePath;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public String status() {
            return status;
        }

        public String language() {
            return language;
        }

        public String site() {
            return site;
        }

        public String fileName() {
            return fileName;
        }

        public String folderPath() {
            return folderPath;
        }

        public Path filePath() {
            return filePath;
        }

        public String folderName() {

            int nameCount = filePath().getNameCount();

            String folderName = File.separator;

            if (nameCount > 1) {
                folderName = filePath().subpath(nameCount - 1, nameCount).toString();
            } else if (nameCount == 1) {
                folderName = filePath().subpath(0, nameCount).toString();
            }

            if (folderName.equalsIgnoreCase(this.site())) {
                folderName = File.separator;
            }

            return folderName;
        }

        @Override
        public String toString() {
            return "LocalPathStructure{" +
                    "status='" + status + '\'' +
                    ", language='" + language + '\'' +
                    ", site='" + site + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", folderPath='" + folderPath + '\'' +
                    ", filePath='" + filePath + '\'' +
                    ", isDirectory='" + isDirectory + '\'' +
                    '}';
        }

        public static class Builder {
            private boolean isDirectory;
            private String status;
            private String language;
            private String site;
            private String fileName;
            private String folderPath;
            private Path filePath;

            public Builder() {
            }

            public Builder withIsDirectory(boolean isDirectory) {
                this.isDirectory = isDirectory;
                return this;
            }

            public Builder withStatus(String status) {
                this.status = status;
                return this;
            }

            public Builder withLanguage(String language) {
                this.language = language;
                return this;
            }

            public Builder withSite(String site) {
                this.site = site;
                return this;
            }

            public Builder withFileName(String fileName) {
                this.fileName = fileName;
                return this;
            }

            public Builder withFolderPath(String folderPath) {
                this.folderPath = folderPath;
                return this;
            }

            public Builder withFilePath(Path filePath) {
                this.filePath = filePath;
                return this;
            }

            public LocalPathStructure build() {
                return new LocalPathStructure(this);
            }
        }
    }

}
