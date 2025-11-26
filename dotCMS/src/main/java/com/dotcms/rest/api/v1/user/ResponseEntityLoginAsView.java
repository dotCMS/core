package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object for login-as operations
 * that return authentication status information
 * @author Steve Bolton
 */
public class ResponseEntityLoginAsView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityLoginAsView(final Map<String, Object> entity) {
        super(entity);
    }
}