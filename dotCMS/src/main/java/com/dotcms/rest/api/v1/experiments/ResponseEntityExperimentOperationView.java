package com.dotcms.rest.api.v1.experiments;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for experiment operation confirmation responses.
 * Contains simple string messages confirming successful experiment operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityExperimentOperationView extends ResponseEntityView<String> {
    public ResponseEntityExperimentOperationView(final String entity) {
        super(entity);
    }
}