package com.dotcms.rest.api.v1.folder;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * Entity View for individual folder responses.
 * Contains single folder information including hierarchy and metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFolderView extends ResponseEntityView<Folder> {
    public ResponseEntityFolderView(final Folder entity) {
        super(entity);
    }
}