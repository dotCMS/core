package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
