package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for password reset responses.
 * Contains the user ID of the user whose password was successfully reset.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPasswordResetView extends ResponseEntityView<String> {
    public ResponseEntityPasswordResetView(final String entity) {
        super(entity);
    }
}