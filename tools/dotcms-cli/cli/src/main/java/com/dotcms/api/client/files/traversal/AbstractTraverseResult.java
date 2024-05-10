package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.common.LocalPathStructure;
import com.dotcms.model.annotation.ValueType;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Simple class to glue together TreeNode, LocalPath, and any exceptions encountered during the
 * traverse process
 */
@ValueType
@Value.Immutable
public interface AbstractTraverseResult {

    List<Exception> exceptions();
    Optional<TreeNode> treeNode();

    LocalPathStructure localPaths();

}
