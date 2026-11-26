package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper of {@code DELETE /api/v1/esindex/failed}, so OpenAPI documents the entity.
 */
public class ResponseEntityClearedFailedRecordsView
        extends ResponseEntityView<ClearedFailedRecordsView> {

    /**
     * Wraps the clear result.
     *
     * @param entity what the clear did
     */
    public ResponseEntityClearedFailedRecordsView(final ClearedFailedRecordsView entity) {
        super(entity);
    }
}
