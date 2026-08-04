package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.auth.AuthAccessDeniedUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

/**
 * Lightweight verification of {@link OAuthWebInterceptor}'s registration metadata.
 * Full HTTP-flow tests are covered by integration tests; these guard against
 * accidental removal of path coverage.
 */
class OAuthWebInterceptorTest {

    @Test
    void getFilters_includesCallbackLogoutAndProtectedUrls() {
        final OAuthWebInterceptor interceptor = new OAuthWebInterceptor();
        final List<String> filters = Arrays.asList(interceptor.getFilters());

        assertTrue(filters.contains(OAuthConstants.CALLBACK_PATH),
                "Interceptor must claim the callback path: " + filters);
        for (final String logoutPath : OAuthConstants.LOGOUT_PATHS) {
            assertTrue(filters.contains(logoutPath),
                    "Interceptor must claim logout path " + logoutPath + "; filters: " + filters);
        }
        assertTrue(filters.stream().anyMatch(f -> f.contains("/dotAdmin/") || f.equals("/dotAdmin/")),
                "Interceptor must claim at least one back-end protected URL: " + filters);
    }

    @Test
    void hasRequiredRole_backendLoginRejectsFrontendOnlyUser() {
        assertFalse(AuthAccessDeniedUtil.hasRequiredRole(new TestUser(false, true, false), false));
    }

    @Test
    void hasRequiredRole_frontendLoginAcceptsFrontendUser() {
        assertTrue(AuthAccessDeniedUtil.hasRequiredRole(new TestUser(false, true, false), true));
    }

    @Test
    void baseUrlFromRequest_omitsDefaultHttpsPort() {
        // Standard 443 must NOT appear in the URL — the IdP redirect_uri is registered without it.
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("cms.example.com");
        when(request.getServerPort()).thenReturn(443);
        assertEquals("https://cms.example.com",
                OAuthWebInterceptor.baseUrlFromRequest(request));
    }

    @Test
    void baseUrlFromRequest_omitsDefaultHttpPort() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("cms.example.com");
        when(request.getServerPort()).thenReturn(80);
        assertEquals("http://cms.example.com",
                OAuthWebInterceptor.baseUrlFromRequest(request));
    }

    @Test
    void baseUrlFromRequest_includesNonDefaultPort() {
        // localhost:8080 etc. keep the explicit port.
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        assertEquals("http://localhost:8080",
                OAuthWebInterceptor.baseUrlFromRequest(request));
    }

    @Test
    void baseUrlFromRequest_blankServerNameReturnsNull() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("");
        assertEquals(null, OAuthWebInterceptor.baseUrlFromRequest(request));
    }

    @Test
    void originalRequestUri_prefersServerSideRedirectAfterLogin() {
        // Core stores the real page in REDIRECT_AFTER_LOGIN; it must win over the client referrer.
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)).thenReturn("/members/");
        when(request.getParameter(OAuthConstants.PARAM_REFERRER)).thenReturn("/other/");
        assertEquals("/members/", OAuthWebInterceptor.originalRequestUri(request));
    }

    @Test
    void originalRequestUri_prefersReferrerParam() {
        // /dotCMS/login?referrer=/members/ must resolve to /members/, not the login URL itself.
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(OAuthConstants.PARAM_REFERRER)).thenReturn("/members/");
        assertEquals("/members/", OAuthWebInterceptor.originalRequestUri(request));
    }

    @Test
    void originalRequestUri_fallsBackToUriWithQueryWhenNoReferrer() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(OAuthConstants.PARAM_REFERRER)).thenReturn(null);
        when(request.getAttribute(javax.servlet.RequestDispatcher.FORWARD_REQUEST_URI)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/dotAdmin/");
        when(request.getQueryString()).thenReturn(null);
        assertEquals("/dotAdmin/", OAuthWebInterceptor.originalRequestUri(request));
    }

    @Test
    void sanitizeRedirect_allowsColonInQueryString() {
        assertEquals("/dotAdmin/?contentType=Blog:detail",
                OAuthWebInterceptor.sanitizeRedirect("/dotAdmin/?contentType=Blog:detail", "/dotAdmin/"));
    }

    @Test
    void sanitizeRedirect_rejectsColonInPath() {
        assertEquals("/dotAdmin/",
                OAuthWebInterceptor.sanitizeRedirect("/http:/evil.test", "/dotAdmin/"));
    }

    @Test
    void sanitizeRedirect_rejectsProtocolRelative() {
        // //evil.test is treated by browsers as an absolute scheme-relative URL -> open redirect.
        assertEquals("/dotAdmin/",
                OAuthWebInterceptor.sanitizeRedirect("//evil.test", "/dotAdmin/"));
    }

    @Test
    void sanitizeRedirect_rejectsBackslashEscapedRoot() {
        assertEquals("/dotAdmin/",
                OAuthWebInterceptor.sanitizeRedirect("/\\evil.test", "/dotAdmin/"));
    }

    @Test
    void sanitizeRedirect_rejectsEmbeddedBackslash() {
        assertEquals("/dotAdmin/",
                OAuthWebInterceptor.sanitizeRedirect("/path\\x", "/dotAdmin/"));
    }

    @Test
    void sanitizeRedirect_nullReturnsFallback() {
        assertEquals("/dotAdmin/", OAuthWebInterceptor.sanitizeRedirect(null, "/dotAdmin/"));
    }

    @Test
    void sanitizeRedirect_emptyReturnsFallback() {
        assertEquals("/dotAdmin/", OAuthWebInterceptor.sanitizeRedirect("", "/dotAdmin/"));
    }

    @Test
    void sanitizeRedirect_allowsNormalRelativePath() {
        assertEquals("/dotAdmin/path?x=y",
                OAuthWebInterceptor.sanitizeRedirect("/dotAdmin/path?x=y", "/dotAdmin/"));
    }

    private static final class TestUser extends User {
        private final boolean backendUser;
        private final boolean frontendUser;
        private final boolean admin;

        private TestUser(final boolean backendUser, final boolean frontendUser, final boolean admin) {
            this.backendUser = backendUser;
            this.frontendUser = frontendUser;
            this.admin = admin;
        }

        @Override
        public boolean isBackendUser() {
            return backendUser;
        }

        @Override
        public boolean isFrontendUser() {
            return frontendUser;
        }

        @Override
        public boolean isAdmin() {
            return admin;
        }
    }
}
