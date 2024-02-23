package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.model.annotation.ValueType;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
public interface AbstractTraverseTaskResult {

    List<Exception> exceptions();

    //This should be an Optional<TreeNode>
    Optional<TreeNode> treeNode();


}
