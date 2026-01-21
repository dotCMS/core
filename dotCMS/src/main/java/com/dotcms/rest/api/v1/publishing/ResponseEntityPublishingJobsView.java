package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.Pagination;
import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response entity view wrapper for paginated publishing jobs list.
 * Provides proper type information for OpenAPI/Swagger documentation.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
public class ResponseEntityPublishingJobsView extends ResponseEntityView<List<PublishingJobView>> {

    /**
     * Creates a response with publishing jobs list only.
     *
     * @param entity The list of publishing jobs
     */
    public ResponseEntityPublishingJobsView(final List<PublishingJobView> entity) {
        super(entity);
    }

    /**
     * Creates a response with publishing jobs list and pagination info.
     *
     * @param entity     The list of publishing jobs
     * @param pagination The pagination metadata
     */
    public ResponseEntityPublishingJobsView(final List<PublishingJobView> entity,
                                            final Pagination pagination) {
        super(entity, pagination);
    }
}
