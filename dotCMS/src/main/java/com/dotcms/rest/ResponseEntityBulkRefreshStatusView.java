package com.dotcms.rest;

import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshStatusView;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected
 * {@link BulkRefreshStatusView} as the entity in the response.
 */
public class ResponseEntityBulkRefreshStatusView extends ResponseEntityView<BulkRefreshStatusView> {
    public ResponseEntityBulkRefreshStatusView(final BulkRefreshStatusView entity) {
        super(entity);
    }
}
