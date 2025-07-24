package com.dotcms.rest;

import com.dotcms.util.pagination.Paginator;
import com.dotcms.util.pagination.PaginatorOrdered;

/**
 * Entity View for relationship pagination responses.
 * Wraps paginated results from relationship queries.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityRelationshipPaginationView extends ResponseEntityView<PaginatorOrdered> {
    public ResponseEntityRelationshipPaginationView(final PaginatorOrdered entity) {
        super(entity);
    }
}