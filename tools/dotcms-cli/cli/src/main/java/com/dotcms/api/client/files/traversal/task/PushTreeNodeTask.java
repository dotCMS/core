package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.AssetsUtils;
import org.jboss.logging.Logger;

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

    }
}
