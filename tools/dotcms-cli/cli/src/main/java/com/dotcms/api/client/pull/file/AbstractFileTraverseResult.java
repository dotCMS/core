package com.dotcms.api.client.pull.file;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.asset.AssetVersionsView;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Simple class to glue together the response of the lookup of files in a dotCMS instance
 */
@ValueType
@Value.Immutable
public interface AbstractFileTraverseResult {

    /**
     * Returns a list of exceptions that can be thrown during the lookup of files
     *
     * @return a list of exceptions
     */
    List<Exception> exceptions();

    /**
     * Returns an Optional<TreeNode> representing the tree structure of traversed remote files.
     *
     * @return an Optional<TreeNode> representing the tree structure, or an empty Optional if no
     * tree structure is available
     */
    Optional<TreeNode> tree();

    /**
     * Returns an Optional<AssetVersionsView> representing a single asset.
     *
     * @return an Optional<AssetVersionsView> representing a single asset, or an empty Optional if
     * no asset is available
     */
    Optional<AssetVersionsView> asset();

}
