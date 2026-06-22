package com.dotcms.rest.api.v1.folder;

/**
 * REST view of a Folder search result. Used by {@link FolderResource},
 * {@link com.dotmarketing.portlets.workflows.actionlet.MoveContentActionlet}, and related classes.
 *
 * @author Jonathan Sanchez
 * @since Jul 19th, 2021
 */
public record FolderSearchResultView(
        String id,
        String inode,
        String name,
        String path,
        boolean addChildrenAllowed
) {}