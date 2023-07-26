package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.security.Utils;
import com.google.common.base.Strings;
import org.jboss.logging.Logger;

import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import static com.dotcms.common.AssetsUtils.ParseLocalPath;
import static com.dotcms.common.AssetsUtils.StatusToBoolean;
import static com.dotcms.model.asset.BasicMetadataFields.PATH_META_KEY;
import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;

/**
 * Recursive task for traversing a file system directory and building a hierarchical tree
 * representation of its contents. The folders and contents are compared to the remote server in order to determine
 * if there are any differences between the local and remote file system.
 * This task is used to split the traversal into smaller sub-tasks
 * that can be executed in parallel, allowing for faster traversal of large directory structures.
 */
public class LocalFolderTraversalTask extends RecursiveTask<TreeNode> {

    private final Retriever retriever;
    private final Logger logger;

    private final boolean siteExists;
    private final String sourcePath;
    private final File workspace;
    private final boolean ignoreEmptyFolders;
    private final boolean removeAssets;
    private final boolean removeFolders;

    /**
     * Constructs a new LocalFolderTraversalTask instance.
     *
     * @param logger        the logger
     * @param retriever     the retriever used for REST calls and other operations.
     * @param siteExists    whether the site exists on the remote server
     * @param sourcePath    the source path to traverse
     * @param workspace     the project workspace
     */
    public LocalFolderTraversalTask(
            final Logger logger,
            final Retriever retriever,
            final boolean siteExists,
            final String sourcePath,
            final File workspace,
            final boolean removeAssets,
            final boolean removeFolders,
            final boolean ignoreEmptyFolders) {

        this.logger = logger;
        this.retriever = retriever;
        this.siteExists = siteExists;
        this.sourcePath = sourcePath;
        this.workspace = workspace;
        this.removeAssets = removeAssets;
        this.removeFolders = removeFolders;
        this.ignoreEmptyFolders = ignoreEmptyFolders;
    }

    /**
     * Executes the folder traversal task and returns a TreeNode representing the directory tree
     * rooted at the folder specified in the constructor.
     *
     * @return A TreeNode representing the directory tree rooted at the folder specified in the
     * constructor.
     */
    @Override
    protected TreeNode compute() {

        File folderOrFile = new File(sourcePath);

        final var localPathStructure = ParseLocalPath(this.workspace, folderOrFile);
        TreeNode currentNode = gatherSyncInformation(this.workspace, folderOrFile, localPathStructure);

        if (folderOrFile.isDirectory()) {

            File[] files = folderOrFile.listFiles(new HiddenFileFilter());

            if (files != null) {

                List<LocalFolderTraversalTask> forks = new ArrayList<>();

                for (File file : files) {

                    if (file.isDirectory()) {

                        LocalFolderTraversalTask subTask = new LocalFolderTraversalTask(
                                this.logger,
                                this.retriever,
                                this.siteExists,
                                file.getAbsolutePath(),
                                this.workspace,
                                this.removeAssets,
                                this.removeFolders,
                                this.ignoreEmptyFolders
                        );
                        forks.add(subTask);
                        subTask.fork();
                    }
                }

                for (LocalFolderTraversalTask task : forks) {
                    TreeNode subNode = task.join();
                    currentNode.addChild(subNode);
                }
            }
        }

        return currentNode;
    }

    /**
     * Gathers synchronization information for a folder or file in the traversal process.
     *
     * @param workspaceFile      the workspace file
     * @param folderOrFile       the folder or file to gather information for
     * @param localPathStructure the local path structure
     * @return The TreeNode containing the synchronization information for the folder or file
     */
    private TreeNode gatherSyncInformation(File workspaceFile, File folderOrFile,
                                           AssetsUtils.LocalPathStructure localPathStructure) {

        var live = StatusToBoolean(localPathStructure.status());
        var lang = localPathStructure.language();

        if (folderOrFile.isDirectory()) {

            var parentFolderViewBuilder = folderViewFromFile(localPathStructure);
            var parentFolderAssetVersionsViewBuilder = AssetVersionsView.builder();

            File[] files = folderOrFile.listFiles(new HiddenFileFilter());

            // First, retrieving the folder from the remote server
            FolderView remoteFolder = retrieveFolder(localPathStructure);

            // ---
            // Checking if we need to push this folder
            checkFolderToPush(parentFolderViewBuilder, remoteFolder, files);

            // ---
            // Checking if we need to remove folders
            checkFoldersToRemove(live, lang, parentFolderViewBuilder, files, remoteFolder);

            // ---
            // Checking if we need to remove files
            checkAssetsToRemove(live, lang, parentFolderAssetVersionsViewBuilder, files, remoteFolder);

            // ---
            // Checking if we need to push files
            checkAssetsToPush(workspaceFile, live, lang, parentFolderAssetVersionsViewBuilder, files, remoteFolder);

            parentFolderViewBuilder.assets(parentFolderAssetVersionsViewBuilder.build());
            var parentFolderView = parentFolderViewBuilder.build();

            var treeNode = new TreeNode(parentFolderView);
            if (parentFolderView.subFolders() != null) {
                for (var subFolder : parentFolderView.subFolders()) {
                    treeNode.addChild(new TreeNode(subFolder));
                }
            }

            return treeNode;

        } else {

            final var parentLocalPathStructure = ParseLocalPath(workspaceFile, folderOrFile.getParentFile());
            var parentFolderViewBuilder = folderViewFromFile(parentLocalPathStructure);
            var parentFolderAssetVersionsViewBuilder = AssetVersionsView.builder();

            // First, retrieving the asset from the remote server
            AssetVersionsView remoteAsset = retrieveAsset(localPathStructure);

            // Checking if we need to push the file
            var pushInfo = shouldPushFile(live, lang, folderOrFile, remoteAsset);
            if (pushInfo.push()) {

                logger.debug(String.format("Marking file [%s] - live [%b] - lang [%s] for push " +
                                "- New [%b] - Modified [%b].",
                        localPathStructure.filePath(), live, lang, pushInfo.isNew(), pushInfo.isModified()));

                var asset = assetViewFromFile(localPathStructure);
                asset.markForPush(true).
                        pushTypeNew(pushInfo.isNew()).
                        pushTypeModified(pushInfo.isModified());

                parentFolderAssetVersionsViewBuilder.addVersions(
                        asset.build()
                );
            } else {
                logger.debug(String.format("File [%s] - live [%b] - lang [%s] - already exist in the server.",
                        localPathStructure.filePath(), live, lang));
            }

            parentFolderViewBuilder.assets(parentFolderAssetVersionsViewBuilder.build());
            return new TreeNode(parentFolderViewBuilder.build());
        }

    }

    /**
     * Checks if files need to be pushed to the remote server.
     *
     * @param workspaceFile                        the workspace file
     * @param live                                 the live status
     * @param lang                                 the language
     * @param parentFolderAssetVersionsViewBuilder the parent folder asset versions view builder
     * @param folderChildren                       the files to check
     * @param remoteFolder                         the remote folder
     */
    private void checkAssetsToPush(File workspaceFile, boolean live, String lang,
                                   AssetVersionsView.Builder parentFolderAssetVersionsViewBuilder,
                                   File[] folderChildren, FolderView remoteFolder) {

        if (folderChildren != null) {
            for (File file : folderChildren) {

                if (file.isFile()) {

                    // Checking if we need to push the file
                    PushInfo pushInfo;
                    if (remoteFolder != null) {
                        pushInfo = shouldPushFile(live, lang, file, remoteFolder.assets());
                    } else {
                        // The folder does not even exist on the remote server, we need to push all files
                        pushInfo = new PushInfo(true, true, false);
                    }

                    if (pushInfo.push()) {

                        logger.debug(String.format("Marking file [%s] - live [%b] - lang [%s] for push " +
                                        "- New [%b] - Modified [%b].",
                                file.toPath(), live, lang, pushInfo.isNew(), pushInfo.isModified()));
                        parentFolderAssetVersionsViewBuilder.addVersions(
                                assetViewFromFile(workspaceFile, file).
                                        markForPush(true).
                                        pushTypeNew(pushInfo.isNew()).
                                        pushTypeModified(pushInfo.isModified()).
                                        build()
                        );
                    } else {
                        logger.debug(String.format("File [%s] - live [%b] - lang [%s] - already exist in the server.",
                                file.toPath(), live, lang));
                    }

                }
            }
        }
    }

    /**
     * Checks if folders need to be pushed to the remote server.
     *
     * @param parentFolderViewBuilder the parent folder view builder
     * @param remoteFolder            the remote folder
     * @param folderFiles             the internal files of the folder
     */
    private void checkFolderToPush(FolderView.Builder parentFolderViewBuilder, FolderView remoteFolder,
                                   File[] folderFiles) {

        if (remoteFolder == null) {

            if (this.ignoreEmptyFolders) {
                if (folderFiles != null && folderFiles.length > 0) {
                    // Does  not exist on remote server, so we need to push it
                    parentFolderViewBuilder.markForPush(true);
                }
            } else {
                // Does  not exist on remote server, so we need to push it
                parentFolderViewBuilder.markForPush(true);
            }
        }
    }

    /**
     * Checks if files need to be removed from the remote server.
     *
     * @param live                                 the live status
     * @param lang                                 the language
     * @param parentFolderAssetVersionsViewBuilder the parent folder asset versions view builder
     * @param folderChildren                       the files to check
     * @param remoteFolder                         the remote folder
     */
    private void checkAssetsToRemove(boolean live, String lang,
                                     AssetVersionsView.Builder parentFolderAssetVersionsViewBuilder,
                                     File[] folderChildren, FolderView remoteFolder) {

        // The option to remove assets is disabled
        if (!this.removeAssets) {
            return;
        }

        if (remoteFolder != null) {
            if (remoteFolder.assets() != null) {
                for (var version : remoteFolder.assets().versions()) {

                    // Checking if we need to remove the version on the remote server
                    var remove = shouldRemoveAsset(live, lang, version, folderChildren);
                    if (remove) {

                        // File exist on remote server, but not locally, so we need to remove it
                        logger.debug(String.format("Marking file [%s] - live [%b] - lang [%s] for delete.",
                                version.name(), live, lang));

                        var copy = version.withMarkForDelete(true);
                        copy = copy.withLive(live);
                        copy = copy.withWorking(!live);
                        parentFolderAssetVersionsViewBuilder.addVersions(
                                copy
                        );
                    }
                }
            }
        }
    }

    /**
     * Checks if folders need to be removed from the remote server.
     *
     * @param live                    the live status
     * @param lang                    the language
     * @param parentFolderViewBuilder the parent folder view builder
     * @param folderChildren          the files to check
     * @param remoteFolder            the remote folder
     */
    private void checkFoldersToRemove(boolean live, String lang, FolderView.Builder parentFolderViewBuilder,
                                      File[] folderChildren, FolderView remoteFolder) {

        // The option to remove folders is disabled
        if (!this.removeFolders && !this.removeAssets) {
            return;
        }

        if (remoteFolder != null) {
            if (remoteFolder.subFolders() != null) {
                for (var subFolder : remoteFolder.subFolders()) {

                    var remove = true;

                    if (folderChildren != null) {
                        for (File file : folderChildren) {

                            if (file.isDirectory()) {
                                if (subFolder.name().equalsIgnoreCase(file.getName())) {
                                    remove = false;// Exist, so we don't need to remove it
                                    break;
                                }
                            }
                        }
                    }

                    if (remove) {

                        // Folder exist on remote server, but not locally, so we need to remove it and also the assets
                        // inside it, this is important because depending on the status (live/working), a delete of a
                        // folder can be an unpublish of the assets inside it or a delete of the folder itself, we need
                        // to have all the assets inside the folder to be able to handle all the cases.
                        var remoteSubFolder = retrieveFolder(subFolder.host(), subFolder.path());
                        if (remoteSubFolder != null) {

                            boolean ignore = false;

                            if (this.ignoreEmptyFolders) {

                                ignore = true;

                                if (remoteSubFolder.assets() != null) {
                                    for (var version : remoteSubFolder.assets().versions()) {

                                        // Make sure we are handling the proper status and language
                                        boolean match = matchByStatusAndLang(live, lang, version);
                                        if (match) {
                                            ignore = false;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (!ignore) {

                                var parentFolderAssetVersionsViewBuilder = AssetVersionsView.builder();
                                checkAssetsToRemove(live, lang, parentFolderAssetVersionsViewBuilder, null, remoteSubFolder);
                                subFolder = subFolder.withAssets(parentFolderAssetVersionsViewBuilder.build());

                                // Folder exist on remote server, but not locally, so we need to remove it
                                logger.debug(String.format("Marking folder [%s] for delete.", subFolder.path()));
                                if (this.removeFolders) {
                                    subFolder = subFolder.withMarkForDelete(true);
                                }
                                parentFolderViewBuilder.addSubFolders(subFolder);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves an asset information from the remote server.
     *
     * @param localPathStructure the local path structure
     * @return The AssetVersionsView representing the retrieved asset data, or null if it doesn't exist
     */
    private AssetVersionsView retrieveAsset(AssetsUtils.LocalPathStructure localPathStructure) {

        if (!this.siteExists) {
            // Site doesn't exist on remote server
            return null;
        }

        AssetVersionsView remoteAsset = null;

        try {
            remoteAsset = this.retriever.retrieveAssetInformation(
                    localPathStructure.site(),
                    localPathStructure.folderPath(),
                    localPathStructure.fileName()
            );
        } catch (NotFoundException e) {
            // File doesn't exist on remote server
            logger.debug(String.format("Local file [%s] in folder [%s] doesn't exist on remote server.",
                    localPathStructure.fileName(), localPathStructure.folderPath()));
        }

        return remoteAsset;
    }

    /**
     * Retrieves a folder information from the remote server.
     *
     * @param localPathStructure the local path structure
     * @return The FolderView representing the retrieved folder data, or null if it doesn't exist
     */
    private FolderView retrieveFolder(AssetsUtils.LocalPathStructure localPathStructure) {
        return retrieveFolder(localPathStructure.site(), localPathStructure.folderPath());
    }

    /**
     * Retrieves a folder information from the remote server.
     *
     * @param site       the remote site
     * @param folderPath the remote folder path
     * @return The FolderView representing the retrieved folder data, or null if it doesn't exist
     */
    private FolderView retrieveFolder(final String site, final String folderPath) {

        if (!this.siteExists) {
            // Site doesn't exist on remote server
            return null;
        }

        FolderView remoteFolder = null;

        try {
            remoteFolder = this.retriever.retrieveFolderInformation(site, folderPath);
        } catch (NotFoundException e) {
            // Folder doesn't exist on remote server
            logger.debug(String.format("Local folder [%s] doesn't exist on remote server.", folderPath));
        }

        return remoteFolder;
    }

    /**
     * Determines if an asset needs to be removed from the remote server.
     *
     * @param live             the live status
     * @param lang             the language
     * @param version          the version of the asset on the remote server
     * @param localFolderFiles the local files
     * @return true if the file needs to be removed, false otherwise
     */
    private boolean shouldRemoveAsset(final boolean live, final String lang,
                                      AssetView version, File[] localFolderFiles) {

        // Make sure we are handling the proper status and language
        boolean match = matchByStatusAndLang(live, lang, version);
        if (!match) {
            return false;
        }

        var remove = true;

        if (localFolderFiles != null) {
            for (File file : localFolderFiles) {

                if (file.isFile()) {

                    if (version.name().equalsIgnoreCase(file.getName())) {
                        remove = false;// Exist, so we don't need to remove it
                        break;
                    }
                }
            }
        }

        return remove;
    }

    /**
     * Determines if a file needs to be pushed to the remote server.
     *
     * @param live        the live status
     * @param lang        the language
     * @param file        the file to check
     * @param remoteAsset the remote asset
     * @return The PushInfo object representing the push information for the file
     */
    private PushInfo shouldPushFile(final boolean live, final String lang,
                                    File file, final AssetVersionsView remoteAsset) {

        if (remoteAsset == null) {
            return new PushInfo(true, true, false);
        }

        // Remote SHA-256
        String remoteFileHash = null;

        // Finding the proper version
        for (var version : remoteAsset.versions()) {

            // Make sure we are handling the proper status and language
            boolean match = matchByStatusAndLang(live, lang, version);
            if (!match) {
                continue;
            }

            if (version.name().equalsIgnoreCase(file.getName())) {
                remoteFileHash = (String) version.metadata().get(SHA256_META_KEY.key());
                break;
            }
        }

        var push = true;
        var isNew = true;
        var modifed = false;

        if (!Strings.isNullOrEmpty(remoteFileHash)) { // We found the file in the remote server

            // Local SHA-256
            final String localFileHash = Utils.Sha256toUnixHash(file.toPath());

            // Verify if we need to push the file
            if (localFileHash.equals(remoteFileHash)) {
                push = false;
            } else {
                isNew = false;
                modifed = true;
            }
        }

        return new PushInfo(push, isNew, modifed);
    }

    /**
     * Validates if the asset version matches the live status and language.
     *
     * @param live    the live status
     * @param lang    the language
     * @param version the asset version to validate
     * @return true if the asset version matches the live status and language, false otherwise
     */
    private boolean matchByStatusAndLang(boolean live, String lang, AssetView version) {

        var match = false;

        if (live) {
            if (version.live() && version.lang().equalsIgnoreCase(lang)) {
                match = true;
            }
        } else {
            if (version.working() && version.lang().equalsIgnoreCase(lang)) {
                match = true;
            }
        }

        return match;
    }

    /**
     * Builds a FolderView object from a local path structure.
     *
     * @param localPathStructure the local path structure
     * @return The FolderView.Builder representing the folder view
     */
    private FolderView.Builder folderViewFromFile(AssetsUtils.LocalPathStructure localPathStructure) {

        return FolderView.builder()
                .localStatus(localPathStructure.status())
                .localLanguage(localPathStructure.language())
                .host(localPathStructure.site())
                .path(localPathStructure.folderPath())
                .name(localPathStructure.folderName());
    }

    /**
     * Builds an AssetView object from a file.
     *
     * @param workspaceFile the workspace file
     * @param file          the file to build the AssetView from
     * @return The AssetView.Builder representing the asset view
     */
    private AssetView.Builder assetViewFromFile(File workspaceFile, File file) {

        final var localPathStructure = ParseLocalPath(workspaceFile, file);
        return assetViewFromFile(localPathStructure);
    }

    /**
     * Builds an AssetView object from a local path structure.
     *
     * @param localPathStructure the local path structure
     * @return The AssetView.Builder representing the asset view
     */
    private AssetView.Builder assetViewFromFile(AssetsUtils.LocalPathStructure localPathStructure) {

        var metadata = new HashMap<String, Object>();
        metadata.put(PATH_META_KEY.key(), localPathStructure.folderPath());

        var live = StatusToBoolean(localPathStructure.status());

        return AssetView.builder().
                name(localPathStructure.fileName()).
                live(live).
                working(!live).
                lang(localPathStructure.language()).
                sortOrder(0).
                metadata(metadata);
    }

    /**
     * FileFilter implementation to allow hidden files and folders and filter out system specific elements.
     */
    private static class HiddenFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return !file.getName().equalsIgnoreCase(".DS_Store");
        }
    }

    /**
     * Represents the push information for a file.
     */
    private static class PushInfo {

        private final boolean push;
        private final boolean isNew;
        private final boolean isModified;

        /**
         * Constructs a new PushInfo instance.
         *
         * @param push       whether the file should be pushed
         * @param isNew      whether the file is new
         * @param isModified whether the file has been modified
         */
        public PushInfo(boolean push, boolean isNew, boolean isModified) {
            this.push = push;
            this.isNew = isNew;
            this.isModified = isModified;
        }

        /**
         * Returns whether the file should be pushed.
         *
         * @return true if the file should be pushed, false otherwise
         */
        public boolean push() {
            return push;
        }

        /**
         * Returns whether the file is new.
         *
         * @return true if the file is new, false otherwise
         */
        public boolean isNew() {
            return isNew;
        }

        /**
         * Returns whether the file has been modified.
         *
         * @return true if the file has been modified, false otherwise
         */
        public boolean isModified() {
            return isModified;
        }
    }

}