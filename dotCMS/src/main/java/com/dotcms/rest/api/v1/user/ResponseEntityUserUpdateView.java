package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object for user update operations
 * that return user information along with authentication status
 * @author Steve Bolton
 */
public class ResponseEntityUserUpdateView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityUserUpdateView(final Map<String, Object> entity) {
        super(entity);
    }
}