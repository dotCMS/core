package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.model.annotation.ValueType;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * This interface represents the parameters for a {@link PullTreeNodeTask}.
 */
@ValueType
@Value.Immutable
public interface AbstractPullTreeNodeTaskParams extends Serializable {

    // START-NOSCAN

    /**
     * Returns the root node of the file system tree as a {@link TreeNode}.
     *
     * @return the root {@link TreeNode}
     */
    TreeNode rootNode();

    /**
     * Returns the destination path to save the pulled files.
     *
     * @return the destination path as a String
     */
    String destination();

    /**
     * Determines if the operation should overwrite existing files.
     *
     * @return true if the operation should overwrite, false otherwise
     */
    boolean overwrite();

    /**
     * Determines if empty folders should be generated.
     *
     * @return true if empty folders should be generated, false otherwise
     */
    boolean generateEmptyFolders();

    /**
     * Determines if the operation should fail fast or continue on error.
     *
     * @return true if the operation should fail quickly on error, false if it should continue
     */
    boolean failFast();

    /**
     * Returns the language of the assets.
     *
     * @return the language of the assets as a String
     */
    String language();

    /**
     * Returns the {@link ConsoleProgressBar} for tracking the pull progress.
     *
     * @return the {@link ConsoleProgressBar}
     */
    ConsoleProgressBar progressBar();

    // END-NOSCAN

}
