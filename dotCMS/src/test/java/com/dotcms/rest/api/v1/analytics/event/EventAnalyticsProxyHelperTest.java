package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;
import java.util.Optional;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
    public void isAllowedRelativePath_rejectsEmbeddedQueryOrFragment() {
        // JAX-RS URL-decodes %3F → ? and %23 → # in @PathParam, so these are the
        // actual strings that would reach the helper after smuggling encoded chars.
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("event/foo?bar"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("event/foo#bar"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("event/foo?bar=baz"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("event/foo#fragment"));
    }

    @Test
    public void isAllowedRelativePath_rejectsNonEventPrefixes() {
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("admin/token"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("health"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("v1/event/total-events"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("eventfoo")); // not "event/"
    }

    /* ---------- buildUpstreamUrl ---------- */

    private UriInfo emptyQueryUriInfo() {
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        return uriInfo;
    }

    private UriInfo uriInfoWith(final String key, final String... values) {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        for (final String v : values) {
            params.add(key, v);
        }
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(params);
        return uriInfo;
    }

    @Test
    public void buildUpstreamUrl_stripsTrailingSlashFromBase() {
        try (MockedStatic<Config> cfg = mockStatic(Config.class)) {
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_PROJECT"), anyString()))
                    .thenReturn("");
            assertEquals(
                    "http://em:8080/v1/event/total-events",
                    EventAnalyticsProxyHelper.buildUpstreamUrl(
                            "http://em:8080/", "event/total-events", emptyQueryUriInfo()));
        }
    }

    @Test
    public void buildUpstreamUrl_forwardsRequestQueryParams() {
        try (MockedStatic<Config> cfg = mockStatic(Config.class)) {
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_PROJECT"), anyString()))
                    .thenReturn("");
            final String url = EventAnalyticsProxyHelper.buildUpstreamUrl(
                    "http://em:8080", "event/total-events",
                    uriInfoWith("range", "last_7_days"));
            assertEquals("http://em:8080/v1/event/total-events?range=last_7_days", url);
        }
    }

    @Test
    public void buildUpstreamUrl_appendsConfiguredProjectWhenAbsent() {
        try (MockedStatic<Config> cfg = mockStatic(Config.class)) {
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_PROJECT"), anyString()))
                    .thenReturn("dev");
            final String url = EventAnalyticsProxyHelper.buildUpstreamUrl(
                    "http://em:8080", "event/total-events",
                    uriInfoWith("range", "last_7_days"));
            assertEquals(
                    "http://em:8080/v1/event/total-events?range=last_7_days&project=dev", url);
        }
    }

    @Test
    public void buildUpstreamUrl_doesNotDuplicateProjectWhenCallerSuppliesIt() {
        try (MockedStatic<Config> cfg = mockStatic(Config.class)) {
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_PROJECT"), anyString()))
                    .thenReturn("dev");
            final String url = EventAnalyticsProxyHelper.buildUpstreamUrl(
                    "http://em:8080", "event/total-events",
                    uriInfoWith("project", "marketing"));
            assertEquals("http://em:8080/v1/event/total-events?project=marketing", url);
        }
    }

    @Test
    public void buildUpstreamUrl_omitsQueryStringWhenNoParamsAndNoProject() {
        try (MockedStatic<Config> cfg = mockStatic(Config.class)) {
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_PROJECT"), anyString()))
                    .thenReturn("");
            assertEquals(
                    "http://em:8080/v1/event/total-events",
                    EventAnalyticsProxyHelper.buildUpstreamUrl(
                            "http://em:8080", "event/total-events", emptyQueryUriInfo()));
        }
    }

    /* ---------- buildAuthHeader — security boundary ---------- */

    @Test
    public void buildAuthHeader_perSiteTokenWins() {
        final Host host = mock(Host.class);
        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<Config> cfg = mockStatic(Config.class)) {
            utilMock.when(() -> ContentAnalyticsUtil.getBearerTokenFromAppSecrets(host))
                    .thenReturn(Optional.of("site-token-xyz"));
            // Fallback would also exist — per-site MUST take precedence.
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_BEARER_TOKEN"), anyString()))
                    .thenReturn("env-fallback-token");

            final Optional<String> header = EventAnalyticsProxyHelper.buildAuthHeader(host);
            assertTrue(header.isPresent());
            assertEquals("Bearer site-token-xyz", header.get());
        }
    }

    @Test
    public void buildAuthHeader_envFallbackUsedWhenSiteTokenAbsent() {
        final Host host = mock(Host.class);
        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<Config> cfg = mockStatic(Config.class)) {
            utilMock.when(() -> ContentAnalyticsUtil.getBearerTokenFromAppSecrets(host))
                    .thenReturn(Optional.empty());
            // Includes surrounding whitespace — the helper must trim before emitting.
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_BEARER_TOKEN"), anyString()))
                    .thenReturn("  env-fallback-token  ");

            final Optional<String> header = EventAnalyticsProxyHelper.buildAuthHeader(host);
            assertTrue(header.isPresent());
            assertEquals("Bearer env-fallback-token", header.get());
        }
    }

    @Test
    public void buildAuthHeader_nullHostFallsThroughToEnvToken() {
        try (MockedStatic<Config> cfg = mockStatic(Config.class)) {
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_BEARER_TOKEN"), anyString()))
                    .thenReturn("env-fallback-token");

            final Optional<String> header = EventAnalyticsProxyHelper.buildAuthHeader(null);
            assertTrue(header.isPresent());
            assertEquals("Bearer env-fallback-token", header.get());
        }
    }

    @Test
    public void buildAuthHeader_neitherSourceReturnsEmpty() {
        // Security-critical branch — must return empty (NOT "Bearer ") when no token is
        // available so the proxy omits Authorization entirely. RFC 6750: a bare "Bearer "
        // with no token is malformed and rejected by upstream gateways.
        final Host host = mock(Host.class);
        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<Config> cfg = mockStatic(Config.class)) {
            utilMock.when(() -> ContentAnalyticsUtil.getBearerTokenFromAppSecrets(host))
                    .thenReturn(Optional.empty());
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_BEARER_TOKEN"), anyString()))
                    .thenReturn("");

            final Optional<String> header = EventAnalyticsProxyHelper.buildAuthHeader(host);
            assertFalse(header.isPresent());
        }
    }

    @Test
    public void buildAuthHeader_blankSiteTokenFallsThroughToEnv() {
        // ContentAnalyticsUtil filters blanks already, but defense-in-depth: if a blank
        // ever surfaces here, the helper must not emit "Bearer " (no token).
        final Host host = mock(Host.class);
        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<Config> cfg = mockStatic(Config.class)) {
            utilMock.when(() -> ContentAnalyticsUtil.getBearerTokenFromAppSecrets(host))
                    .thenReturn(Optional.of("   "));
            cfg.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_BEARER_TOKEN"), anyString()))
                    .thenReturn("env-fallback-token");

            final Optional<String> header = EventAnalyticsProxyHelper.buildAuthHeader(host);
            assertTrue(header.isPresent());
            assertEquals("Bearer env-fallback-token", header.get());
        }
    }
}
