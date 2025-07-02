package com.dotcms.rest;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected String message and related
 * Used for Bundle resource endpoints that return status messages about bundle operations
 * @author Steve Bolton
 */
public class ResponseEntityBundleStatusView extends ResponseEntityView<String> {
    public ResponseEntityBundleStatusView(final String entity) {
        super(entity);
    }
}