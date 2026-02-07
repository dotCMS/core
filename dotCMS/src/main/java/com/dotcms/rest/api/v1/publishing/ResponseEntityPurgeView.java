package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Response view for purge operation acknowledgment.
 * Used for Swagger documentation of the purge endpoint response.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
public class ResponseEntityPurgeView extends ResponseEntityView<Map<String, Object>> {

    /**
     * Creates a new ResponseEntityPurgeView with the given entity.
     *
     * @param entity Map containing purge operation details (message, statusesRequested)
     */
    public ResponseEntityPurgeView(final Map<String, Object> entity) {
        super(entity);
    }
}
