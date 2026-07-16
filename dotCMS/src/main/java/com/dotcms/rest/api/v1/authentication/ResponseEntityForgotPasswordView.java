package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for forgot password responses.
 * Contains the email address where the password reset link was sent.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityForgotPasswordView extends ResponseEntityView<String> {
    public ResponseEntityForgotPasswordView(final String entity) {
        super(entity);
    }
}