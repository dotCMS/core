package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.client.files.traversal.data.Downloader;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.model.asset.AssetRequest;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.security.Utils;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import static com.dotcms.common.AssetsUtils.BuildRemoteAssetURL;
import static com.dotcms.common.AssetsUtils.BuildRemoteURL;
import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;

/**
 * Recursive task for pulling the contents of a tree node from a remote server.
 */
public class PullTreeNodeTask extends RecursiveTask<List<Exception>> {

    private final TreeNode rootNode;
    private final String destination;
    private final boolean overwrite;
    private final boolean generateEmptyFolders;
    private final boolean failFast;
    private final String language;

    private final Downloader downloader;

    private final ConsoleProgressBar progressBar;

    private Logger logger;

    /**
     * Constructs a new PullTreeNodeTask.
     *
     * @param logger               logger for logging messages
     * @param downloader           class responsible for handling the downloading of assets
     *                             from a given path through the AssetAPI
     * @param rootNode             the root node of the file system tree
     * @param destination          the destination path to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param failFast             true to fail fast, false to continue on error
     * @param language             the language of the assets
     * @param progressBar          the progress bar for tracking the pull progress
     */
    public PullTreeNodeTask(final Logger logger,
                            final Downloader downloader,
                            final TreeNode rootNode,
                            final String destination,
                            final boolean overwrite,
                            final boolean generateEmptyFolders,
                            final boolean failFast,
                            final String language,
                            final ConsoleProgressBar progressBar) {
        this.logger = logger;
        this.downloader = downloader;
        this.rootNode = rootNode;
        this.overwrite = overwrite;
        this.destination = destination;
        this.generateEmptyFolders = generateEmptyFolders;
        this.failFast = failFast;
        this.language = language;
        this.progressBar = progressBar;
    }

    @Override
    protected List<Exception> compute() {

        var errors = new ArrayList<Exception>();

        // Create the folder for the current node
        try {
            createFolderInFileSystem(destination, rootNode.folder());
        } catch (Exception e) {
            if (failFast) {
                throw e;
            } else {
                errors.add(e);
            }
        }

        // Create files for the assets in the current node
        for (AssetView asset : rootNode.assets()) {
            try {
                createFileInFileSystem(destination, rootNode.folder(), asset);
            } catch (Exception e) {
                if (failFast) {
                    throw e;
                } else {
                    errors.add(e);
                }
            }
        }

        // Recursively build the file system tree for the children nodes
        if (rootNode.children() != null && !rootNode.children().isEmpty()) {

            List<PullTreeNodeTask> tasks = new ArrayList<>();
            for (TreeNode child : rootNode.children()) {
                var task = new PullTreeNodeTask(
                        logger,
                        downloader,
                        child,
                        destination,
                        overwrite,
                        generateEmptyFolders,
                        failFast,
                        language,
                        progressBar
                );
                task.fork();
                tasks.add(task);
            }

            for (PullTreeNodeTask task : tasks) {
                var taskErrors = task.join();
                errors.addAll(taskErrors);
            }
        }

        return errors;
    }

    /**
     * Creates the corresponding folder in the file system for a given folder view.
     *
     * @param destination the destination path to save the pulled files
     * @param folder      the folder view representing the folder
     */
    private void createFolderInFileSystem(final String destination, final FolderView folder) {

        String remoteFolderURL = generateRemoteFolderURL(folder);

        // Create the corresponding folder in the file system
        var folderPath = Paths.get(destination, folder.path());

        try {
            if (!Files.exists(folderPath)) {

                Files.createDirectories(folderPath);
                logger.debug(String.format("Created folder [%s] to [%s] ", remoteFolderURL, folderPath));
            } else {
                logger.debug(String.format("Skipping folder [%s], it already exists in the file system", remoteFolderURL));
            }
        } catch (Exception e) {
            var message = String.format("Error creating folder [%s] to [%s] --- %s",
                    remoteFolderURL, folderPath, e.getMessage());
            logger.debug(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Creates the file in the file system for a given asset view.
     *
     * @param destination the destination path to save the pulled files
     * @param folder      the folder view representing the parent folder
     * @param asset       the asset view representing the asset
     */
    private void createFileInFileSystem(final String destination, final FolderView folder, final AssetView asset) {

        String remoteAssetURL = generateRemoteAssetURL(folder, asset.name());
        var assetFilePath = Paths.get(destination, folder.path(), asset.name());

        try {

            // Remote SHA-256
            final String remoteFileHash = (String) asset.metadata().get(SHA256_META_KEY.key());

            // Local SHA-256
            String localFileHash = null;
            if (Files.exists(assetFilePath)) {

                if (overwrite) {
                    localFileHash = Utils.Sha256toUnixHash(assetFilePath);
                } else {
                    // If the file already exist, and we are not overwriting files, there is no point in downloading it
                    localFileHash = remoteFileHash; // Fixing hashes so the download is skipped
                }
            }

            // Verify if we need to download the file
            if (localFileHash == null || !localFileHash.equals(remoteFileHash)) {

                logger.debug("Downloading file: " + remoteAssetURL);

                // Download the file
                try (var inputStream = this.downloader.download(AssetRequest.builder().
                        assetPath(remoteAssetURL).
                        language(language).
                        live(asset.live()).
                        build())) {

                    // Copy the contents of the InputStream to the target file
                    Files.copy(inputStream, assetFilePath, StandardCopyOption.REPLACE_EXISTING);
                    logger.debug(String.format("Downloaded file [%s] to [%s] ", remoteAssetURL, assetFilePath));
                }
            } else {
                logger.debug(String.format("Skipping file [%s], it already exists in the file system - Override flag [%b]",
                        remoteAssetURL, overwrite));
            }
        } catch (Exception e) {
            var message = String.format("Error pulling file [%s] to [%s] --- %s",
                    remoteAssetURL, assetFilePath, e.getMessage());
            logger.debug(message, e);
            throw new RuntimeException(message, e);
        } finally {
            // File processed, updating the progress bar
            progressBar.incrementStep();
        }

    }

    /**
     * Generates the remote URL for a given folder
     *
     * @param folder the folder view representing the parent folder
     * @return the remote URL for the folder
     */
    private String generateRemoteFolderURL(final FolderView folder) {
        return BuildRemoteURL(folder.host(), folder.path());
    }

    /**
     * Generates the remote URL for an asset
     *
     * @param folder    the folder view representing the parent folder
     * @param assetName the name of the asset
     * @return the remote URL for the folder
     */
    private String generateRemoteAssetURL(final FolderView folder, final String assetName) {
        return BuildRemoteAssetURL(folder.host(), folder.path(), assetName);
    }

}
