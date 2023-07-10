package com.dotcms.api.client.files;

import com.dotcms.api.client.files.traversal.LocalTraversalService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import io.quarkus.arc.DefaultBean;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.dotcms.common.AssetsUtils.ParseLocalPath;

@DefaultBean
@Dependent
public class PushServiceImpl implements PushService {

    @Inject
    Logger logger;

    @Inject
    LocalTraversalService traversalService;

    /**
     * Traverses the local folders and retrieves the hierarchical tree representation of their contents with the push
     * related information for each file and folder.
     * Each folder is represented as a pair of its local path structure and the corresponding tree node.
     *
     * @param output the output option mixin
     * @param source the source path to traverse
     * @return a list of pairs, where each pair represents a folder's local path structure and its corresponding tree node
     * @throws IllegalArgumentException if the source path or workspace path does not exist, or if the source path is
     *                                  outside the workspace
     */
    @ActivateRequestContext
    @Override
    public List<Pair<AssetsUtils.LocalPathStructure, TreeNode>> traverseLocalFolders(OutputOptionMixin output, String source) {

        // TODO: Remove this hardcoded path
        var workspacePath = "/Users/jonathan/Downloads/CLI";

        var workspaceFile = new File(workspacePath);
        var sourceFile = new File(source);

        // Validating the source is a valid path
        validateSource(workspaceFile, sourceFile);

        var traversalResult = new ArrayList<Pair<AssetsUtils.LocalPathStructure, TreeNode>>();

        // Parsing the source in order to get the root or roots for the traversal
        var roots = AssetsUtils.ParseRootPaths(workspaceFile, sourceFile);
        for (var root : roots) {

            final var localPathStructure = ParseLocalPath(workspaceFile, new File(root));
            var treeNode = traversalService.traverseLocalFolder(output, workspacePath, root);

            traversalResult.add(
                    Pair.of(localPathStructure, treeNode)
            );
        }

        return traversalResult;
    }

    @ActivateRequestContext
    @Override
    public void processTreeNodes(OutputOptionMixin output, List<Pair<AssetsUtils.LocalPathStructure, TreeNode>> treeNodes) {

        // TODO: Remove this hardcoded path
        var workspacePath = "/Users/jonathan/Downloads/CLI";

    }

    /**
     * Validates the source path and workspace path.
     *
     * @param workspaceFile the workspace file
     * @param sourceFile    the source file
     * @throws IllegalArgumentException if the source path does not exist, the workspace path does not exist,
     *                                  or the source path is outside the workspace
     */
    private void validateSource(File workspaceFile, File sourceFile) {

        if (!sourceFile.exists()) {
            throw new IllegalArgumentException(String.format("Source path [%s] does not exist", sourceFile.getAbsolutePath()));
        }

        if (!workspaceFile.exists()) {
            throw new IllegalArgumentException(String.format("Workspace path [%s] does not exist", workspaceFile.getAbsolutePath()));
        }

        // Validating the source is within the workspace
        var workspaceFilePath = workspaceFile.toPath();
        var sourceFilePath = sourceFile.toPath();

        var workspaceCount = workspaceFilePath.getNameCount();
        var sourceCount = sourceFilePath.getNameCount();

        if (sourceCount < workspaceCount) {
            throw new IllegalArgumentException("Source path cannot be outside of the workspace");
        }
    }

}
