package com.dotcms.filters.interceptor.meta;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.config.MinSdkVersion;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.ReleaseInfo;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Test;

/**
 * Checks which requests {@link SdkVersionWebInterceptor} sends the version headers to, and that
 * {@link ResponseMetaDataWebInterceptor} leaves {@code x-dot-server} off requests without
 * credentials.
 */
public class ResponseMetaHeaderAudienceTest extends UnitTestBase {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @After
    public void tearDown() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, null);
    }

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept}
     * Given scenario: a request with no session and no Authorization header
     * Expected result: neither version header is added
     */
    @Test
    public void testIntercept_whenNoCredentials_shouldNotAddVersionHeaders() {
        new SdkVersionWebInterceptor().intercept(request, response);

        verifyNoHeaders();
    }

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept}
     * Given scenario: a request whose Authorization header is not a token dotCMS issued
     * Expected result: neither version header is added, so presenting any header value is not enough
     */
    @Test
    public void testIntercept_whenBearerTokenIsNotValid_shouldNotAddVersionHeaders() {
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-token");

        new SdkVersionWebInterceptor().intercept(request, response);

        verifyNoHeaders();
    }

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept}
     * Given scenario: a request whose session holds the anonymous user
     * Expected result: neither version header is added
     */
    @Test
    public void testIntercept_whenSessionUserIsAnonymous_shouldNotAddVersionHeaders() {
        givenSessionUser("anonymous", true);

        new SdkVersionWebInterceptor().intercept(request, response);

        verifyNoHeaders();
    }

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept}
     * Given scenario: a request whose session holds an inactive user
     * Expected result: neither version header is added
     */
    @Test
    public void testIntercept_whenSessionUserIsInactive_shouldNotAddVersionHeaders() {
        givenSessionUser("dotcms.org.1", false);

        new SdkVersionWebInterceptor().intercept(request, response);

        verifyNoHeaders();
    }

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept}
     * Given scenario: a request whose session holds an active, non-anonymous user
     * Expected result: both version headers are added
     */
    @Test
    public void testIntercept_whenSessionUserIsLoggedIn_shouldAddVersionHeaders() {
        givenSessionUser("dotcms.org.1", true);

        new SdkVersionWebInterceptor().intercept(request, response);

        verifyVersionHeaders();
    }

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept}
     * Given scenario: no credentials, with the anonymous feature flag turned on
     * Expected result: both version headers are added, as before the flag existed
     */
    @Test
    public void testIntercept_whenAnonymousFlagOn_shouldAddVersionHeadersWithoutCredentials() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, true);

        new SdkVersionWebInterceptor().intercept(request, response);

        verifyVersionHeaders();
    }

    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept}
     * Given scenario: a request with no credentials and no nodeid parameter
     * Expected result: the x-dot-server header is not added
     */
    @Test
    public void testIntercept_whenNoCredentials_shouldNotAddNodeHeader() {
        new ResponseMetaDataWebInterceptor().intercept(request, response);

        verify(response, never()).addHeader(anyString(), anyString());
    }

    private void givenSessionUser(final String userId, final boolean active) {
        final User user = new User();
        user.setUserId(userId);
        user.setActive(active);
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.USER)).thenReturn(user);
        when(request.getSession(false)).thenReturn(session);
    }

    private void verifyNoHeaders() {
        verify(response, never()).setHeader(anyString(), anyString());
    }

    private void verifyVersionHeaders() {
        verify(response).setHeader(SdkVersionWebInterceptor.DOTCMS_VERSION_HEADER,
                ReleaseInfo.getVersion());
        verify(response).setHeader(SdkVersionWebInterceptor.DOTCMS_MIN_SDK_HEADER,
                MinSdkVersion.VALUE);
    }

}
