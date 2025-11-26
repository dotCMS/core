package com.dotcms.api.client.files.traversal.task;

import static com.dotcms.common.AssetsUtils.buildRemoteAssetURL;
import static com.dotcms.common.AssetsUtils.buildRemoteURL;
import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;

import com.dotcms.api.client.FileHashCalculatorService;
import com.dotcms.api.client.files.traversal.data.Downloader;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.model.asset.AssetRequest;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jakarta.enterprise.context.Dependent;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Recursive task for pulling the contents of a tree node from a remote server.
 */
@Dependent
public class PullTreeNodeTask extends
        TaskProcessor<PullTreeNodeTaskParams, CompletableFuture<List<Exception>>> {

    private final ManagedExecutor executor;

    private final Downloader downloader;

    private final Logger logger;

    private final FileHashCalculatorService fileHashService;

    private PullTreeNodeTaskParams traversalTaskParams;

    /**
     * Constructs a new PullTreeNodeTask.
     *
     * @param logger          logger for logging messages
     * @param executor        the executor for parallel execution of pulling tasks
     * @param downloader      class responsible for handling the downloading of assets from a given
     *                        path through the AssetAPI
     * @param fileHashService The file hash calculator service
     */
    public PullTreeNodeTask(final Logger logger, final ManagedExecutor executor,
            final Downloader downloader, final FileHashCalculatorService fileHashService) {
        this.logger = logger;
        this.executor = executor;
        this.downloader = downloader;
        this.fileHashService = fileHashService;
    }

    /**
     * Sets the traversal parameters for the PullTreeNodeTask. This method provides a way to inject
     * necessary configuration after the instance of PullTreeNodeTask has been created by the
     * container, which is a common pattern when working with frameworks like Quarkus that manage
     * object creation and dependency injection in a specific manner.
     * <p>
     * This method is used as an alternative to constructor injection, which is not feasible due to
     * the limitations or constraints of the framework's dependency injection mechanism. It allows
     * for the explicit setting of traversal parameters after the object's instantiation, ensuring
     * that the executor is properly configured before use.
     *
     * @param params The traversal parameters
     */
    @Override
    public void setTaskParams(final PullTreeNodeTaskParams params) {
        this.traversalTaskParams = params;
    }

    @Override
    public CompletableFuture<List<Exception>> compute() {

        var errors = new ArrayList<Exception>();

        // Create the folder for the current node
        try {
            createFolderInFileSystem(
                    traversalTaskParams.destination(),
                    traversalTaskParams.rootNode().folder()
            );
        } catch (Exception e) {
            if (traversalTaskParams.failFast()) {
                return CompletableFuture.failedFuture(e);
            } else {
                errors.add(e);
            }
        }

        // Create files for the assets in the current node
        for (AssetView asset : traversalTaskParams.rootNode().assets()) {
            try {
                createFileInFileSystem(
                        traversalTaskParams.destination(),
                        traversalTaskParams.rootNode().folder(),
                        asset
                );
            } catch (Exception e) {
                if (traversalTaskParams.failFast()) {
                    return CompletableFuture.failedFuture(e);
                } else {
                    errors.add(e);
                }
            }
        }

        // Recursively build the file system tree for the children nodes
        return handleChildren(errors);
    }

    /**
     * Recursively build the file system tree for the children nodes
     *
     * @param errors the list of errors to add to
     */
    private CompletableFuture<List<Exception>> handleChildren(ArrayList<Exception> errors) {

        if (traversalTaskParams.rootNode().children() != null &&
                !traversalTaskParams.rootNode().children().isEmpty()) {

            List<CompletableFuture<List<Exception>>> futures = new ArrayList<>();

            for (TreeNode child : traversalTaskParams.rootNode().children()) {

                var task = new PullTreeNodeTask(
                        logger,
                        executor,
                        downloader,
                        fileHashService
                );
                task.setTaskParams(PullTreeNodeTaskParams.builder()
                        .from(traversalTaskParams)
                        .rootNode(child)
                        .build()
                );

                CompletableFuture<List<Exception>> future = CompletableFuture.supplyAsync(
                        task::compute, executor
                ).thenCompose(Function.identity());
                futures.add(future);
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        for (CompletableFuture<List<Exception>> future : futures) {
                            errors.addAll(future.join());
                        }
                        return errors;
                    });
        }

        return CompletableFuture.completedFuture(errors);
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
            var message = String.format("Error creating folder [%s] to [%s]", remoteFolderURL, folderPath);
            logger.error(message, e);
            throw new TraversalTaskException(message, e);
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

                if (traversalTaskParams.overwrite()) {
                    localFileHash = fileHashService.sha256toUnixHash(assetFilePath);
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
                        language(traversalTaskParams.language()).
                        live(asset.live()).
                        build())) {

                    // Copy the contents of the InputStream to the target file
                    Files.copy(inputStream, assetFilePath, StandardCopyOption.REPLACE_EXISTING);
                    logger.debug(String.format("Downloaded file [%s] to [%s] ", remoteAssetURL, assetFilePath));
                }
            } else {
                logger.debug(String.format("Skipping file [%s], it already exists in the file system - Override flag [%b]",
                        remoteAssetURL, traversalTaskParams.overwrite()));
            }
        } catch (Exception e) {
            var message = String.format("Error pulling file [%s] to [%s]", remoteAssetURL, assetFilePath);
            logger.error(message, e);
            throw new TraversalTaskException(message, e);
        } finally {
            // File processed, updating the progress bar
            traversalTaskParams.progressBar().incrementStep();
        }

    }

    /**
     * Generates the remote URL for a given folder
     *
     * @param folder the folder view representing the parent folder
     * @return the remote URL for the folder
     */
    private String generateRemoteFolderURL(final FolderView folder) {
        return buildRemoteURL(folder.host(), folder.path());
    }

    /**
     * Generates the remote URL for an asset
     *
     * @param folder    the folder view representing the parent folder
     * @param assetName the name of the asset
     * @return the remote URL for the folder
     */
    private String generateRemoteAssetURL(final FolderView folder, final String assetName) {
        return buildRemoteAssetURL(folder.host(), folder.path(), assetName);
    }

}
