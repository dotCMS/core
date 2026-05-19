package com.dotcms.rest.api.v1.analytics.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link EventAnalyticsProxyHelper} security boundary helpers.
 */
public class EventAnalyticsProxyHelperTest {

    @Test
    public void isAllowedRelativePath_acceptsEventPaths() {
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event/total-events"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event/unique-visitors"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event/pageviews-by-device-browser"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event/ingest"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event"));
    }

    @Test
    public void isAllowedRelativePath_rejectsNullEmptyBlank() {
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath(null));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath(""));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("   "));
    }

    @Test
    public void isAllowedRelativePath_rejectsTraversalSegments() {
        // JAX-RS URL-decodes %2e%2e to .. before binding the @PathParam, so these are
        // representative of what an attacker would actually reach this helper with.
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("../admin/token"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("event/../admin/token"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("event/foo/../../admin"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath(".."));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("event\\..\\admin"));
    }

    @Test
    public void isAllowedRelativePath_rejectsNonEventPrefixes() {
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("admin/token"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("health"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("v1/event/total-events"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("eventfoo")); // not "event/"
    }
}
