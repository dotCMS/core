package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.asset.AssetVersionsView;
import io.quarkus.arc.DefaultBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

/**
 * Implementation of the PullFilesService interface for pulling files from the server.
 */
@DefaultBean
@Dependent
public class PullServiceImpl implements PullService {

    @Inject
    protected PullFolder pullFolder;

    @Inject
    protected PullFile pullFile;

    /**
     * Pulls files from the server and saves them to the specified destination.
     *
     * @param output               the output option mixin for printing progress
     * @param tree                 the tree node representing the file structure
     * @param destination          the destination path to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param failFast             true to fail fast, false to continue on error
     */
    @ActivateRequestContext
    @Override
    public void pullTree(OutputOptionMixin output, final TreeNode tree, final String destination,
                         final boolean overwrite, final boolean generateEmptyFolders, final boolean failFast) {
        pullFolder.pull(output, tree, destination, overwrite, generateEmptyFolders, failFast);
    }

    /**
     * Pulls a file from the server and saves it to the specified destination.
     *
     * @param output      the output option mixin for printing progress
     * @param assetInfo   the remote asset information
     * @param source      the remote source path for the file to pull
     * @param destination the destination path to save the pulled files
     * @param overwrite   true to overwrite existing files, false otherwise
     * @param failFast    true to fail fast, false to continue on error
     */
    @ActivateRequestContext
    @Override
    public void pullFile(OutputOptionMixin output, final AssetVersionsView assetInfo, final String source,
                         final String destination, final boolean overwrite, final boolean failFast) {
        pullFile.pull(output, assetInfo, source, destination, overwrite, failFast);
    }

}
