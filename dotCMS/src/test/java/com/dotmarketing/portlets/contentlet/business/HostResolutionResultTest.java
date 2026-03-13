package com.dotmarketing.portlets.contentlet.business;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link HostResolutionResult} value object.
 *
 * <p>These tests verify the static factory methods and accessors without requiring any
 * dotCMS infrastructure.</p>
 */
public class HostResolutionResultTest {

    // -------------------------------------------------------------------------
    // nested() factory
    // -------------------------------------------------------------------------

    @Test
    public void nested_setsFieldsCorrectly() {
        final HostResolutionResult result = HostResolutionResult.nested("host-uuid", "/page.html");

        assertTrue("isNested must be true", result.isNested());
        assertEquals("host-uuid", result.getResolvedHostId());
        assertEquals("/page.html", result.getRemainingUri());
    }

    @Test
    public void nested_nullRemainingUri_defaultsToRootSlash() {
        final HostResolutionResult result = HostResolutionResult.nested("host-uuid", null);

        assertEquals("null remaining URI must default to '/'", "/", result.getRemainingUri());
    }

    @Test
    public void nested_emptyRemainingUri_defaultsToRootSlash() {
        final HostResolutionResult result = HostResolutionResult.nested("host-uuid", "");

        assertEquals("empty remaining URI must default to '/'", "/", result.getRemainingUri());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nested_nullHostId_throwsIllegalArgumentException() {
        HostResolutionResult.nested(null, "/page.html");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nested_emptyHostId_throwsIllegalArgumentException() {
        HostResolutionResult.nested("", "/page.html");
    }

    // -------------------------------------------------------------------------
    // topLevel() factory
    // -------------------------------------------------------------------------

    @Test
    public void topLevel_setsFieldsCorrectly() {
        final HostResolutionResult result = HostResolutionResult.topLevel("/original/uri.html");

        assertFalse("isNested must be false for a top-level result", result.isNested());
        assertNull("resolvedHostId must be null for a top-level result", result.getResolvedHostId());
        assertEquals("/original/uri.html", result.getRemainingUri());
    }

    @Test
    public void topLevel_nullUri_storesNull() {
        final HostResolutionResult result = HostResolutionResult.topLevel(null);

        assertFalse(result.isNested());
        assertNull(result.getResolvedHostId());
        assertNull(result.getRemainingUri());
    }

    @Test
    public void topLevel_rootSlashUri_preserved() {
        final HostResolutionResult result = HostResolutionResult.topLevel("/");

        assertFalse(result.isNested());
        assertEquals("/", result.getRemainingUri());
    }

    // -------------------------------------------------------------------------
    // toString()
    // -------------------------------------------------------------------------

    @Test
    public void toString_nested_containsFields() {
        final HostResolutionResult result = HostResolutionResult.nested("abc-123", "/about");
        final String str = result.toString();

        assertNotNull(str);
        assertTrue("toString must contain resolvedHostId", str.contains("abc-123"));
        assertTrue("toString must contain remainingUri",   str.contains("/about"));
    }

    @Test
    public void toString_topLevel_containsRemainingUri() {
        final HostResolutionResult result = HostResolutionResult.topLevel("/home.html");
        final String str = result.toString();

        assertNotNull(str);
        assertTrue("toString must contain remainingUri", str.contains("/home.html"));
    }
}
