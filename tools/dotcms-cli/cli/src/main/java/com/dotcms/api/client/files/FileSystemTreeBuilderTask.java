package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.asset.SearchByPathRequest;
import com.dotcms.security.Utils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;

/**
 * Recursive task for building the file system tree.
 */
public class FileSystemTreeBuilderTask extends RecursiveTask<Void> {

    private final TreeNode rootNode;
    private final String destination;
    private final boolean generateEmptyFolders;
    private final String language;

    private final Downloader downloader;

    private final ConsoleProgressBar progressBar;

    private Logger logger;

    /**
     * Constructs a new FileSystemTreeBuilderTask.
     *
     * @param logger               logger for logging messages
     * @param downloader           class responsible for handling the downloading of assets
     *                             from a given path through the AssetAPI
     * @param rootNode             the root node of the file system tree
     * @param destination          the destination path to save the pulled files
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param language             the language of the assets
     * @param progressBar          the progress bar for tracking the pull progress
     */
    public FileSystemTreeBuilderTask(final Logger logger,
                                     final Downloader downloader,
                                     final TreeNode rootNode,
                                     final String destination,
                                     final boolean generateEmptyFolders,
                                     final String language,
                                     final ConsoleProgressBar progressBar) {
        this.logger = logger;
        this.downloader = downloader;
        this.rootNode = rootNode;
        this.destination = destination;
        this.generateEmptyFolders = generateEmptyFolders;
        this.language = language;
        this.progressBar = progressBar;
    }

    @Override
    protected Void compute() {

        // Create the folder for the current node
        createFolderInFileSystem(destination, rootNode.folder());

        // Create files for the assets in the current node
        for (AssetView asset : rootNode.assets()) {
            createFileInFileSystem(destination, rootNode.folder(), asset);
        }

        // Recursively build the file system tree for the children nodes
        if (rootNode.children() != null && !rootNode.children().isEmpty()) {

            List<FileSystemTreeBuilderTask> tasks = new ArrayList<>();
            for (TreeNode child : rootNode.children()) {
                var task = new FileSystemTreeBuilderTask(
                        logger,
                        downloader,
                        child,
                        destination,
                        generateEmptyFolders,
                        language,
                        progressBar
                );
                task.fork();
                tasks.add(task);
            }

            for (FileSystemTreeBuilderTask task : tasks) {
                task.join();
            }
        }

        return null;
    }

    /**
     * Creates the corresponding folder in the file system for a given folder view.
     *
     * @param destination the destination path to save the pulled files
     * @param folder      the folder view representing the folder
     */
    private void createFolderInFileSystem(final String destination, final FolderView folder) {

        // Create the corresponding folder in the file system
        var folderPath = Paths.get(destination, folder.path());

        try {
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                logger.debug("Created folder: " + folderPath);
            } else {
                logger.debug(String.format("Skipping folder [%s], it already exists in the file system", folderPath));
            }
        } catch (IOException e) {
            var message = String.format("Error creating folder [%s]", folderPath);
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

        String remoteAssetURL = generateRemoteURL(folder, asset);
        var assetFilePath = Paths.get(destination, folder.path(), asset.name());

        // Remove SHA-256
        final String remoteFileHash = (String) asset.metadata().get(SHA256_META_KEY.key());

        // Local SHA-256
        String localFileHash = null;
        if (Files.exists(assetFilePath)) {
            localFileHash = Utils.Sha256toUnixHash(assetFilePath);
        }

        // Verify if we need to download the file
        if (localFileHash == null || !localFileHash.equals(remoteFileHash)) {

            logger.debug("Downloading file: " + remoteAssetURL);

            // Download the file
            try (var inputStream = this.downloader.download(SearchByPathRequest.builder().
                    assetPath(remoteAssetURL).
                    language(language).
                    live(asset.live()).
                    build())) {

                // Copy the contents of the InputStream to the target file
                Files.copy(inputStream, assetFilePath, StandardCopyOption.REPLACE_EXISTING);

                logger.debug(String.format("Downloaded file [%s] to [%s] ", remoteAssetURL, assetFilePath));
            } catch (IOException e) {
                var message = String.format("Error downloading file [%s] to [%s] ", remoteAssetURL, assetFilePath);
                logger.debug(message, e);
                throw new RuntimeException(message, e);
            }
        } else {
            logger.debug(String.format("Skipping file [%s], it already exists in the file system",
                    remoteAssetURL));
        }

        // Downloaded file, updating the progress bar
        progressBar.incrementStep();
    }

    /**
     * Generates the remote URL for a given folder and asset.
     *
     * @param folder the folder view representing the parent folder
     * @param asset  the asset view representing the asset
     * @return the remote URL for the asset
     */
    private static String generateRemoteURL(FolderView folder, AssetView asset) {

        String remoteAssetURL;
        if ("/".equals(folder.path())) {
            if (folder.name().isEmpty() || folder.name().equals("/")) {
                remoteAssetURL = "//" + folder.host() + "/" + asset.name();
            } else {
                remoteAssetURL = "//" + folder.host() + "/" + folder.name() + "/" + asset.name();
            }
        } else {
            remoteAssetURL = "//" + folder.host() + folder.path() + asset.name();
        }
        return remoteAssetURL;
    }

}
