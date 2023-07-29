package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.asset.AssetVersionsView;

/**
 * Service interface for pulling files from the server.
 */
public interface PullService {

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
    void pullTree(OutputOptionMixin output, TreeNode tree, String destination, boolean overwrite,
                  boolean generateEmptyFolders, final boolean failFast);

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
    void pullFile(OutputOptionMixin output, AssetVersionsView assetInfo, String source, String destination,
                  boolean overwrite, final boolean failFast);

}
