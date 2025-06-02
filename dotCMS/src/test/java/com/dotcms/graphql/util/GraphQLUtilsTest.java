package com.dotcms.graphql.util;

import com.dotcms.graphql.DotGraphQLContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.dotcms.graphql.util.GraphQLUtils.IS_VANITY_REDIRECT_PARAM;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GraphQLUtils#isRedirectPage(DotGraphQLContext)}.
 */
class GraphQLUtilsTest {

    /**
     * Verifies that isRedirectPage returns true when the redirect flag is true.
     */
    @Test
    void returnsTrue_whenRedirectFlagIsTrue() {
        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam(IS_VANITY_REDIRECT_PARAM)).thenReturn(true);

        assertTrue(GraphQLUtils.isRedirectPage(ctx));
    }

    /**
     * Verifies that isRedirectPage returns false when the redirect flag is false.
     */
    @Test
    void returnsFalse_whenRedirectFlagIsFalse() {
        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam(IS_VANITY_REDIRECT_PARAM)).thenReturn(false);

        assertFalse(GraphQLUtils.isRedirectPage(ctx));
    }

    /**
     * Verifies that isRedirectPage returns false when the redirect flag is not set.
     */
    @Test
    void returnsFalse_whenRedirectFlagIsMissing() {
        DotGraphQLContext ctx = Mockito.mock(DotGraphQLContext.class);
        Mockito.when(ctx.getParam(IS_VANITY_REDIRECT_PARAM)).thenReturn(null);

        assertFalse(GraphQLUtils.isRedirectPage(ctx));
    }
}
