package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response view for purge operation acknowledgment.
 * Used for Swagger documentation of the purge endpoint response.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
public class ResponseEntityPurgeView extends ResponseEntityView<PurgeResultView> {

    /**
     * Creates a new ResponseEntityPurgeView with the given entity.
     *
     * @param entity PurgeResultView containing purge operation details
     */
    public ResponseEntityPurgeView(final PurgeResultView entity) {
        super(entity);
    }
}
