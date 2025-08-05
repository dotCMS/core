package com.dotcms.rest;

import com.dotcms.rest.api.BulkResultView;

/**
 * Entity View for bulk operation responses.
 * Contains results from bulk operations including success count and failed items.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityBulkResultView extends ResponseEntityView<BulkResultView> {
    public ResponseEntityBulkResultView(final BulkResultView entity) {
        super(entity);
    }
}