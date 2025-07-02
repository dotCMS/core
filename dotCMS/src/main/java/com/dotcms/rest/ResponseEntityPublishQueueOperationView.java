package com.dotcms.rest;

/**
 * Entity View for publish queue operation responses.
 * Contains operation confirmation messages for publish queue actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPublishQueueOperationView extends ResponseEntityView<String> {
    public ResponseEntityPublishQueueOperationView(final String entity) {
        super(entity);
    }
}