package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

/**
 * Base DataFetcher that skips execution when the contentlet is identified as a vanity redirect.
 */
public abstract class RedirectAwareDataFetcher<T> implements DataFetcher<T> {

    @Override
    public T get(DataFetchingEnvironment environment) throws Exception {
        final DotGraphQLContext context = environment.getContext();

        if (context.isVanityRedirect() ) {
            return onRedirect();
        }

        return safeGet(environment, context);
    }

    /**
     * Called when the page is determined to be a redirect.
     * Override this in subclasses to define the default response (e.g. null, empty list, empty map).
     */
    protected abstract T onRedirect();

    /**
     * Called when the page is valid and not a redirect.
     */
    protected abstract T safeGet(DataFetchingEnvironment env, DotGraphQLContext context) throws Exception;
}
