package com.dotcms.rest.api.v1.serviceauth;

import com.dotcms.rest.ResponseEntityView;

/**
 * ResponseEntityView wrapper for ServiceTokenValidationResult.
 * Used for Swagger schema documentation.
 *
 * @author dotCMS
 */
public class ResponseEntityServiceTokenValidationView extends ResponseEntityView<ServiceTokenValidationResult> {

    public ResponseEntityServiceTokenValidationView(final ServiceTokenValidationResult entity) {
        super(entity);
    }
}
