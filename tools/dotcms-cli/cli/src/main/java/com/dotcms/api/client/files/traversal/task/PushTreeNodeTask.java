package com.dotcms.api.client.files.traversal.task;

import static com.dotcms.common.AssetsUtils.isMarkedForDelete;
import static com.dotcms.common.AssetsUtils.isMarkedForPush;
import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;

import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.client.files.traversal.exception.SiteCreationException;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.command.PushContext;
import com.dotcms.cli.common.FilesUtils;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.site.SiteView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Represents a task that pushes the contents of a tree node to a remote server.
 */
@Dependent
public class PushTreeNodeTask extends
        TaskProcessor<PushTreeNodeTaskParams, CompletableFuture<List<Exception>>> {

    private final ManagedExecutor executor;

    private final PushContext pushContext;

    private final Pusher pusher;

    private final Logger logger;

    private PushTreeNodeTaskParams traversalTaskParams;

    /**
     * Constructs a new PushTreeNodeTask with the specified parameters.
     *
     * @param logger               logger for logging messages
     * @param executor             the executor for parallel execution of pushing tasks
     * @param pushContext          the push shared Context
     * @param pusher               the pusher to use for pushing the contents of the tree node
     */
    public PushTreeNodeTask(final Logger logger,
            final ManagedExecutor executor,
            final PushContext pushContext,
            final Pusher pusher) {
        this.executor = executor;
        this.pushContext = pushContext;
        this.pusher = pusher;
        this.logger = logger;
    }

    /**
     * Sets the traversal parameters for the PushTreeNodeTask. This method provides a way to inject
     * necessary configuration after the instance of PushTreeNodeTask has been created by the
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
    public void setTaskParams(final PushTreeNodeTaskParams params) {
        this.traversalTaskParams = params;
    }

    @Override
    public CompletableFuture<List<Exception>> compute() {

        var errors = new ArrayList<Exception>();
        final TreeNode rootNode = traversalTaskParams.rootNode();

        // Handle the folder for the current node
        try {
            processFolder(rootNode.folder(), pushContext);
        } catch (Exception e) {

            // If we failed to create the site there is not point in continuing
            if (e instanceof SiteCreationException || traversalTaskParams.failFast()) {
                traversalTaskParams.progressBar().done();

                return CompletableFuture.failedFuture(e);
            } else {
                errors.add(e);
            }
        }

        // Handle assets for the current node
        for (AssetView asset : rootNode.assets()) {
            try {
                processAsset(rootNode.folder(), asset, pushContext);
            } catch (Exception e) {
                if (traversalTaskParams.failFast()) {
                    traversalTaskParams.progressBar().done();

                    return CompletableFuture.failedFuture(e);
                } else {
                    errors.add(e);
                }
            }
        }

        return handleChildren(errors, rootNode);
    }

    /**
     * Recursively build the file system tree for the children nodes
     *
     * @param errors   the list of errors to add to
     * @param rootNode the root node to process
     */
    private CompletableFuture<List<Exception>> handleChildren(final ArrayList<Exception> errors,
            final TreeNode rootNode) {

        if (rootNode.children() != null && !rootNode.children().isEmpty()) {

            List<CompletableFuture<List<Exception>>> futures = new ArrayList<>();

            for (TreeNode child : rootNode.children()) {

                var task = new PushTreeNodeTask(
                        logger,
                        executor,
                        pushContext,
                        pusher
                );
                task.setTaskParams(PushTreeNodeTaskParams.builder()
                        .from(traversalTaskParams).rootNode(child).build()
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
     * Processes the folder associated with the specified FolderView.
     *
     * @param folder the folder to process
     */
    private void processFolder(final FolderView folder, PushContext pushContext) {

        if (isMarkedForDelete(folder)) {// Delete
            doDeleteFolder(folder, pushContext);
        } else if (isMarkedForPush(folder)) {// Push
            doPushFolder(folder, pushContext);
        }

    }

    /**
     * Processes the asset associated with the specified AssetView.
     *
     * @param folder      the folder associated with the asset
     * @param pushContext the push context
     */
    private void doPushFolder(FolderView folder, PushContext pushContext) {
        var isSite = Objects.equals(folder.path(), "/") && Objects.equals(folder.name(), "/");
        try {
            if (isSite) {
                final String status = traversalTaskParams.localPaths().status();
                // And we need to create the non-existing site
                final Optional<SiteView> optional = pushContext.execPush(folder.host(),
                        () -> Optional.of(
                                pusher.pushSite(folder.host(), status))
                );
                if (optional.isPresent()) {
                    logger.debug(String.format("Site [%s] created", folder.host()));
                } else {
                    logger.debug(String.format("Site [%s] already pushed", folder.host()));
                }
            } else {

                // Creating the non-existing folder only if it hasn't been created already
                final Optional<Map<String, Object>> optional = pushContext.execPush(
                        String.format("%s/%s", folder.host(), folder.path()),
                        () -> {
                            final List<Map<String, Object>> created = pusher.createFolder(
                                    folder.host(), folder.path());
                            if (created != null && !created.isEmpty()) {
                                return Optional.of(created.get(0));
                            }
                            return Optional.empty();
                        });
                if (optional.isPresent()) {
                    logger.debug(String.format("Folder [%s] created", folder.path()));
                } else {
                    logger.debug(String.format("Folder [%s] already exist", folder.path()));
                }
            }
        } catch (Exception e) {
            var message = String.format("Error creating %s [%s]",
                    isSite ? "site" : "folder",
                    isSite ? folder.host() : folder.path());
            logger.debug(message, e);
            if (isSite) {

                // Using the exception to check if the site already exist
                var alreadyExist = checkIfSiteAlreadyExist(e);

                // If we are trying to create a site that already exist we could ignore the error on retries
                if (!traversalTaskParams.isRetry() || !alreadyExist) {
                    logger.error(message, e);
                    throw new SiteCreationException(message, e);
                }
            } else {
                // Using the exception to check if the folder already exist
                var alreadyExist = checkIfAssetOrFolderAlreadyExist(e);

                // If we are trying to create a folder that already exist we could ignore the error on retries
                if (!traversalTaskParams.isRetry() || !alreadyExist) {
                    logger.error(message, e);

                    if (e instanceof TraversalTaskException) {
                        throw (TraversalTaskException) e;
                    }

                    throw new TraversalTaskException(message, e);
                }
            }
        } finally {
            // Folder processed, updating the progress bar
            traversalTaskParams.progressBar().incrementStep();
        }
    }

    /**
     * Performs the deletion of the specified folder.
     *
     * @param folder      the folder to check
     * @param pushContext the push context
     */
    private void doDeleteFolder(FolderView folder, PushContext pushContext) {
        try {
            final Optional<Boolean> delete = pushContext.execDelete(
                    String.format("%s/%s", folder.host(), folder.path()),
                    () -> Optional.of(pusher.deleteFolder(folder.host(), folder.path())));
            if (delete.isPresent()) {
                logger.debug(String.format("Folder [%s] deleted", folder.path()));
            } else {
                logger.debug(String.format("Folder [%s] already deleted", folder.path()));
            }

        } catch (Exception e) {

            // If we are trying to delete a folder that does not exist anymore we could ignore the error on retries
            if (!traversalTaskParams.isRetry() || !(e instanceof NotFoundException)) {

                var message = String.format("Error deleting folder [%s]", folder.path());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

        } finally {
            // Folder processed, updating the progress bar
            traversalTaskParams.progressBar().incrementStep();
        }
    }

    /**
     * Processes the asset associated with the specified AssetView.
     *
     * @param folder the folder containing the asset
     * @param asset  the asset to process
     */
    private void processAsset(final FolderView folder, final AssetView asset,
            final PushContext pushContext) {
        if (isMarkedForDelete(asset)) {
            doDeleteAsset(folder, asset, pushContext);
        } else if (isMarkedForPush(asset)) {
            doPushAsset(folder, asset, pushContext);
        }
    }

    /**
     * Performs a push operation on the specified asset.
     *
     * @param folder      the folder containing the asset
     * @param asset       the asset to push
     * @param pushContext the push context
     */
    private void doPushAsset(FolderView folder, AssetView asset, PushContext pushContext) {
        try {

            final String cleaned = FilesUtils.cleanFileName(asset.name());
            //Invalid characters found in the asset name
            if (!cleaned.equals(asset.name())) {
                logger.warn(String.format("Asset [%s%s] has invalid characters in the name. Skipping push on this file for security reasons.",
                        folder.path(), asset.name()));
                //We don't want to push assets with invalid characters we report them and move on
                 throw new TraversalTaskException(String.format("Asset [%s%s] has invalid characters in the name. Skipping push on this file for security reasons.",
                        folder.path(), asset.name()));
            }

            final String pushAssetKey = generatePushAssetKey(folder, asset);
            final Optional<AssetView> optional = pushContext.execPush(
                    pushAssetKey,
                    () -> {
                        // Pushing the asset (and creating the folder if needed
                        final AssetView assetView = pusher.push(traversalTaskParams.workspacePath(),
                                traversalTaskParams.localPaths().status(),
                                traversalTaskParams.localPaths().language(),
                                traversalTaskParams.localPaths().site(),
                                folder.path(), asset.name()
                        );
                        return Optional.of(assetView);
                    });

            if (optional.isPresent()) {
                logger.debug(String.format("Asset [%s%s] pushed", folder.path(), asset.name()));
            } else {
                logger.debug(
                        String.format("Asset [%s%s] already pushed", folder.path(), asset.name()));
            }
        } catch (Exception e) {

            // Using the exception to check if the asset already exist
            var alreadyExist = checkIfAssetOrFolderAlreadyExist(e);

            // If we are trying to push an asset that already exist we could ignore the error on retries
            if (!traversalTaskParams.isRetry() || !alreadyExist) {

                if (e instanceof TraversalTaskException) {
                    throw (TraversalTaskException) e;
                }

                var message = String.format("Error pushing asset [%s%s]", folder.path(),
                        asset.name());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

        } finally {
            // Asset processed, updating the progress bar
            traversalTaskParams.progressBar().incrementStep();
        }
    }

    /**
     * Performs a delete operation on the specified asset.
     *
     * @param folder      the folder containing the asset
     * @param asset       the asset to delete
     * @param pushContext the push context
     */
    private void doDeleteAsset(final FolderView folder, final AssetView asset,
            final PushContext pushContext) {
        try {
            // Check if we already deleted the folder
            if (isMarkedForDelete(folder)) {
                // Folder already deleted, we don't need to delete the asset
                logger.debug(String.format(
                        "Folder [%s] already deleted, ignoring deletion of [%s] asset",
                        folder.path(), asset.name()));
            } else {
                final Optional<Boolean> optional = pushContext.execArchive(
                        String.format("%s/%s/%s", folder.host(), folder.path(), asset.name()),
                        () -> {
                            final Boolean archive = pusher.archive(folder.host(), folder.path(),
                                    asset.name());
                            if (archive) {
                                return Optional.of(true);
                            }
                            return Optional.empty();
                        });
                if (optional.isPresent()) {
                    logger.debug(
                            String.format("Asset [%s%s] archived", folder.path(), asset.name()));
                } else {
                    logger.debug(String.format("Asset [%s%s] already archived", folder.path(),
                            asset.name()));
                }
            }
        } catch (Exception e) {

            // If we are trying to delete an asset that does not exist anymore we could ignore the error on retries
            if (!traversalTaskParams.isRetry() || !(e instanceof NotFoundException)) {

                var message = String.format("Error deleting asset [%s%s]", folder.path(),
                        asset.name());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

        } finally {
            // Asset processed, updating the progress bar
            traversalTaskParams.progressBar().incrementStep();
        }
    }

    /**
     * Generates a push asset key based on the provided folder and asset.
     *
     * @param folder the folder associated with the asset
     * @param asset  the asset for which the key needs to be generated
     * @return the generated push asset key
     */
    private String generatePushAssetKey(FolderView folder, AssetView asset) {

        var assetHash = (String) asset.metadata().get(SHA256_META_KEY.key());

        // We don't include the status in the key as this logic expects to always run in the same
        //  order, live first, working second.
        //  If already pushed and the order is respected, we don't need to push the same file again.
        return String.format("%s/%s/%s/%s/%s",
                traversalTaskParams.localPaths().language(),
                folder.host(),
                folder.path(),
                asset.name(),
                assetHash
        );
    }

    /**
     * Checks if the exception indicates that the site already exists.
     *
     * @param e the exception to check
     * @return true if the site or folder already exists, false otherwise
     */
    private boolean checkIfSiteAlreadyExist(Exception e) {

        var alreadyExist = false;
        if (e instanceof WebApplicationException) {
            var webApplicationException = (WebApplicationException) e;
            var status = webApplicationException.getResponse().getStatus();
            var responseMessage = e.getMessage();

            if (status == 400 && responseMessage.contains("already exists")) {
                alreadyExist = true;
            }
        }
        return alreadyExist;
    }

    /**
     * Checks if the exception indicates that the asset or folder already exists.
     *
     * @param e the exception to check
     * @return true if the asset already exists, false otherwise
     */
    private boolean checkIfAssetOrFolderAlreadyExist(Exception e) {

        var alreadyExist = false;
        if (e instanceof WebApplicationException) {
            var webApplicationException = (WebApplicationException) e;
            var status = webApplicationException.getResponse().getStatus();
            var responseMessage = e.getMessage();

            if (status == 400 && responseMessage.contains("duplicate key")) {
                alreadyExist = true;
            }
        }
        return alreadyExist;
    }

}
