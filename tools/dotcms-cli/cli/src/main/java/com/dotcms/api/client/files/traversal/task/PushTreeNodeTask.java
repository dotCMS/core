package com.dotcms.api.client.files.traversal.task;

import static com.dotcms.common.AssetsUtils.isMarkedForDelete;
import static com.dotcms.common.AssetsUtils.isMarkedForPush;

import com.dotcms.api.client.files.traversal.PushTraverseParams;
import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.client.files.traversal.exception.SiteCreationException;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.command.PushContext;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.site.SiteView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.RecursiveTask;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

/**
 * Represents a task that pushes the contents of a tree node to a remote server.
 */
public class PushTreeNodeTask extends RecursiveTask<List<Exception>> {

    private final PushTraverseParams params;
    private final Pusher pusher;
    private final Logger logger;
    private final transient ConsoleProgressBar progressBar;

    /**
     * Constructs a new PushTreeNodeTask with the specified parameters.
     * @param params
     */
    public PushTreeNodeTask(final PushTraverseParams params) {
        this.params = params;
        this.pusher = params.pusher();
        this.logger = params.logger();
        this.progressBar = params.progressBar();
    }

    @Override
    protected List<Exception> compute() {
        var pushContext = params.pushContext();
        var errors = new ArrayList<Exception>();
        final TreeNode rootNode = params.rootNode();
        // Handle the folder for the current node
        try {
            processFolder(rootNode.folder(), pushContext);
        } catch (Exception e) {

            if (params.failFast()) {
                throw e;
            } else {
                errors.add(e);
            }

            // If we failed to create the site there is not point in continuing
            if (e instanceof SiteCreationException) {
                return errors;
            }
        }

        // Handle assets for the current node
        for (AssetView asset : rootNode.assets()) {
            try {
                processAsset(rootNode.folder(), asset, pushContext);
            } catch (Exception e) {
                if (params.failFast()) {
                    //This adds a line so when the exception gets written to the console it looks consistent
                    progressBar.done();
                    throw e;
                } else {
                    errors.add(e);
                }
            }
        }

        handleChildren(errors, rootNode);

        return errors;
    }

    /**
     * Recursively build the file system tree for the children nodes
     * @param errors
     * @param rootNode
     */
    private void handleChildren(ArrayList<Exception> errors, TreeNode rootNode) {

        if (rootNode.children() != null && !rootNode.children().isEmpty()) {

            List<PushTreeNodeTask> tasks = new ArrayList<>();

            for (TreeNode child : rootNode.children()) {

                var task = new PushTreeNodeTask(
                        PushTraverseParams.builder()
                        .from(params).rootNode(child).build()
                );

                task.fork();
                tasks.add(task);
            }

            for (var task : tasks) {
                var taskErrors = task.join();
                errors.addAll(taskErrors);
            }
        }
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

    private void doPushFolder(FolderView folder,  PushContext pushContext) {
        var isSite = Objects.equals(folder.path(), "/") && Objects.equals(folder.name(), "/");
        try {
            if (isSite) {
                final String status = params.localPaths().status();
                // And we need to create the non-existing site
                final Optional<SiteView> optional = pushContext.execPush(folder.host(),
                        () -> Optional.of(
                                pusher.pushSite(folder.host(), status))
                );
                if (optional.isPresent()){
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
                if (!params.isRetry() || !alreadyExist) {
                    logger.error(message, e);
                    throw new SiteCreationException(message, e);
                }
            } else {
                // Using the exception to check if the folder already exist
                var alreadyExist = checkIfAssetOrFolderAlreadyExist(e);

                // If we are trying to create a folder that already exist we could ignore the error on retries
                if (!params.isRetry() || !alreadyExist) {
                    logger.error(message, e);
                    throw new TraversalTaskException(message, e);
                }
            }
        } finally {
            // Folder processed, updating the progress bar
            progressBar.incrementStep();
        }
    }

    private void doDeleteFolder(FolderView folder, PushContext pushContext) {
        try {
            final Optional<Boolean> delete = pushContext.execDelete(String.format("%s/%s",folder.host(),folder.path()),
                    () -> Optional.of(pusher.deleteFolder(folder.host(), folder.path())));
            if (delete.isPresent()) {
                logger.debug(String.format("Folder [%s] deleted", folder.path()));
            } else {
                logger.debug(String.format("Folder [%s] already deleted", folder.path()));
            }

        } catch (Exception e) {

            // If we are trying to delete a folder that does not exist anymore we could ignore the error on retries
            if (!params.isRetry() || !(e instanceof NotFoundException)) {

                var message = String.format("Error deleting folder [%s]", folder.path());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

        } finally {
            // Folder processed, updating the progress bar
            this.progressBar.incrementStep();
        }
    }

    /**
     * Processes the asset associated with the specified AssetView.
     *
     * @param folder the folder containing the asset
     * @param asset  the asset to process
     */
    private void processAsset(final FolderView folder, final AssetView asset, final PushContext pushContext) {
        if (isMarkedForDelete(asset)) {
            doDeleteAsset(folder, asset, pushContext);
        } else if (isMarkedForPush(asset)) {
            doPushAsset(folder, asset, pushContext);
        }
    }

    private void doPushAsset(FolderView folder, AssetView asset, PushContext pushContext) {
        try {
            final String pushAssetKey = String.format("%s/%s/%s/%s/%s", params.localPaths().status(),
                    params.localPaths().language(), folder.host(), folder.path(), asset.name());
            final Optional<AssetView> optional = pushContext.execPush(
                    pushAssetKey,
                    () -> {
                        // Pushing the asset (and creating the folder if needed
                        final AssetView assetView = pusher.push(params.workspacePath(),
                                params.localPaths().status(),
                                params.localPaths().language(),
                                params.localPaths().site(),
                                folder.path(), asset.name()
                        );
                        return Optional.of(assetView);
                    });

            if(optional.isPresent()){
               logger.debug(String.format("Asset [%s%s] pushed", folder.path(), asset.name()));
            } else {
                logger.debug(String.format("Asset [%s%s] already pushed", folder.path(), asset.name()));
            }
        } catch (Exception e) {

            // Using the exception to check if the asset already exist
            var alreadyExist = checkIfAssetOrFolderAlreadyExist(e);

            // If we are trying to push an asset that already exist we could ignore the error on retries
            if (!params.isRetry() || !alreadyExist) {
                var message = String.format("Error pushing asset [%s%s]", folder.path(), asset.name());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

        } finally {
            // Asset processed, updating the progress bar
            this.progressBar.incrementStep();
        }
    }

    private void doDeleteAsset(final FolderView folder, final AssetView asset, final PushContext pushContext) {
        try {
            // Check if we already deleted the folder
            if (isMarkedForDelete(folder)) {
                // Folder already deleted, we don't need to delete the asset
                logger.debug(String.format("Folder [%s] already deleted, ignoring deletion of [%s] asset",
                        folder.path(), asset.name()));
            } else {
                //TODO: Why is it we're only archiving the asset and not deleting it?
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
                    logger.debug(String.format("Asset [%s%s] archived", folder.path(), asset.name()));
                } else {
                    logger.debug(String.format("Asset [%s%s] already archived", folder.path(), asset.name()));
                }
            }
        } catch (Exception e) {

            // If we are trying to delete an asset that does not exist anymore we could ignore the error on retries
            if (!params.isRetry() || !(e instanceof NotFoundException)) {

                var message = String.format("Error deleting asset [%s%s]", folder.path(), asset.name());
                logger.error(message, e);
                throw new TraversalTaskException(message, e);
            }

        } finally {
            // Asset processed, updating the progress bar
            this.progressBar.incrementStep();
        }
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
