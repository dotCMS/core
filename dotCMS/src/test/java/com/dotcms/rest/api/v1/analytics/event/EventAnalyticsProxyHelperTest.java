package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;
import java.util.Optional;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
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
    public void isAllowedRelativePath_acceptsConversionPaths() {
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("conversion"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("conversion/content/attribution"));
    }

    @Test
    public void isAllowedRelativePath_acceptsSessionPaths() {
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("session"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("session/engagement"));
    }

    @Test
    public void isAllowedRelativePath_acceptsHealthPath() {
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("health"));
    }

    @Test
    public void isAllowedRelativePath_rejectsNullEmptyBlank() {
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath(null));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath(""));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("   "));
    }

    @Test
    public void isAllowedRelativePath_acceptsDotsInsideSegments() {
        // Regression: prior `contains("..")` check would have rejected these even though
        // they have no traversal intent. The fix is segment-based — only exact ".." or
        // "." segments are rejected.
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event/last..7..days"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event/foo.bar"));
        assertTrue(EventAnalyticsProxyHelper.isAllowedRelativePath("event/foo..bar.baz"));
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
    public void isAllowedRelativePath_rejectsDisallowedPrefixes() {
        // Admin / management surfaces on the event manager must not be reachable through
        // the proxy with a tenant-scoped bearer token.
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("admin/token"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("admin"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("tenants"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("tenants/foo"));
        // Off-by-prefix-boundary: must NOT match an allowed prefix as a substring of a
        // longer first segment.
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("v1/event/total-events"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("eventfoo"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("healthcheck"));
        assertFalse(EventAnalyticsProxyHelper.isAllowedRelativePath("conversionfoo"));
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

    /* ---------- wrapUpstreamResponse — status re-mapping ---------- */

    @SuppressWarnings("unchecked")
    private static CircuitBreakerUrl.Response<String> mockCbResponse(final int status, final String body) {
        final CircuitBreakerUrl.Response<String> response = mock(CircuitBreakerUrl.Response.class);
        when(response.getStatusCode()).thenReturn(status);
        when(response.getResponse()).thenReturn(body);
        return response;
    }

    @Test
    public void wrapUpstreamResponse_upstream401_remapsTo502() {
        // Bug: dotCMS UI's global HTTP interceptor treats any 401 from a fetch() as
        // session-expired and triggers /dotAdmin/logout. An upstream 401 from the event
        // manager (stale bearer, rotated secret, etc.) was logging the admin out
        // mid-dashboard — six concurrent analytics fetches × six 401 cascades = instant
        // session kill. Re-mapping to 502 Bad Gateway keeps the session intact.
        final Response result = EventAnalyticsProxyHelper.wrapUpstreamResponse(
                mockCbResponse(401, "{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid token signature\"}"));
        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), result.getStatus());
    }

    @Test
    public void wrapUpstreamResponse_upstream403_remapsTo502() {
        // Same rationale as the 401 case — an upstream 403 also isn't "this dotCMS user
        // is forbidden," it's "dotCMS-on-behalf-of-this-user lacks upstream authority."
        final Response result = EventAnalyticsProxyHelper.wrapUpstreamResponse(
                mockCbResponse(403, "{\"code\":\"FORBIDDEN\",\"message\":\"row policy denied\"}"));
        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), result.getStatus());
    }

    @Test
    public void wrapUpstreamResponse_upstream200_passesThrough() {
        final Response result = EventAnalyticsProxyHelper.wrapUpstreamResponse(
                mockCbResponse(200, "{\"data\":[]}"));
        assertEquals(200, result.getStatus());
    }

    @Test
    public void wrapUpstreamResponse_upstream500_passesThrough() {
        // Non-auth upstream errors must propagate unchanged so the dashboard can
        // distinguish "EM unreachable" from "auth misconfigured."
        final Response result = EventAnalyticsProxyHelper.wrapUpstreamResponse(
                mockCbResponse(500, "{\"code\":\"INTERNAL_ERROR\"}"));
        assertEquals(500, result.getStatus());
    }

    @Test
    public void wrapUpstreamResponse_upstream400_passesThrough() {
        // 400 means we (dotCMS) sent a bad request — surface it so the developer sees it.
        final Response result = EventAnalyticsProxyHelper.wrapUpstreamResponse(
                mockCbResponse(400, "{\"code\":\"INVALID_PARAMETER\"}"));
        assertEquals(400, result.getStatus());
    }
}
