package com.dotcms.graphql.util;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.dotcms.graphql.util.GraphQLUtils.IS_VANITY_REDIRECT_PARAM;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GraphQLUtils#isRedirectPage(Contentlet, DotGraphQLContext)}.
 */
class GraphQLUtilsTest {

    /**
     * Verifies that isRedirectPage returns false when the contentlet has no content type but redirect flag is false.
     */
    @Test
    void returnsFalse_whenContentletHasNoContentType_andNoRedirectFlag() {
        Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getContentType()).thenReturn(null);

        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam(IS_VANITY_REDIRECT_PARAM)).thenReturn(false);

        assertFalse(GraphQLUtils.isRedirectPage(contentlet, ctx));
    }

    /**
     * Verifies that isRedirectPage returns false when the contentlet has a content type and redirect flag is true.
     */
    @Test
    void returnsFalse_whenContentletIsValid_andRedirectFlagIsTrue() {
        Contentlet contentlet = Mockito.mock(Contentlet.class);
        ContentType type = Mockito.mock(ContentType.class);
        Mockito.when(contentlet.getContentType()).thenReturn(type);

        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam(IS_VANITY_REDIRECT_PARAM)).thenReturn(true);

        assertFalse(GraphQLUtils.isRedirectPage(contentlet, ctx));
    }

    /**
     * Verifies that isRedirectPage returns true when the contentlet has no content type and redirect flag is true.
     */
    @Test
    void returnsTrue_whenContentletHasNoContentType_andRedirectFlagIsTrue() {
        Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getContentType()).thenReturn(null);

        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam(IS_VANITY_REDIRECT_PARAM)).thenReturn(true);

        assertTrue(GraphQLUtils.isRedirectPage(contentlet, ctx));
    }

    /**
     * Verifies that isRedirectPage returns false when the contentlet is valid and redirect flag is false.
     */
    @Test
    void returnsFalse_whenContentletIsValid_andNoRedirectFlag() {
        Contentlet contentlet = Mockito.mock(Contentlet.class);
        ContentType type = Mockito.mock(ContentType.class);
        Mockito.when(contentlet.getContentType()).thenReturn(type);

        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam(IS_VANITY_REDIRECT_PARAM)).thenReturn(false);

        assertFalse(GraphQLUtils.isRedirectPage(contentlet, ctx));
    }
}
