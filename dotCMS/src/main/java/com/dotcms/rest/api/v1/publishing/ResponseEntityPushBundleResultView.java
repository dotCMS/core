package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for PushBundleResultView.
 * Provides Swagger documentation with precise type information.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
public class ResponseEntityPushBundleResultView extends ResponseEntityView<PushBundleResultView> {

    /**
     * Creates a new response entity wrapping the push bundle result.
     *
     * @param entity The push bundle result view
     */
    public ResponseEntityPushBundleResultView(final PushBundleResultView entity) {
        super(entity);
    }

}
