package com.dotcms.rest;

import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteSubmitResponse;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected
 * {@link FolderBulkDeleteSubmitResponse} as the entity in the response.
 */
public class ResponseEntityFolderBulkDeleteSubmitView
        extends ResponseEntityView<FolderBulkDeleteSubmitResponse> {
    public ResponseEntityFolderBulkDeleteSubmitView(final FolderBulkDeleteSubmitResponse entity) {
        super(entity);
    }
}
