package com.dotcms.graphql.util;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GraphQLUtils#isRedirectPage(Contentlet, DotGraphQLContext)}.
 */
class GraphQLUtilsTest {

    /**
     * Verifies that isRedirectPage returns true when the contentlet is null.
     */
    @Test
    void returnsTrue_whenContentletIsNull() {
        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);

        assertTrue(GraphQLUtils.isRedirectPage(null, ctx));
    }

    /**
     * Verifies that isRedirectPage returns true when the contentlet has no content type.
     */
    @Test
    void returnsTrue_whenContentletHasNoContentType() {
        Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getContentType()).thenReturn(null);

        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);

        assertTrue(GraphQLUtils.isRedirectPage(contentlet, ctx));
    }

    /**
     * Verifies that isRedirectPage returns true when the context contains the isVanityRedirect flag set to true.
     */
    @Test
    void returnsTrue_whenContextIndicatesVanityRedirect() {
        Contentlet contentlet = Mockito.mock(Contentlet.class);
        ContentType type = Mockito.mock(ContentType.class);

        Mockito.when(contentlet.getContentType()).thenReturn(type);
        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam("isVanityRedirect")).thenReturn(true);

        assertTrue(GraphQLUtils.isRedirectPage(contentlet, ctx));
    }

    /**
     * Verifies that isRedirectPage returns false when the contentlet is valid and there is no redirect flag in the context.
     */
    @Test
    void returnsFalse_whenContentletIsValidAndNoRedirectInContext() {
        Contentlet contentlet = Mockito.mock(Contentlet.class);
        ContentType type = Mockito.mock(ContentType.class);

        Mockito.when(contentlet.getContentType()).thenReturn(type);
        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam("isVanityRedirect")).thenReturn(false);

        assertFalse(GraphQLUtils.isRedirectPage(contentlet, ctx));
    }
}
