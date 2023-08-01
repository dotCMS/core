package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.client.files.traversal.exception.SiteCreationException;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import org.jboss.logging.Logger;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RecursiveTask;

/**
 * Represents a task that pushes the contents of a tree node to a remote server.
 */
public class PushTreeNodeTask extends RecursiveTask<List<Exception>> {

    private final Pusher pusher;

    private final String workspacePath;

    private final AssetsUtils.LocalPathStructure localPathStructure;

    private final TreeNode rootNode;

    private final boolean failFast;
    private final boolean isRetry;

    private final ConsoleProgressBar progressBar;

    private final Logger logger;

    /**
     * Constructs a new PushTreeNodeTask with the specified parameters.
     *
     * @param workspacePath      the local workspace path
     * @param localPathStructure the local path structure of the folder being pushed
     * @param rootNode           the tree node representing the folder and its contents with all the push
     *                           information for each file and folder
     * @param failFast           true to fail fast, false to continue on error
     * @param isRetry            true if this is a retry, false otherwise
     * @param logger             the logger for logging debug information
     * @param pusher             the pusher for pushing assets and folders
     * @param progressBar        the console progress bar to track and display the push progress
     */
    public PushTreeNodeTask(String workspacePath,
                            AssetsUtils.LocalPathStructure localPathStructure,
                            TreeNode rootNode,
                            final boolean failFast,
                            final boolean isRetry,
                            final Logger logger,
                            final Pusher pusher,
                            final ConsoleProgressBar progressBar) {

        this.workspacePath = workspacePath;
        this.localPathStructure = localPathStructure;
        this.rootNode = rootNode;
        this.failFast = failFast;
        this.isRetry = isRetry;
        this.pusher = pusher;
        this.logger = logger;
        this.progressBar = progressBar;
    }

    @Override
    protected List<Exception> compute() {

        var errors = new ArrayList<Exception>();

        // Handle the folder for the current node
        try {
            processFolder(rootNode.folder());
        } catch (Exception e) {

            if (failFast) {
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
                processAsset(rootNode.folder(), asset);
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

            List<PushTreeNodeTask> tasks = new ArrayList<>();

            for (TreeNode child : rootNode.children()) {
                var task = new PushTreeNodeTask(
                        this.workspacePath,
                        this.localPathStructure,
                        child,
                        this.failFast,
                        this.isRetry,
                        this.logger,
                        this.pusher,
                        this.progressBar
                );
                task.fork();
                tasks.add(task);
            }

            for (var task : tasks) {
                var taskErrors = task.join();
                errors.addAll(taskErrors);
            }
        }

        return errors;
    }

    /**
     * Processes the folder associated with the specified FolderView.
     *
     * @param folder the folder to process
     */
    private void processFolder(final FolderView folder) {

        if (folder.markForDelete().isPresent() || folder.markForPush().isPresent()) {

            if (folder.markForDelete().isPresent() && folder.markForDelete().get()) {// Delete

                try {

                    pusher.deleteFolder(folder.host(), folder.path());
                    logger.debug(String.format("Folder [%s] deleted", folder.path()));
                } catch (Exception e) {

                    // If we are trying to delete a folder that does not exist anymore we could ignore the error on retries
                    if (!this.isRetry || !(e instanceof NotFoundException)) {

                        var message = String.format("Error deleting folder [%s] --- %s",
                                folder.path(), e.getMessage());
                        logger.debug(message, e);
                        throw new RuntimeException(message, e);
                    }

                } finally {
                    // Folder processed, updating the progress bar
                    this.progressBar.incrementStep();
                }
            } else if (folder.markForPush().isPresent() && folder.markForPush().get()) {// Push

                var isSite = Objects.equals(folder.path(), "/") && Objects.equals(folder.name(), "/");

                try {

                    if (isSite) {

                        // And we need to create the non-existing site
                        pusher.pushSite(folder.host(), this.localPathStructure.status());
                        logger.debug(String.format("Site [%s] created", folder.host()));
                    } else {

                        // Creating the non-existing folder
                        pusher.createFolder(folder.host(), folder.path());
                        logger.debug(String.format("Folder [%s] created", folder.path()));
                    }
                } catch (Exception e) {
                    var message = String.format("Error creating %s [%s] --- %s",
                            isSite ? "site" : "folder",
                            isSite ? folder.host() : folder.path(),
                            e.getMessage());
                    logger.debug(message, e);
                    if (isSite) {

                        // Using the exception to check if the site already exist
                        var alreadyExist = checkIfSiteAlreadyExist(e);

                        // If we are trying to create a site that already exist we could ignore the error on retries
                        if (!this.isRetry || !alreadyExist) {
                            throw new SiteCreationException(message, e);
                        }
                    } else {

                        // Using the exception to check if the folder already exist
                        var alreadyExist = checkIfAssetOrFolderAlreadyExist(e);

                        // If we are trying to create a folder that already exist we could ignore the error on retries
                        if (!this.isRetry || !alreadyExist) {
                            throw new RuntimeException(message, e);
                        }
                    }
                } finally {
                    // Folder processed, updating the progress bar
                    this.progressBar.incrementStep();
                }
            }
        }
    }

    /**
     * Processes the asset associated with the specified AssetView.
     *
     * @param folder the folder containing the asset
     * @param asset  the asset to process
     */
    private void processAsset(final FolderView folder, final AssetView asset) {

        if (asset.markForDelete().isPresent() || asset.markForPush().isPresent()) {

            if (asset.markForDelete().isPresent() && asset.markForDelete().get()) {

                try {

                    // Check if we already deleted the folder
                    if ((folder.markForDelete().isPresent() && folder.markForDelete().get())) {

                        // Folder already deleted, we don't need to delete the asset
                        logger.debug(String.format("Folder [%s] already deleted, ignoring deletion of [%s] asset",
                                folder.path(), asset.name()));
                    } else {

                        pusher.archive(folder.host(), folder.path(), asset.name());
                        logger.debug(String.format("Asset [%s] deleted", asset.name()));
                    }
                } catch (Exception e) {

                    // If we are trying to delete an asset that does not exist anymore we could ignore the error on retries
                    if (!this.isRetry || !(e instanceof NotFoundException)) {

                        var message = String.format("Error deleting asset [%s%s] --- %s",
                                folder.path(), asset.name(), e.getMessage());
                        logger.debug(message, e);
                        throw new RuntimeException(message, e);
                    }

                } finally {
                    // Asset processed, updating the progress bar
                    this.progressBar.incrementStep();
                }

            } else if (asset.markForPush().isPresent() && asset.markForPush().get()) {

                try {

                    pusher.push(this.workspacePath, this.localPathStructure.status(), localPathStructure.language(),
                            localPathStructure.site(), folder.path(), asset.name());
                    logger.debug(String.format("Asset [%s%s] pushed", folder.path(), asset.name()));
                } catch (Exception e) {

                    // Using the exception to check if the asset already exist
                    var alreadyExist = checkIfAssetOrFolderAlreadyExist(e);

                    // If we are trying to push an asset that already exist we could ignore the error on retries
                    if (!this.isRetry || !alreadyExist) {
                        var message = String.format("Error pushing asset [%s%s] --- %s",
                                folder.path(), asset.name(), e.getMessage());
                        logger.debug(message, e);
                        throw new RuntimeException(message, e);
                    }

                } finally {
                    // Asset processed, updating the progress bar
                    this.progressBar.incrementStep();
                }

            }
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
