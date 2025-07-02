package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for logout responses.
 * Contains a logout confirmation message and optional redirect URL information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLogoutView extends ResponseEntityView<String> {
    public ResponseEntityLogoutView(final String entity) {
        super(entity);
    }
}