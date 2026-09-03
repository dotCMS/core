package com.dotcms.rest;

import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshSubmitResponse;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected
 * {@link BulkRefreshSubmitResponse} as the entity in the response.
 */
public class ResponseEntityBulkRefreshSubmitView extends ResponseEntityView<BulkRefreshSubmitResponse> {
    public ResponseEntityBulkRefreshSubmitView(final BulkRefreshSubmitResponse entity) {
        super(entity);
    }
}
