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

public class PushTreeNodeTask extends RecursiveAction {

    private final Pusher pusher;

    private final String workspacePath;

    private final AssetsUtils.LocalPathStructure localPathStructure;

    private final TreeNode rootNode;

    private final ConsoleProgressBar progressBar;

    private final Logger logger;

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
                        workspacePath,
                        localPathStructure,
                        child,
                        logger,
                        pusher,
                        progressBar
                );
                task.fork();
                tasks.add(task);
            }

            for (var task : tasks) {
                task.join();
            }
        }

    }

    private void processFolder(final FolderView folder) {

        if (folder.markForDelete().isPresent() || folder.markForPush().isPresent()) {

            if (folder.markForDelete().isPresent() && folder.markForDelete().get()) {

                //pusher.deleteFolder(folder);
                logger.debug(String.format("Folder [%s] deleted", folder.path()));

                // Folder deleted, updating the progress bar
                this.progressBar.incrementStep();
            } else if (folder.markForPush().isPresent() && folder.markForPush().get()) {

                // This is the site
                if (Objects.equals(folder.path(), "/") && Objects.equals(folder.name(), "/")) {

                    // And we need to create it
                    //pusher.createSite(folder);
                    logger.debug(String.format("Site [%s] created", folder.host()));
                }

                //pusher.pushFolder(folder);
                logger.debug(String.format("Folder [%s] created", folder.path()));

                // Folder pushed, updating the progress bar
                this.progressBar.incrementStep();
            }
        }
    }

    private void processAsset(final FolderView folder, final AssetView asset) {

        //TODO: Remove!!!
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //TODO: Remove!!!

        if (asset.markForDelete().isPresent() || asset.markForPush().isPresent()) {

            if (asset.markForDelete().isPresent() && asset.markForDelete().get()) {

                //pusher.deleteAsset(folder, asset);
                logger.debug(String.format("Asset [%s] deleted", asset.name()));

                // Asset deleted, updating the progress bar
                this.progressBar.incrementStep();
            } else if (asset.markForPush().isPresent() && asset.markForPush().get()) {

                // TODO: It is matters to know if it is new or modified?

                //pusher.pushAsset(folder);
                logger.debug(String.format("Asset [%s%s] pushed", folder.path(), asset.name()));

                // Asset pushed, updating the progress bar
                this.progressBar.incrementStep();
            }
        }
    }

}
