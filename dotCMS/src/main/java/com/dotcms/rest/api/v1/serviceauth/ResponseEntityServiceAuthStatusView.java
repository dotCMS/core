package com.dotcms.rest.api.v1.serviceauth;

import com.dotcms.rest.ResponseEntityView;

/**
 * ResponseEntityView wrapper for ServiceAuthStatus.
 * Used for Swagger schema documentation.
 *
 * @author dotCMS
 */
public class ResponseEntityServiceAuthStatusView extends ResponseEntityView<ServiceAuthStatus> {

    public ResponseEntityServiceAuthStatusView(final ServiceAuthStatus entity) {
        super(entity);
    }
}
