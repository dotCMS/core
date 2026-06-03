package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.auth.AuthAccessDeniedUtil;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.List;
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
