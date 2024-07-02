package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.LocalPathStructure;
import com.dotcms.model.annotation.ValueType;
import java.io.Serializable;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * This interface represents the parameters for a {@link PushTreeNodeTask}.
 */
@ValueType
@Value.Immutable
public interface AbstractPushTreeNodeTaskParams extends Serializable {

    // START-NOSCAN

    /**
     * Returns the path to the workspace directory.
     *
     * @return the path to the workspace directory
     */
    String workspacePath();

    /**
     * Returns the local path structure.
     *
     * @return the local path structure
     */
    LocalPathStructure localPaths();

    /**
     * Returns the root node of the file system tree as a {@link TreeNode}.
     *
     * @return the root {@link TreeNode}
     */
    TreeNode rootNode();

    /**
     * Determines if the operation should fail fast or continue on error.
     *
     * @return true if the operation should fail quickly on error, false if it should continue
     */
    @Default
    default boolean failFast() {
        return true;
    }

    /**
     * Determine whether it is a retry or not.
     *
     * @return false by default
     */
    @Default
    default boolean isRetry() {
        return false;
    }

    /**
     * Returns the maximum number of retry attempts for the push operation.
     *
     * @return the maximum number of retry attempts
     */
    @Default
    default int maxRetryAttempts() {
        return 0;
    }

    /**
     * Returns the {@link ConsoleProgressBar} for tracking the push progress.
     *
     * @return the {@link ConsoleProgressBar}
     */
    @Nullable
    ConsoleProgressBar progressBar();

    // END-NOSCAN

}