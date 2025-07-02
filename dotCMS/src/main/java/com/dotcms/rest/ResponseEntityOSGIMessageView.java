package com.dotcms.rest;

/**
 * Entity View for OSGI operation message responses.
 * Contains simple string messages for OSGI bundle operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityOSGIMessageView extends ResponseEntityView<String> {
    public ResponseEntityOSGIMessageView(final String entity) {
        super(entity);
    }
}