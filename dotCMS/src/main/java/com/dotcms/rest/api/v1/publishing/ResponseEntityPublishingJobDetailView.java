package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity view wrapper for publishing job detail.
 * Provides proper type information for OpenAPI/Swagger documentation.
 *
 * @since Jan 2026
 */
public class ResponseEntityPublishingJobDetailView extends ResponseEntityView<PublishingJobDetailView> {

    /**
     * Creates a response with publishing job detail.
     *
     * @param entity The publishing job detail view
     */
    public ResponseEntityPublishingJobDetailView(final PublishingJobDetailView entity) {
        super(entity);
    }
}
