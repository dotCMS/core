package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RecursiveAction;

/**
 * Represents a task that pushes the contents of a tree node to a remote server.
 */
public class PushTreeNodeTask extends RecursiveAction {

    private final Pusher pusher;

    private final String workspacePath;

    private final AssetsUtils.LocalPathStructure localPathStructure;

    private final TreeNode rootNode;

    private final ConsoleProgressBar progressBar;

    private final Logger logger;

    /**
     * Constructs a new PushTreeNodeTask with the specified parameters.
     *
     * @param workspacePath      the local workspace path
     * @param localPathStructure the local path structure of the folder being pushed
     * @param rootNode           the tree node representing the folder and its contents with all the push
     *                           information for each file and folder
     * @param logger             the logger for logging debug information
     * @param pusher             the pusher for pushing assets and folders
     * @param progressBar        the console progress bar to track and display the push progress
     */
    public PushTreeNodeTask(String workspacePath,
                            AssetsUtils.LocalPathStructure localPathStructure,
                            TreeNode rootNode,
                            final Logger logger,
                            final Pusher pusher,
                            final ConsoleProgressBar progressBar) {

        this.workspacePath = workspacePath;
        this.localPathStructure = localPathStructure;
        this.rootNode = rootNode;
        this.pusher = pusher;
        this.logger = logger;
        this.progressBar = progressBar;
    }

    @Override
    protected void compute() {

        // Handle the folder for the current node
        processFolder(rootNode.folder());

        // Handle assets for the current node
        for (AssetView asset : rootNode.assets()) {
            processAsset(rootNode.folder(), asset);
        }

        // Recursively build the file system tree for the children nodes
        if (rootNode.children() != null && !rootNode.children().isEmpty()) {

            List<PushTreeNodeTask> tasks = new ArrayList<>();

            for (TreeNode child : rootNode.children()) {
                var task = new PushTreeNodeTask(
                        this.workspacePath,
                        this.localPathStructure,
                        child,
                        this.logger,
                        this.pusher,
                        this.progressBar
                );
                task.fork();
                tasks.add(task);
            }

            for (var task : tasks) {
                task.join();
            }
        }

    }

    /**
     * Processes the folder associated with the specified FolderView.
     *
     * @param folder the folder to process
     */
    private void processFolder(final FolderView folder) {

        if (folder.markForDelete().isPresent() || folder.markForPush().isPresent()) {

            if (folder.markForDelete().isPresent() && folder.markForDelete().get()) {// Delete

                pusher.deleteFolder(folder.host(), folder.path());
                logger.debug(String.format("Folder [%s] deleted", folder.path()));

                // Folder processed, updating the progress bar
                this.progressBar.incrementStep();
            } else if (folder.markForPush().isPresent() && folder.markForPush().get()) {// Push

                // This is the site
                if (Objects.equals(folder.path(), "/") && Objects.equals(folder.name(), "/")) {

                    // And we need to create the non-existing site
                    pusher.pushSite(folder.host(), this.localPathStructure.status());
                    logger.debug(String.format("Site [%s] created", folder.host()));
                } else {

                    // Creating the non-existing folder
                    pusher.createFolder(folder.host(), folder.path());
                    logger.debug(String.format("Folder [%s] created", folder.path()));
                }

                // Folder processed, updating the progress bar
                this.progressBar.incrementStep();
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

                // Check if we already deleted the folder
                if ((folder.markForDelete().isPresent() && folder.markForDelete().get())) {

                    // Folder already deleted, we don't need to delete the asset
                    logger.debug(String.format("Folder [%s] already deleted, ignoring deletion of [%s] asset",
                            folder.path(), asset.name()));
                } else {

                    pusher.archive(folder.host(), folder.path(), asset.name());
                    logger.debug(String.format("Asset [%s] deleted", asset.name()));
                }

                // Asset processed, updating the progress bar
                this.progressBar.incrementStep();
            } else if (asset.markForPush().isPresent() && asset.markForPush().get()) {

                pusher.push(this.workspacePath, this.localPathStructure.status(), localPathStructure.language(),
                        localPathStructure.site(), folder.path(), asset.name());
                logger.debug(String.format("Asset [%s%s] pushed", folder.path(), asset.name()));

                // Asset processed, updating the progress bar
                this.progressBar.incrementStep();
            }
        }
    }

}
