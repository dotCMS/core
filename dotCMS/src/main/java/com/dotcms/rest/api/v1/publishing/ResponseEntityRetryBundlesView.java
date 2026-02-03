package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response entity view wrapper for bundle retry results.
 * Provides proper type information for OpenAPI/Swagger documentation.
 *
 * @author hassandotcms
 * @since Feb 2026
 */
public class ResponseEntityRetryBundlesView extends ResponseEntityView<List<RetryBundleResultView>> {

    /**
     * Creates a response with retry results list.
     *
     * @param entity The list of retry results
     */
    public ResponseEntityRetryBundlesView(final List<RetryBundleResultView> entity) {
        super(entity);
    }
}
