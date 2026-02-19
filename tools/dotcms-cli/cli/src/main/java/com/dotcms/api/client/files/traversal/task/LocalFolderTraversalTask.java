package com.dotcms.api.client.files.traversal.task;

import static com.dotcms.common.AssetsUtils.parseLocalPath;
import static com.dotcms.common.AssetsUtils.statusToBoolean;
import static com.dotcms.model.asset.BasicMetadataFields.PATH_META_KEY;
import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;

import com.dotcms.api.client.FileHashCalculatorService;
import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.DotCliIgnoreFileFilter;
import com.dotcms.cli.common.HiddenFileFilter;
import com.dotcms.common.LocalPathStructure;
import com.dotcms.model.asset.AbstractAssetSync.PushType;
import com.dotcms.model.asset.AssetSync;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderSync;
import com.dotcms.model.asset.FolderSync.Builder;
import com.dotcms.model.asset.FolderView;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Recursive task for traversing a file system directory and building a hierarchical tree
 * representation of its contents. The folders and contents are compared to the remote server in
 * order to determine if there are any differences between the local and remote file system. This
 * task is used to split the traversal into smaller sub-tasks that can be executed in parallel,
 * allowing for faster traversal of large directory structures.
 */
@Dependent
public class LocalFolderTraversalTask extends
        TaskProcessor<LocalFolderTraversalTaskParams, CompletableFuture<TraverseTaskResult>> {

    private final ManagedExecutor executor;

    private final Logger logger;

    private final Retriever retriever;

    private final FileHashCalculatorService fileHashService;

    private LocalFolderTraversalTaskParams traversalTaskParams;

    /**
     * Constructs a new LocalFolderTraversalTask instance.
     *
     * @param executor        the executor for parallel execution of traversal tasks
     * @param logger          the logger for logging debug information
     * @param retriever       The retriever used for REST calls and other operations.
     * @param fileHashService The file hash calculator service
     */
    public LocalFolderTraversalTask(final Logger logger, final ManagedExecutor executor,
            final Retriever retriever, final FileHashCalculatorService fileHashService) {
        this.executor = executor;
        this.logger = logger;
        this.retriever = retriever;
        this.fileHashService = fileHashService;
    }

    /**
     * Sets the traversal parameters for the LocalFolderTraversalTask. This method provides a way to
     * inject necessary configuration after the instance of LocalFolderTraversalTask has been
     * created by the container, which is a common pattern when working with frameworks like Quarkus
     * that manage object creation and dependency injection in a specific manner.
     * <p>
     * This method is used as an alternative to constructor injection, which is not feasible due to
     * the limitations or constraints of the framework's dependency injection mechanism. It allows
     * for the explicit setting of traversal parameters after the object's instantiation, ensuring
     * that the executor is properly configured before use.
     *
     * @param params The traversal parameters
     */
    @Override
    public void setTaskParams(final LocalFolderTraversalTaskParams params) {
        this.traversalTaskParams = params;
    }

    /**
     * Returns the appropriate FileFilter based on whether DotCliIgnore is configured.
     * If DotCliIgnore is present in the task parameters, returns a DotCliIgnoreFileFilter
     * that filters based on .dotcliignore patterns. Otherwise, returns the default HiddenFileFilter.
     *
     * @return FileFilter instance for filtering files during traversal
     */
    private FileFilter getFileFilter() {
        return traversalTaskParams.dotCliIgnore()
                .map(DotCliIgnoreFileFilter::new)
                .map(FileFilter.class::cast)
                .orElse(new HiddenFileFilter());
    }

    /**
     * Executes the folder traversal task and returns a TreeNode representing the directory tree
     * rooted at the folder specified in the constructor.
     *
     * @return A CompletableFuture containing the TreeNode representing the directory tree rooted at
     * the folder specified in the constructor and a list of exceptions encountered during traversal.
     */
    @Override
    public CompletableFuture<TraverseTaskResult> compute() {

        var errors = new ArrayList<Exception>();

        File folderOrFile = new File(traversalTaskParams.sourcePath());

        AtomicReference<TreeNode> currentNode = new AtomicReference<>();
        try {
            final var localPathStructure = parseLocalPath(traversalTaskParams.workspace(),
                    folderOrFile);
            currentNode.set(gatherSyncInformation(traversalTaskParams.workspace(), folderOrFile,
                    localPathStructure));
        } catch (Exception e) {
            if (traversalTaskParams.failFast()) {
                return CompletableFuture.failedFuture(e);
            } else {
                errors.add(e);
            }
        }

        return handleFolder(errors, currentNode, folderOrFile);
    }

    /**
     * Handles a folder by traversing it and its subdirectories, creating tasks to process each
     * subdirectory. Any encountered errors are stored in an ArrayList and the resulting tree node
     * is returned.
     *
     * @param errors       The list of exceptions encountered during traversal.
     * @param currentNode  The current tree node.
     * @param folderOrFile The folder or file to handle.
     * @return A CompletableFuture containing the list of exceptions encountered and the resulting
     * tree node.
     */
    private CompletableFuture<TraverseTaskResult> handleFolder(
            final ArrayList<Exception> errors, final AtomicReference<TreeNode> currentNode,
            final File folderOrFile) {

        if (null != currentNode.get() && folderOrFile.isDirectory()) {

            List<CompletableFuture<TraverseTaskResult>> futures = new ArrayList<>();

            File[] files = folderOrFile.listFiles(getFileFilter());

            if (files != null) {

                for (File file : files) {

                    if (file.isDirectory()) {

                        var subTask = new LocalFolderTraversalTask(
                                logger, executor, retriever, fileHashService
                        );

                        subTask.setTaskParams(LocalFolderTraversalTaskParams.builder()
                                .from(traversalTaskParams)
                                .sourcePath(file.getAbsolutePath())
                                .build()
                        );

                        CompletableFuture<TraverseTaskResult> future =
                                CompletableFuture.supplyAsync(
                                        subTask::compute, executor
                                ).thenCompose(Function.identity());
                        futures.add(future);
                    }
                }

                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(ignored -> {
                            for (CompletableFuture<TraverseTaskResult> future : futures) {
                                var taskResult = future.join();
                                errors.addAll(taskResult.exceptions());
                                taskResult.treeNode().ifPresent(currentNode.get()::addChild);
                            }

                            final TreeNode treeNode = currentNode.get();
                            //If the task failed to complete ad the exception got added to the list
                            // of errors instead of being thrown current node will be null
                            return TraverseTaskResult.builder()
                                    .treeNode(Optional.ofNullable(treeNode))
                                    .exceptions(errors)
                                    .build();
                        });
            }
        }

        final TreeNode treeNode = currentNode.get();
        //If the task failed to complete ad the exception got added to the list of errors instead
        // of being thrown current node will be null
        return CompletableFuture.completedFuture(TraverseTaskResult.builder()
                .treeNode(Optional.ofNullable(treeNode))
                .exceptions(errors)
                .build());
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

            File[] files = folderOrFile.listFiles(getFileFilter());

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
                var message = String.format("Error processing folder [%s]",
                        folderOrFile.getAbsolutePath());
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

            final var parentLocalPathStructure = parseLocalPath(workspaceFile,
                    folderOrFile.getParentFile());
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

                    var asset = assetViewFromFile(
                            localPathStructure,
                            pushInfo.fileHash()
                    );
                    final AssetSync syncData = AssetSync.builder().
                            markedForPush(true).
                            pushType(pushInfo.pushType()).
                            build();
                    asset.sync(syncData);
                    assetVersions.addVersions(
                            asset.build()
                    );
                } else {
                    logger.debug(String.format(
                            "File [%s] - live [%b] - lang [%s] - already exist in the server.",
                            localPathStructure.filePath(), live, lang));
                }
            } catch (Exception e) {
                var message = String.format("Error processing file [%s]",
                        folderOrFile.getAbsolutePath());
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
     * @param workspaceFile        the workspace file
     * @param live                 the live status
     * @param lang                 the language
     * @param assetVersionsBuilder the parent folder asset versions view builder
     * @param folderChildren       the files to check
     * @param remoteFolder         the remote folder
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
                        pushInfo = pushInfoForNoRemote(file);
                    }

                    if (pushInfo.push()) {

                        logger.debug(String.format(
                                "Marking file [%s] - live [%b] - lang [%s] for push " +
                                        "- New [%b] - Modified [%b].",
                                file.toPath(), live, lang, pushInfo.isNew(),
                                pushInfo.isModified()));

                        final AssetSync syncData = AssetSync.builder()
                                .markedForPush(true)
                                .pushType(pushInfo.pushType())
                                .build();

                        assetVersionsBuilder.addVersions(
                                assetViewFromFile(workspaceFile, file, pushInfo.fileHash()).
                                        sync(syncData).
                                        build()
                        );
                    } else {
                        logger.debug(String.format(
                                "File [%s] - live [%b] - lang [%s] - already exist in the server.",
                                file.toPath(), live, lang));
                    }
                }
            }
        }
    }

    /**
     * Checks if folders need to be pushed to the remote server.
     *
     * @param folder       the parent folder view builder
     * @param remoteFolder the remote folder
     * @param folderFiles  the internal files of the folder
     */
    private void checkFolderToPush(FolderView.Builder folder,
            FolderView remoteFolder,
            File[] folderFiles) {

        if (remoteFolder == null) {
            boolean markForPush = false;
            if (traversalTaskParams.ignoreEmptyFolders()) {
                if (folderFiles != null && folderFiles.length > 0) {
                    // Does not exist on remote server, so we need to push it
                    markForPush = true;
                }
            } else {
                // Does not exist on remote server, so we need to push it
                markForPush = true;
            }
            folder.sync(FolderSync.builder().markedForPush(markForPush).build());
        }
    }

    /**
     * Checks if files need to be removed from the remote server.
     *
     * @param live           the live status
     * @param lang           the language
     * @param assetVersions  the parent folder asset versions view builder
     * @param folderChildren the files to check
     * @param remoteFolder   the remote folder
     */
    private void checkAssetsToRemove(boolean live, String lang,
            AssetVersionsView.Builder assetVersions,
            File[] folderChildren, FolderView remoteFolder) {

        // The option to remove assets is disabled
        if (!traversalTaskParams.removeAssets()) {
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
     * @param live           the live status
     * @param lang           the language
     * @param folder         the parent folder view builder
     * @param folderChildren the files to check
     * @param remoteFolder   the remote folder
     */
    private void checkFoldersToRemove(boolean live, String lang,
            FolderView.Builder folder,
            File[] folderChildren, FolderView remoteFolder) {

        // The option to remove folders is disabled
        if (!traversalTaskParams.removeFolders() && !traversalTaskParams.removeAssets()) {
            return;
        }

        if (null != remoteFolder && null != remoteFolder.subFolders()) {

            for (var subFolder : remoteFolder.subFolders()) {

                final boolean remove = !(findLocalFolderMatch(folderChildren, subFolder));

                if (remove) {

                    // Folder exist on remote server, but not locally, so we need to remove it and also the assets
                    // inside of it, this is important because depending on the status (live/working), a "delete" of a
                    // folder can be an "un-publish" of the assets inside it or a "delete" of the folder itself, we need
                    // to have all the assets inside the folder to be able to handle all the cases.
                    var remoteSubFolder = retrieveFolder(subFolder.host(), subFolder.path());
                    if (remoteSubFolder != null) {

                        boolean ignore = false;

                        if (traversalTaskParams.ignoreEmptyFolders()) {
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
                            logger.debug(String.format("Marking folder [%s] for delete.",
                                    subFolder.path()));
                            if (traversalTaskParams.removeFolders()) {
                                final Optional<FolderSync> existingSyncData = subFolder.sync();
                                final Builder builder = FolderSync.builder();
                                existingSyncData.ifPresent(builder::from);
                                subFolder = subFolder.withSync(
                                        builder.markedForDelete(true).build());
                            }
                            folder.addSubFolders(subFolder);
                        }
                    }
                }
            }
        }
    }

    /**
     * explore assets in the remote folder and  figure out if they match status and language if so
     * we can not ignore the remote folder cuz it has assets matching the status and language were
     * in
     *
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
     *
     * @param folderChildren
     * @param remote
     * @return
     */
    private boolean findLocalFolderMatch(File[] folderChildren, FolderView remote) {

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
     * @return The AssetVersionsView representing the retrieved asset data, or null if it doesn't
     * exist
     */
    private AssetVersionsView retrieveAsset(LocalPathStructure localPathStructure) {

        if (!traversalTaskParams.siteExists()) {
            // Site doesn't exist on remote server
            // No need to pass a siteExists flag we could NullRetriever when the site doesn't exist
            return null;
        }

        AssetVersionsView remoteAsset = null;

        try {
            remoteAsset = retriever.retrieveAssetInformation(
                    localPathStructure.site(),
                    localPathStructure.folderPath(),
                    localPathStructure.fileName()
            );
        } catch (NotFoundException e) {
            // File doesn't exist on remote server
            logger.debug(
                    String.format("Local file [%s] in folder [%s] doesn't exist on remote server.",
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

        if (!traversalTaskParams.siteExists()) {
            // Site doesn't exist on remote server
            return null;
        }

        FolderView remoteFolder = null;

        try {
            remoteFolder = retriever.retrieveFolderInformation(site, folderPath);
        } catch (NotFoundException e) {
            // Folder doesn't exist on remote server
            logger.debug(
                    String.format("Local folder [%s] doesn't exist on remote server.", folderPath));
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
            return pushInfoForNoRemote(file);
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

        // Local SHA-256
        final String localFileHash = fileHashService.sha256toUnixHash(file.toPath());

        var push = true;
        var isNew = true;
        var modifed = false;

        if (!Strings.isNullOrEmpty(remoteFileHash)) { // We found the file in the remote server

            // Verify if we need to push the file
            if (localFileHash.equals(remoteFileHash)) {
                push = false;
            } else {
                isNew = false;
                modifed = true;
            }
        }

        return new PushInfo(push, isNew, modifed, localFileHash);
    }

    /**
     * Generates the push information for a file that does not have a remote counterpart.
     *
     * @param file The file for which to generate push information.
     * @return The PushInfo object representing the push information for the file.
     */
    private PushInfo pushInfoForNoRemote(File file) {

        // Local SHA-256
        final String localFileHash = fileHashService.sha256toUnixHash(file.toPath());

        return new PushInfo(true, true, false, localFileHash);
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
     * @param fileHash      the file hash
     * @return The AssetView.Builder representing the asset view
     */
    private AssetView.Builder assetViewFromFile(final File workspaceFile, final File file,
            final String fileHash) {

        final var localPathStructure = parseLocalPath(workspaceFile, file);
        return assetViewFromFile(localPathStructure, fileHash);
    }

    /**
     * Builds an AssetView object from a local path structure.
     *
     * @param localPathStructure the local path structure
     * @param fileHash           the file hash
     * @return The AssetView.Builder representing the asset view
     */
    private AssetView.Builder assetViewFromFile(final LocalPathStructure localPathStructure,
            final String fileHash) {

        var metadata = new HashMap<String, Object>();
        metadata.put(PATH_META_KEY.key(), localPathStructure.folderPath());
        metadata.put(SHA256_META_KEY.key(), fileHash);

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
        private final String fileHash;

        /**
         * Represents the push information for a file.
         *
         * @param push       whether the file should be pushed
         * @param isNew      whether the file is new
         * @param isModified whether the file has been modified
         * @param fileHash   the file hash
         */
        PushInfo(final boolean push, final boolean isNew, final boolean isModified,
                final String fileHash) {
            this.push = push;
            this.isNew = isNew;
            this.isModified = isModified;
            this.fileHash = fileHash;
        }

        /**
         * Returns whether the file should be pushed.
         *
         * @return true if the file should be pushed, false otherwise
         */
        boolean push() {
            return push;
        }

        /**
         * Returns whether the file is new.
         *
         * @return true if the file is new, false otherwise
         */
        boolean isNew() {
            return isNew;
        }

        /**
         * Returns whether the file has been modified.
         *
         * @return true if the file has been modified, false otherwise
         */
        boolean isModified() {
            return isModified;
        }

        /**
         * Returns the file hash.
         *
         * @return The file hash
         */
        String fileHash() {
            return fileHash;
        }

        /**
         * Determines the push type for a file based on its synchronization information.
         *
         * @return The push type for the file: NEW, MODIFIED, or UNKNOWN
         */
        PushType pushType() {
            PushType pushType = isNew() ? PushType.NEW : PushType.UNKNOWN;
            if (pushType == PushType.UNKNOWN) {
                pushType = isModified() ? PushType.MODIFIED : PushType.UNKNOWN;
            }
            return pushType;
        }

    }

}