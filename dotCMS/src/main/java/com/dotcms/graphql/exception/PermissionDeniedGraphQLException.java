package com.dotcms.graphql.exception;

import graphql.GraphqlErrorException;

public class PermissionDeniedGraphQLException extends GraphqlErrorException {

    public PermissionDeniedGraphQLException(String message) {
        super(new Builder().message(message));
    }

    public PermissionDeniedGraphQLException() {
        super(new Builder().message("Permission denied: You do not have permission to access this resource."));
    }

}


