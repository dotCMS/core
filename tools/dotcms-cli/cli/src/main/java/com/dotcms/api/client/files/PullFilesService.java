package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.OutputOptionMixin;

/**
 * Service interface for pulling files from the server.
 */
public interface PullFilesService {

    /**
     * Pulls files from the server and saves them to the specified destination.
     *
     * @param output      the output option mixin for printing progress
     * @param tree        the tree node representing the file structure
     * @param destination the destination path to save the pulled files
     * @param overwrite   true to overwrite existing files, false otherwise
     */
    void pull(OutputOptionMixin output, final TreeNode tree, String destination, boolean overwrite);
}
