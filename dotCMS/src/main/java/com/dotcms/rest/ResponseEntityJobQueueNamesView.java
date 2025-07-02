package com.dotcms.rest;

import java.util.Set;

/**
 * Entity View for job queue names responses.
 * Contains set of available job queue names.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityJobQueueNamesView extends ResponseEntityView<Set<String>> {
    public ResponseEntityJobQueueNamesView(final Set<String> entity) {
        super(entity);
    }
}