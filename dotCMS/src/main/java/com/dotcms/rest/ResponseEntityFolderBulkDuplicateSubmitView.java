package com.dotcms.rest;

import com.dotcms.rest.api.v1.asset.bulkduplicate.FolderBulkDuplicateSubmitResponse;

/**
 * Wraps a {@link FolderBulkDuplicateSubmitResponse} as the entity of the {@code 202} answer to
 * {@code POST /v1/assets/folders/_bulkduplicate}, so the endpoint's {@code @Schema} matches what it
 * returns (#37062).
 */
public class ResponseEntityFolderBulkDuplicateSubmitView
        extends ResponseEntityView<FolderBulkDuplicateSubmitResponse> {

    /**
     * @param entity the accepted run's handle
     */
    public ResponseEntityFolderBulkDuplicateSubmitView(
            final FolderBulkDuplicateSubmitResponse entity) {
        super(entity);
    }
}
