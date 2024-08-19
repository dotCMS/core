package com.dotcms.common;

import static com.dotcms.common.LocationUtils.encodePath;
import static com.dotcms.common.WorkspaceManager.resolvePath;
import static com.dotcms.model.config.Workspace.FILES_NAMESPACE;

import com.dotcms.model.asset.AbstractAssetSync.PushType;
import com.dotcms.model.asset.AssetSync;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderSync;
import com.dotcms.model.asset.FolderView;
import com.google.common.base.Strings;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetsUtils {

    private static final String STATUS_LIVE = "live";
    private static final String STATUS_WORKING = "working";

    private static final Logger logger = LoggerFactory.getLogger(AssetsUtils.class);

    private AssetsUtils() {
        //Hide public constructor
    }

    /**
     * Converts a boolean live status to a string representation.
     *
     * @param isLive the status to convert
     * @return the string representation of the status
     */
    public static String statusToString(boolean isLive) {
        return isLive ? STATUS_LIVE : STATUS_WORKING;
    }

    /**
     * Converts a string status to a boolean live representation.
     *
     * @param status the status to convert
     * @return the boolean representation of the status. True if the status is live, false otherwise.
     * @throws IllegalArgumentException if the status is neither "live" nor "working"
     */
    public static boolean statusToBoolean(final String status) {
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
    public static String buildRemoteURL(final String site, final String folderPath) {
        return buildRemoteAssetURL(site, folderPath, null);
    }

    /**
     * Builds a remote asset URL based on the given parameters.
     *
     * @param site       the site to build the URL for
     * @param folderPath the folder path to build the URL for
     * @param assetName  the asset name to build the URL for
     * @return the remote asset URL
     */
    public static String buildRemoteAssetURL(final String site, final String folderPath,
            final String assetName) {

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
        String remoteFolderPath;
        if (!emptyFolderPath) {
            remoteFolderPath = String.format("//%s/%s", site, cleanedFolderPath);
        } else {
            remoteFolderPath = String.format("//%s", site);
        }

        if (assetName != null && !assetName.isEmpty()) {
            remoteFolderPath = String.format("%s/%s", remoteFolderPath, assetName);
        }

        return encodePath(remoteFolderPath);
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
    public static RemotePathStructure parseRemotePath(String remotePathToParse) {

        if (remotePathToParse == null || remotePathToParse.isEmpty()) {
            var error = "path cannot be null or empty";
            throw new IllegalArgumentException(error);
        }

        final URI uri;
        try {
            uri = new URI(encodePath(remotePathToParse));
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

        final var isFolder = LocationUtils.isFolderURI(uri);

        Path dotCMSPath;
        try {
            dotCMSPath = Paths.get(path);
        } catch (InvalidPathException e) {
            var error = String.format("Invalid folder path [%s] provided", remotePathToParse);
            throw new IllegalArgumentException(error, e);
        }

        final AbstractRemotePathStructure.Builder builder = AbstractRemotePathStructure.builder();

        // Represents the site and folder path components of the parsed path.
        if(isFolder){
            return builder
                    .site(site)
                    .folder(dotCMSPath)
                    .build();
        } else {
            return builder
                    .site(site)
                    .asset(dotCMSPath)
                    .build();
        }
    }

    /**
     * Parses the root paths based on the workspace and source files.
     *
     * @param workspace the workspace directory
     * @param source  the source file or directory within the workspace to parse
     * @return a list of root paths
     * @throws IllegalArgumentException if the source path is outside the workspace or does not follow the required
     *                                  structure
     */
    public static List<String> parseRootPaths(final File workspace, final File source) {

        //Call resolve Path to get an absolute path
        var sourcePath = resolvePath(source.toPath());
        var workspacePath = workspace.toPath().toAbsolutePath().normalize();
        var workspaceCount = workspacePath.getNameCount();
        var sourceCount = sourcePath.getNameCount();

        if (sourceCount < workspaceCount) {
            throw new IllegalArgumentException("Source path cannot be outside of the workspace");
        }

        final Path filesPath = Path.of(workspace.getAbsolutePath(), FILES_NAMESPACE)
                .toAbsolutePath().normalize();
        // Check if we are inside the workspace but also inside the files folder
        if (sourceCount > workspaceCount + 1 || (sourceCount == workspaceCount + 1 && !sourcePath.startsWith(filesPath))) {
            logger.warn("Invalid source path provided for a files push {}. Source path must be inside the files folder. otherwise it will fall back to workspace. {}", sourcePath, workspacePath);
            //if a source path is provided, but it is not inside the files folder but still is a valid folder then we will fall back to the workspace
            return parseRootPaths(workspacePath, workspaceCount, workspaceCount);
        }

        return parseRootPaths(sourcePath, workspaceCount, sourceCount);
    }

    /**
     * Parses the root paths based on the workspace and source paths.
     * @param sourcePath the source path
     * @param workspaceCount the workspace path components count
     * @param sourceCount the source path components count
     * @return a list of root paths
     */
    private static List<String> parseRootPaths(final Path sourcePath, final int workspaceCount,
            final int sourceCount) {
        var rootPaths = new ArrayList<String>();

        final File sourcePathFile = sourcePath.toFile();

        if (!sourcePathFile.exists()) {
            return rootPaths;
        }

        Map<Integer, Function<File, List<String>>> levelFunctions = Map.of(
                workspaceCount, AssetsUtils::fromRootFolder,
                workspaceCount + 1, AssetsUtils::fromFilesFolder,
                workspaceCount + 2, AssetsUtils::fromStatusFolder,
                workspaceCount + 3, AssetsUtils::fromLanguageFolder,
                workspaceCount + 4, AssetsUtils::fromSiteFolder
        );

        if (levelFunctions.containsKey(sourceCount)) {
            rootPaths.addAll(levelFunctions.get(sourceCount).apply(sourcePathFile));
        } else {
            rootPaths.add(sourcePathFile.getAbsolutePath());
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
        return fromFilesFolder(new File(source, FILES_NAMESPACE));
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
                rootPaths.addAll(fromSiteFolder(siteFolder));
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
    private static List<String> fromSiteFolder(File siteFolder) {
        return List.of(siteFolder.getAbsolutePath());
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
    public static LocalPathStructure parseLocalPath(File workspace, File source) {

        var sourcePath = source.toPath();
        var workspacePath = workspace.toPath();
        var isDirectory = source.isDirectory();

        var pathStructureBuilder = LocalPathStructure.builder();
        pathStructureBuilder.filePath(sourcePath);
        pathStructureBuilder.isDirectory(source.isDirectory());

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
        if (sourcePart.getFileName().toString().equals(FILES_NAMESPACE)) {

            // Finding the status section
            var statusPart = sourcePath.getName(++workspaceCount);
            if (!Strings.isNullOrEmpty(statusPart.getFileName().toString())) {

                statusToBoolean(statusPart.getFileName().toString());
                pathStructureBuilder.status(statusPart.getFileName().toString());

                // Finding the language section
                var languagePart = sourcePath.getName(++workspaceCount);
                if (!Strings.isNullOrEmpty(languagePart.getFileName().toString())) {

                    pathStructureBuilder.language(languagePart.getFileName().toString());

                    // Finding the site section
                    var site = sourcePath.getName(++workspaceCount);
                    if (!Strings.isNullOrEmpty(site.getFileName().toString())) {

                        pathStructureBuilder.site(site.getFileName().toString());

                        var folderPath = File.separator;
                        // Now calculate the folder path
                        for (var i = workspaceCount + 1; i < sourcePath.getNameCount(); i++) {

                            if (!isDirectory && i == sourcePath.getNameCount() - 1) {
                                pathStructureBuilder.fileName(source.getName());
                                continue;
                            }

                            folderPath = folderPath.
                                    concat(sourcePath.getName(i).getFileName().toString()).
                                    concat(File.separator);
                        }
                        pathStructureBuilder.folderPath(folderPath);
                    }
                }
            }
        }

        return pathStructureBuilder.build();
    }


    /**
     * Checks if the given folder hs any sync metadata indicating that it  must be removed
     * @param folder
     * @return
     */
    public static boolean isMarkedForDelete(FolderView folder) {
        return  folder.sync().map(FolderSync::markedForDelete).orElse(false);
    }

    /**
     * Checks if the given folder hs any sync metadata indicating that it  must be pushed
     * @param folder
     * @return
     */
    public static boolean isMarkedForPush(FolderView folder) {
        return  folder.sync().map(FolderSync::markedForPush).orElse(false);
    }

    /**
     * Checks if the given asset hs any sync metadata indicating that it  must be removed
     * @param asset
     * @return
     */
    public static boolean isMarkedForDelete(AssetView asset) {
        return  asset.sync().map(AssetSync::markedForDelete).orElse(false);
    }

    /**
     * Checks if the given asset hs any sync metadata indicating that it  must be pushed
     * @param asset
     * @return
     */
    public static boolean isMarkedForPush(AssetView asset) {
        return  asset.sync().map(AssetSync::markedForPush).orElse(false);
    }

    /**
     * Checks if the given asset hs any sync metadata indicating that it  must be pushed as existing but modified content
     * @param asset
     * @return
     */
    public static boolean isPushModified(AssetView asset) {
        final Optional<AssetSync> optional = asset.sync();
        if (optional.isPresent()) {
            final AssetSync sync = optional.get();
            return sync.markedForPush() && sync.pushType() == PushType.MODIFIED;
        }
        return false;
    }

    /**
     * Checks if the given asset hs any sync metadata indicating that it  must be pushed as new content
     * @param asset
     * @return
     */
    public static boolean isPushNew(AssetView asset) {
        final Optional<AssetSync> optional = asset.sync();
        if (optional.isPresent()) {
            final AssetSync sync = optional.get();
            return sync.markedForPush() && sync.pushType() == PushType.NEW;
        }
        return false;
    }

}
