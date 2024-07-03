package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.command.PushContext;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.LocalPathStructure;
import com.dotcms.model.annotation.ValueType;
import java.io.Serializable;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * Just a class to compile all the params shared by various Traverse APIs
 */
@ValueType
@Value.Immutable
public interface AbstractPushTraverseParams extends Serializable {

    String workspacePath();
    LocalPathStructure localPaths();
    TreeNode rootNode();
    @Default
    default boolean failFast() {return  true;}
    @Default
    default boolean isRetry(){return  false;}
    @Default
    default int maxRetryAttempts(){return 0;}

    @Nullable
    ConsoleProgressBar progressBar();

    PushContext pushContext();

}
