package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.traversal.Filter;
import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.asset.FolderView;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * This interface represents the parameters for a {@link RemoteFolderTraversalTask}.
 */
@ValueType
@Value.Immutable
public interface AbstractRemoteFolderTraversalTaskParams extends Serializable {

    // START-NOSCAN

    /**
     * Retrieves the {@link Filter} instance used to include or exclude folders and assets.
     *
     * @return The filter instance.
     */
    Filter filter();

    /**
     * Retrieves the name of the site containing the folder to traverse.
     *
     * @return The site name.
     */
    String siteName();

    /**
     * Retrieves the folder to traverse.
     *
     * @return The {@link FolderView} representing the folder to traverse.
     */
    FolderView folder();

    /**
     * Indicates whether this task is for the root folder.
     *
     * @return true if this task is for the root folder, false otherwise.
     */
    boolean isRoot();

    /**
     * Retrieves the maximum depth to traverse the directory tree.
     *
     * @return The maximum depth.
     */
    int depth();

    /**
     * This method is used to determine if the traversal task should fail fast or continue on
     * error.
     *
     * @return true if the traversal task should fail fast, false otherwise.
     */
    boolean failFast();

    // END-NOSCAN

}
