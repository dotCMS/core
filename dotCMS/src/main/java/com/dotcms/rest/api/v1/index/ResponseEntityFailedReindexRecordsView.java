package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper of {@code GET /api/v1/index/failed}, so OpenAPI documents the item type.
 */
public class ResponseEntityFailedReindexRecordsView
        extends ResponseEntityView<FailedReindexRecordsView> {

    /**
     * Wraps the listing.
     *
     * @param entity the failed reindex records and the limits they are measured against
     */
    public ResponseEntityFailedReindexRecordsView(final FailedReindexRecordsView entity) {
        super(entity);
    }
}
