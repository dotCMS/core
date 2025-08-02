package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for page operation confirmation responses.
 * Contains simple string messages confirming successful page operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPageOperationView extends ResponseEntityView<String> {
    public ResponseEntityPageOperationView(final String entity) {
        super(entity);
    }
}