package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for login form configuration responses.
 * Contains login form data along with additional response metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLoginFormView extends ResponseEntityView<LoginFormResultView> {
    public ResponseEntityLoginFormView(final LoginFormResultView entity) {
        super(entity);
    }
}