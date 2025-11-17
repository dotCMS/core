package com.dotcms.api.client.files.traversal.task;

import com.dotcms.cli.common.DotCliIgnore;
import com.dotcms.model.annotation.ValueType;
import java.io.File;
import java.io.Serializable;
import java.util.Optional;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * This interface represents the parameters for a {@link LocalFolderTraversalTask}.
 */
@ValueType
@Value.Immutable
public interface AbstractLocalFolderTraversalTaskParams extends Serializable {

    // START-NOSCAN

    /**
     * Returns a boolean indicating whether the site exists.
     *
     * @return true if the site exists, false otherwise. Default is false.
     */
    @Default
    default boolean siteExists() {
        return false;
    }

    /**
     * Returns the source path for the traversal process.
     *
     * @return String representing the source path.
     */
    String sourcePath();

    /**
     * Returns the workspace file.
     *
     * @return File representing the workspace.
     */
    File workspace();

    /**
     * Returns a boolean flag indicating whether to remove assets.
     *
     * @return true if the assets should be removed, false otherwise.
     */
    boolean removeAssets();

    /**
     * Returns a boolean flag indicating whether to remove folders.
     *
     * @return true if the folders should be removed, false otherwise.
     */
    boolean removeFolders();

    /**
     * Returns a boolean flag indicating whether to ignore empty folders.
     *
     * @return true if the empty folders should be ignored, false otherwise.
     */
    boolean ignoreEmptyFolders();

    /**
     * Returns a boolean flag indicating whether to fail fast.
     *
     * @return true if the operation should fail fast, false otherwise.
     */
    boolean failFast();

    /**
     * Returns the DotCliIgnore instance for filtering files and directories based on .dotcliignore patterns.
     *
     * @return Optional containing the DotCliIgnore instance, or empty if not set.
     */
    Optional<DotCliIgnore> dotCliIgnore();

    // END-NOSCAN

}