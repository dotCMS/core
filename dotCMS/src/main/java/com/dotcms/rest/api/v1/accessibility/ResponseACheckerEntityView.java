package com.dotcms.rest.api.v1.accessibility;

import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.rest.ResponseEntityView;

/**
 * This Entity View is used to return the response of the Accessibility Checker service indicating
 * what issues were found in the specified content.
 *
 * @author Jose Castro
 * @since Sep 5th, 2024
 */
public class ResponseACheckerEntityView extends ResponseEntityView<ACheckerResponse> {

    public ResponseACheckerEntityView(final ACheckerResponse entity) {
        super(entity);
    }

}
