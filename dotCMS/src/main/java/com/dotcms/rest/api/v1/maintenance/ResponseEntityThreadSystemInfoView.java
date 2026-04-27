package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the thread-info endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntityThreadSystemInfoView extends ResponseEntityView<ThreadSystemInfoView> {

    public ResponseEntityThreadSystemInfoView(final ThreadSystemInfoView entity) {
        super(entity);
    }
}
