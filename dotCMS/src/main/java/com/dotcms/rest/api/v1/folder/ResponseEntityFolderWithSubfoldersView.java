package com.dotcms.rest.api.v1.folder;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for folder responses with subfolders.
 * Contains folder information including hierarchy and nested subfolder structure.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFolderWithSubfoldersView extends ResponseEntityView<FolderView> {
    public ResponseEntityFolderWithSubfoldersView(final FolderView entity) {
        super(entity);
    }
}