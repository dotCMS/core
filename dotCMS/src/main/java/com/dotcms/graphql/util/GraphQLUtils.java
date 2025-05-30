package com.dotcms.graphql.util;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;

public class GraphQLUtils {
    public static boolean isRedirectPage(Contentlet c, DotGraphQLContext ctx) {
        return c == null || !UtilMethods.isSet(c.getContentType()) ||
                Boolean.TRUE.equals(ctx.getParam("isVanityRedirect"));
    }
}
