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

    List<Exception> exceptions();

    Optional<TreeNode> tree();

    Optional<AssetVersionsView> asset();

}
