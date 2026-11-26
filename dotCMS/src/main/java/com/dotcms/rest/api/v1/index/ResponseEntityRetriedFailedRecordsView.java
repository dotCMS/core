package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper of {@code POST /api/v1/index/failed/retry}, so OpenAPI documents the entity.
 */
public class ResponseEntityRetriedFailedRecordsView
        extends ResponseEntityView<RetriedFailedRecordsView> {

    /**
     * Wraps the retry result.
     *
     * @param entity what the retry did
     */
    public ResponseEntityRetriedFailedRecordsView(final RetriedFailedRecordsView entity) {
        super(entity);
    }
}
