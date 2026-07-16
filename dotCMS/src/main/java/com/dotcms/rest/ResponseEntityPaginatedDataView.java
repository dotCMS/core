package com.dotcms.rest;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected
 * paginated data.
 *
 * @author Jose Castro
 * @since Jul 31st, 2025
 */
public class ResponseEntityPaginatedDataView extends ResponseEntityView<Object> {

    public ResponseEntityPaginatedDataView(final Object entity, final Pagination pagination) {
        super(entity, pagination);
    }

}
