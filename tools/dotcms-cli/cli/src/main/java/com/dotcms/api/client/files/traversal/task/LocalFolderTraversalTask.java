package com.dotcms.api.client.files.traversal.task;

import static com.dotcms.common.AssetsUtils.parseLocalPath;
import static com.dotcms.common.AssetsUtils.statusToBoolean;
import static com.dotcms.model.asset.BasicMetadataFields.PATH_META_KEY;
import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;

import com.dotcms.api.client.files.traversal.TraverseParams;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.HiddenFileFilter;
import com.dotcms.common.LocalPathStructure;
import com.dotcms.model.asset.AbstractAssetSync.PushType;
import com.dotcms.model.asset.AssetSync;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderSync;
import com.dotcms.model.asset.FolderSync.Builder;
import com.dotcms.model.asset.FolderView;
import com.dotcms.security.Utils;
import com.google.common.base.Strings;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RecursiveTask;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

/**
 * Recursive task for traversing a file system directory and building a hierarchical tree
 * representation of its contents. The folders and contents are compared to the remote server in order to determine
 * if there are any differences between the local and remote file system.
 * This task is used to split the traversal into smaller sub-tasks
 * that can be executed in parallel, allowing for faster traversal of large directory structures.
 */
public class LocalFolderTraversalTask extends RecursiveTask<Pair<List<Exception>, TreeNode>> {

    private final TraverseParams params;

    private final Logger logger;

    /**
     * Constructs a new LocalFolderTraversalTask instance.
     * @param params the traverse parameters
     */
    public LocalFolderTraversalTask(final TraverseParams params) {
        this.params = params;
        this.logger = params.logger();
    }

    /**
     * Executes the folder traversal task and returns a TreeNode representing the directory tree
     * rooted at the folder specified in the constructor.
     *
     * @return A TreeNode representing the directory tree rooted at the folder specified in the
     * constructor.
     */
    @Override
    protected Pair<List<Exception>, TreeNode> compute() {

        var errors = new ArrayList<Exception>();

        File folderOrFile = new File(params.sourcePath());

        TreeNode currentNode = null;
        try {
            final var localPathStructure = parseLocalPath(params.workspace(), folderOrFile);
            currentNode = gatherSyncInformation(params.workspace(), folderOrFile, localPathStructure);
        } catch (Exception e) {
            if (params.failFast()) {
                throw e;
            } else {
                errors.add(e);
            }
        }

        if ( null != currentNode && folderOrFile.isDirectory() ) {

            File[] files = folderOrFile.listFiles(new HiddenFileFilter());

            if (files != null) {

                List<LocalFolderTraversalTask> forks = new ArrayList<>();

                for (File file : files) {

                    if (file.isDirectory()) {
                        LocalFolderTraversalTask subTask = new LocalFolderTraversalTask(
                                TraverseParams.builder()
                                        .from(params)
                                        .sourcePath(file.getAbsolutePath())
                                        .build()
                        );
                        forks.add(subTask);
                        subTask.fork();
                    }
                }

                for (LocalFolderTraversalTask task : forks) {
                    var taskResult = task.join();
                    errors.addAll(taskResult.getLeft());
                    currentNode.addChild(taskResult.getRight());
                }
            }
        }

        return Pair.of(errors, currentNode);
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
                                           LocalPathStructure localPathStructure) {

        var live = statusToBoolean(localPathStructure.status());
        var lang = localPathStructure.language();

        if (folderOrFile.isDirectory()) {

            var folder = folderViewFromFile(localPathStructure);

            var assetVersions = AssetVersionsView.builder();

            File[] files = folderOrFile.listFiles(new HiddenFileFilter());

            try {
                // First, retrieving the folder from the remote server
                final FolderView remoteFolder = retrieveFolder(localPathStructure);

                // ---
                // Checking if we need to push this folder
                checkFolderToPush(folder, remoteFolder, files);

                // ---
                // Checking if we need to remove folders
                checkFoldersToRemove(live, lang, folder, files, remoteFolder);

                // ---
                // Checking if we need to remove files
                checkAssetsToRemove(live, lang, assetVersions, files, remoteFolder);

                // ---
                // Checking if we need to push files
                checkAssetsToPush(workspaceFile, live, lang, assetVersions, files, remoteFolder);
            } catch (Exception e) {
                var message = String.format("Error processing folder [%s]", folderOrFile.getAbsolutePath());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

            folder.assets(assetVersions.build());
            var parentFolderView = folder.build();

            var treeNode = new TreeNode(parentFolderView);
            if (parentFolderView.subFolders() != null) {
                for (var subFolder : parentFolderView.subFolders()) {
                    treeNode.addChild(new TreeNode(subFolder));
                }
            }

            return treeNode;

        } else {

            final var parentLocalPathStructure = parseLocalPath(workspaceFile, folderOrFile.getParentFile());
            var folder = folderViewFromFile(parentLocalPathStructure);
            var assetVersions = AssetVersionsView.builder();

            try {
                // First, retrieving the asset from the remote server
                AssetVersionsView remoteAsset = retrieveAsset(localPathStructure);

                // Checking if we need to push the file
                var pushInfo = shouldPushFile(live, lang, folderOrFile, remoteAsset);
                if (pushInfo.push()) {

                    logger.debug(
                            String.format("Marking file [%s] - live [%b] - lang [%s] for push " +
                                            "- New [%b] - Modified [%b].",
                                    localPathStructure.filePath(), live, lang, pushInfo.isNew(),
                                    pushInfo.isModified()));

                    var asset = assetViewFromFile(localPathStructure);
                    final AssetSync syncData = AssetSync.builder().
                            markedForPush(true).
                            pushType(pushInfo.pushType()).
                            build();
                    asset.sync(syncData);
                    assetVersions.addVersions(
                            asset.build()
                    );
                } else {
                    logger.debug(String.format("File [%s] - live [%b] - lang [%s] - already exist in the server.",
                            localPathStructure.filePath(), live, lang));
                }
            } catch (Exception e) {
                var message = String.format("Error processing file [%s]", folderOrFile.getAbsolutePath());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

            folder.assets(assetVersions.build());
            return new TreeNode(folder.build());
        }

    }

    /**
     * Checks if files need to be pushed to the remote server.
     *
     * @param workspaceFile                        the workspace file
     * @param live                                 the live status
     * @param lang                                 the language
     * @param assetVersionsBuilder the parent folder asset versions view builder
     * @param folderChildren                       the files to check
     * @param remoteFolder                         the remote folder
     */
    private void checkAssetsToPush(File workspaceFile, boolean live, String lang,
                                   AssetVersionsView.Builder assetVersionsBuilder,
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

                        final AssetSync syncData = AssetSync.builder()
                                .markedForPush(true)
                                .pushType(pushInfo.pushType())
                                .build();

                        assetVersionsBuilder.addVersions(
                                assetViewFromFile(workspaceFile, file).
                                        sync(syncData).
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
     * @param folder the parent folder view builder
     * @param remoteFolder            the remote folder
     * @param folderFiles             the internal files of the folder
     */
    private void checkFolderToPush(FolderView.Builder folder,
            FolderView remoteFolder,
            File[] folderFiles) {

        if (remoteFolder == null) {
            boolean markForPush = false;
            if (params.ignoreEmptyFolders()) {
                if (folderFiles != null && folderFiles.length > 0) {
                    // Does  not exist on remote server, so we need to push it
                    markForPush = true;
                }
            } else {
                // Does  not exist on remote server, so we need to push it
                markForPush = true;
            }
            folder.sync(FolderSync.builder().markedForPush(markForPush).build());
        }
    }

    /**
     * Checks if files need to be removed from the remote server.
     *
     * @param live                                 the live status
     * @param lang                                 the language
     * @param assetVersions the parent folder asset versions view builder
     * @param folderChildren                       the files to check
     * @param remoteFolder                         the remote folder
     */
    private void checkAssetsToRemove(boolean live, String lang,
            AssetVersionsView.Builder assetVersions,
            File[] folderChildren, FolderView remoteFolder) {

        // The option to remove assets is disabled
        if (!params.removeAssets()) {
            return;
        }

        if (null != remoteFolder && remoteFolder.assets() != null) {
            for (var version : remoteFolder.assets().versions()) {

                // Checking if we need to remove the version on the remote server
                var remove = shouldRemoveAsset(live, lang, version, folderChildren);
                if (remove) {

                    // File exist on remote server, but not locally, so we need to remove it
                    logger.debug(
                            String.format("Marking file [%s] - live [%b] - lang [%s] for delete.",
                                    version.name(), live, lang));

                    final Optional<AssetSync> existingSyncData = version.sync();

                    final AssetSync.Builder builder = AssetSync.builder();
                    existingSyncData.ifPresent(builder::from);
                    builder.markedForDelete(true);

                    final AssetSync syncData = builder.build();
                    var copy = version.withSync(syncData)
                            .withLive(live)
                            .withWorking(!live);
                    assetVersions.addVersions(copy);

                }
            }
        }
    }

    /**
     * Checks if folders need to be removed from the remote server.
     *
     * @param live                    the live status
     * @param lang                    the language
     * @param folder the parent folder view builder
     * @param folderChildren          the files to check
     * @param remoteFolder            the remote folder
     */
    private void checkFoldersToRemove(boolean live, String lang,
            FolderView.Builder folder,
            File[] folderChildren, FolderView remoteFolder) {

        // The option to remove folders is disabled
        if (!params.removeFolders() && !params.removeAssets()) {
            return;
        }

        if (null != remoteFolder && null != remoteFolder.subFolders()) {

            for (var subFolder : remoteFolder.subFolders()) {

                final boolean remove = !findLocalMatch(folderChildren, subFolder);

                if (remove) {

                    // Folder exist on remote server, but not locally, so we need to remove it and also the assets
                    // inside it, this is important because depending on the status (live/working), a delete of a
                    // folder can be an "un-publish" of the assets inside it or a delete of the folder itself, we need
                    // to have all the assets inside the folder to be able to handle all the cases.
                    // TODO: This is not going to work because even if you have all the assets inside the folder locally.
                    //  If we delete the remote folder and it has pages in it, the pages will be lost. We need some sort of merge folder mechanism.
                    var remoteSubFolder = retrieveFolder(subFolder.host(), subFolder.path());
                    if (remoteSubFolder != null) {

                        boolean ignore = false;

                        if (params.ignoreEmptyFolders()) {
                            //This is basically a check for delete
                            ignore = ignoreFolder(live, lang, remoteSubFolder);
                        }

                        if (!ignore) { //This "ignore" flag is basically saying the folder needs to be removed or not

                            //Ok we have determined the remote folder has assets that match the status and language
                            //therefore we can not ignore it

                            var assetVersions = AssetVersionsView.builder();
                            checkAssetsToRemove(live, lang, assetVersions, null, remoteSubFolder);
                            subFolder = subFolder.withAssets(assetVersions.build());

                            // Folder exist on remote server, but not locally, so we need to remove it
                            logger.debug(String.format("Marking folder [%s] for delete.", subFolder.path()));
                            if (params.removeFolders()) {
                                final Optional<FolderSync> existingSyncData = subFolder.sync();
                                final Builder builder = FolderSync.builder();
                                existingSyncData.ifPresent(builder::from);
                                subFolder = subFolder.withSync(builder.markedForDelete(true).build());
                            }
                            folder.addSubFolders(subFolder);
                        }
                    }
                }
            }
        }
    }

    /**
     * explore assets in the remote folder and  figure out if they match status and language
     * if so we can not ignore the remote folder cuz it has assets matching the status and language were in
     * @param live
     * @param lang
     * @param remoteSubFolder
     * @return
     */
    private boolean ignoreFolder(boolean live, String lang, FolderView remoteSubFolder) {
        boolean ignore = true;
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
        return ignore;
    }

    /**
     * Take the remote folder representation and find its equivalent locally
     * @param folderChildren
     * @param remote
     * @return
     */
    private boolean findLocalMatch(File[] folderChildren, FolderView remote) {

        if (null == folderChildren) {
            return false;
        }

        return Arrays.stream(folderChildren).filter(File::isDirectory)
                .filter(customer -> remote.name().equalsIgnoreCase(customer.getName())).count()
                == 1;
    }

    /**
     * Retrieves an asset information from the remote server.
     *
     * @param localPathStructure the local path structure
     * @return The AssetVersionsView representing the retrieved asset data, or null if it doesn't exist
     */
    private AssetVersionsView retrieveAsset(LocalPathStructure localPathStructure) {

        if (!params.siteExists()) {
            // Site doesn't exist on remote server
            // No need to pass a siteExists flag we could NullRetriever when the site doesn't exist
            return null;
        }

        AssetVersionsView remoteAsset = null;

        try {
            remoteAsset = params.retriever().retrieveAssetInformation(
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
    private FolderView retrieveFolder(LocalPathStructure localPathStructure) {
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

        if (!params.siteExists()) {
            // Site doesn't exist on remote server
            return null;
        }

        FolderView remoteFolder = null;

        try {
            remoteFolder = params.retriever().retrieveFolderInformation(site, folderPath);
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
    private FolderView.Builder folderViewFromFile(LocalPathStructure localPathStructure) {

        return FolderView.builder()

                //Keep an eye on these two
                //.localStatus(localPathStructure.status())
                //.localLanguage(localPathStructure.language())

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

        final var localPathStructure = parseLocalPath(workspaceFile, file);
        return assetViewFromFile(localPathStructure);
    }

    /**
     * Builds an AssetView object from a local path structure.
     *
     * @param localPathStructure the local path structure
     * @return The AssetView.Builder representing the asset view
     */
    private AssetView.Builder assetViewFromFile(LocalPathStructure localPathStructure) {

        var metadata = new HashMap<String, Object>();
        metadata.put(PATH_META_KEY.key(), localPathStructure.folderPath());

        var live = statusToBoolean(localPathStructure.status());

        return AssetView.builder().
                name(localPathStructure.fileName()).
                live(live).
                working(!live).
                lang(localPathStructure.language()).
                sortOrder(0).
                metadata(metadata);
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

        PushType pushType() {
            PushType pushType = isNew() ? PushType.NEW : PushType.UNKNOWN;
            if(pushType == PushType.UNKNOWN){
                pushType = isModified() ? PushType.MODIFIED : PushType.UNKNOWN;
            }
            return pushType;
        }

    }

}