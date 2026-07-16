package com.dotcms.graphql;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class DotGraphQLContextTest {

    /**
     * Verifies that calling markAsVanityRedirect sets the internal flag,
     * and that isVanityRedirect returns true afterward.
     */
    @Test
    void markAsVanityRedirect_setsFlagCorrectly() {
        var context = DotGraphQLContext.createServletContext().build();
        context.markAsVanityRedirect();

        assertTrue(context.isVanityRedirect());
    }


    /**
     * Verifies that isVanityRedirect returns true when the flag is set to true in context.
     */
    @Test
    void returnsTrue_whenVanityRedirectFlagIsTrue() {
        var context = Mockito.spy(DotGraphQLContext.createServletContext().build());
        context.addParam("isVanityRedirect", true);

        assertTrue(context.isVanityRedirect());
    }

    /**
     * Verifies that isVanityRedirect returns false when the flag is false in context.
     */
    @Test
    void returnsFalse_whenVanityRedirectFlagIsFalse() {
        var context = Mockito.spy(DotGraphQLContext.createServletContext().build());
        context.addParam("isVanityRedirect", false);

        assertFalse(context.isVanityRedirect());
    }

    /**
     * Verifies that isVanityRedirect returns false when the flag is not set.
     */
    @Test
    void returnsFalse_whenVanityRedirectFlagIsMissing() {
        var context = Mockito.spy(DotGraphQLContext.createServletContext().build());

        assertFalse(context.isVanityRedirect());
    }
}