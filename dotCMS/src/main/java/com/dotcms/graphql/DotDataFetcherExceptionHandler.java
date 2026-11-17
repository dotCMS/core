package com.dotcms.graphql;

import com.dotcms.graphql.exception.ResourceNotFoundException;
import com.dotmarketing.util.Logger;
import graphql.ExceptionWhileDataFetching;
import graphql.execution.SimpleDataFetcherExceptionHandler;

/**
 * Keeps expected missing-page diagnostics at DEBUG without changing GraphQL error responses.
 */
final class DotDataFetcherExceptionHandler extends SimpleDataFetcherExceptionHandler {

    /**
     * Logs missing pages at DEBUG and leaves all other exception logging to GraphQL.
     *
     * @param error the GraphQL error, including its query path and location
     * @param exception the unwrapped data-fetcher exception
     */
    @Override
    protected void logException(final ExceptionWhileDataFetching error, final Throwable exception) {
        if (exception instanceof ResourceNotFoundException notFound
                && "Page".equals(notFound.getExtensions().get("resourceType"))) {
            Logger.debug(this, error.getMessage(), exception);
        } else {
            super.logException(error, exception);
        }
    }
}
