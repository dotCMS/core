package com.dotcms.graphql.util;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;

/**
 * Utility methods for GraphQL-related logic.
 */
public class GraphQLUtils {

    private GraphQLUtils() {}

    /** Context parameter key for identifying vanity URL redirects. */
    public static final String IS_VANITY_REDIRECT_PARAM = "isVanityRedirect";

    /**
     * Determines whether a page contentlet represents a redirect caused by a vanity URL.
     *
     * @param ctx  the GraphQL context
     * @return true if the context indicates a vanity redirect
     */
    public static boolean isRedirectPage(final DotGraphQLContext ctx) {
        return Boolean.TRUE.equals(ctx.getParam(IS_VANITY_REDIRECT_PARAM));
    }
}
