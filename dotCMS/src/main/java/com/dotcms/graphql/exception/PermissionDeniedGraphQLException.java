package com.dotcms.graphql.exception;

import javax.ws.rs.core.Response;

public class PermissionDeniedGraphQLException extends CustomGraphQLException {

    public PermissionDeniedGraphQLException(String errorMessage) {
        super(Response.Status.FORBIDDEN.getStatusCode(), errorMessage);
    }

    public PermissionDeniedGraphQLException() {
        super(Response.Status.FORBIDDEN.getStatusCode(), "Permission denied: You do not have permission to access this resource.");
    }
}


