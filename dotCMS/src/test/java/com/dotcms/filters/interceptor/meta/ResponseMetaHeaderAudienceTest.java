package com.dotcms.filters.interceptor.meta;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.config.MinSdkVersion;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.ReleaseInfo;
import com.liferay.portal.util.WebKeys;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Checks which requests {@link SdkVersionWebInterceptor} sends the version headers to, and which
 * ones {@link ResponseMetaDataWebInterceptor} sends {@code x-dot-server} to, including when the
 * request asks for it with {@code ?nodeid=true}.
 */
public class ResponseMetaHeaderAudienceTest extends UnitTestBase {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @After
    public void tearDown() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, null);
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, null);
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, null);
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

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept}
     * Given scenario: a request whose bearer token resolves to an active, non-anonymous user
     * Expected result: both version headers are added
     */
    @Test
    public void testIntercept_whenBearerTokenIsValid_shouldAddVersionHeaders() {
        when(request.getHeader("Authorization")).thenReturn("Bearer a-token");

        try (MockedStatic<PortalUtil> portalUtil = Mockito.mockStatic(PortalUtil.class)) {
            portalUtil.when(() -> PortalUtil.getUserFromAuthHeader(request))
                    .thenReturn(user("dotcms.org.1", true));

            new SdkVersionWebInterceptor().intercept(request, response);
        }

        verifyVersionHeaders();
    }

    /**
     * Method to test: {@link ResponseMetaHeaderAudience#includes}
     * Given scenario: both interceptors handle the same request that carries a bearer token
     * Expected result: the token is resolved to a user once, and both interceptors get the answer
     */
    @Test
    public void testIncludes_whenAskedTwiceForOneRequest_shouldResolveCredentialsOnce() {
        final Map<String, Object> attributes = rememberRequestAttributes();
        when(request.getHeader("Authorization")).thenReturn("Bearer a-token");
        when(request.getParameter("nodeid")).thenReturn("true");

        try (MockedStatic<PortalUtil> portalUtil = Mockito.mockStatic(PortalUtil.class);
                MockedStatic<APILocator> apiLocator = givenNodeHeaderInputs()) {
            portalUtil.when(() -> PortalUtil.getUserFromAuthHeader(request))
                    .thenReturn(user("dotcms.org.1", true));

            new SdkVersionWebInterceptor().intercept(request, response);
            new ResponseMetaDataWebInterceptor().intercept(request, response);

            portalUtil.verify(() -> PortalUtil.getUserFromAuthHeader(request), times(1));
        }

        verifyVersionHeaders();
        verify(response).addHeader(eq(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER), anyString());
        assertEquals(Boolean.TRUE, attributes.get(ResponseMetaHeaderAudience.DECISION_ATTRIBUTE));
    }

    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept}
     * Given scenario: a request with no credentials that asks for the header with nodeid=true
     * Expected result: the x-dot-server header is not added
     */
    @Test
    public void testIntercept_whenNodeIdRequestedWithoutCredentials_shouldNotAddNodeHeader() {
        when(request.getParameter("nodeid")).thenReturn("true");

        new ResponseMetaDataWebInterceptor().intercept(request, response);

        verify(response, never()).addHeader(anyString(), anyString());
    }

    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept}
     * Given scenario: a logged-in request with nodeid=true while the node header is switched off
     * Expected result: the x-dot-server header is added, as nodeid=true forces it on for operators
     */
    @Test
    public void testIntercept_whenNodeIdRequestedWithCredentials_shouldAddNodeHeader() {
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, false);
        givenSessionUser("dotcms.org.1", true);
        when(request.getParameter("nodeid")).thenReturn("true");

        try (MockedStatic<APILocator> apiLocator = givenNodeHeaderInputs()) {
            new ResponseMetaDataWebInterceptor().intercept(request, response);
        }

        verify(response).addHeader(eq(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER), anyString());
    }

    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept}
     * Given scenario: no credentials and no nodeid parameter, with the anonymous feature flag on
     * Expected result: the x-dot-server header is added
     */
    @Test
    public void testIntercept_whenAnonymousFlagOn_shouldAddNodeHeaderWithoutCredentials() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, true);

        try (MockedStatic<APILocator> apiLocator = givenNodeHeaderInputs()) {
            new ResponseMetaDataWebInterceptor().intercept(request, response);
        }

        verify(response).addHeader(eq(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER), anyString());
    }

    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept}
     * Given scenario: no credentials, nodeid=true and the anonymous feature flag on
     * Expected result: the x-dot-server header is added
     */
    @Test
    public void testIntercept_whenNodeIdRequestedAndAnonymousFlagOn_shouldAddNodeHeader() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, true);
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, false);
        when(request.getParameter("nodeid")).thenReturn("true");

        try (MockedStatic<APILocator> apiLocator = givenNodeHeaderInputs()) {
            new ResponseMetaDataWebInterceptor().intercept(request, response);
        }

        verify(response).addHeader(eq(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER), anyString());
    }

    private static User user(final String userId, final boolean active) {
        final User user = new User();
        user.setUserId(userId);
        user.setActive(active);
        return user;
    }

    /**
     * Stubs what the node header is built from: the server id, and no node name so the cluster
     * lookup, which needs a database, is skipped.
     */
    private static MockedStatic<APILocator> givenNodeHeaderInputs() {
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, false);
        final ServerAPI serverAPI = mock(ServerAPI.class);
        when(serverAPI.readServerId()).thenReturn("1234567890abcdef");
        final MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class);
        apiLocator.when(APILocator::getServerAPI).thenReturn(serverAPI);
        return apiLocator;
    }

    /** Makes the mock request keep the attributes set on it, like a real request does. */
    private Map<String, Object> rememberRequestAttributes() {
        final Map<String, Object> attributes = new HashMap<>();
        when(request.getAttribute(anyString())).thenAnswer(i -> attributes.get(i.<String>getArgument(0)));
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return attributes;
    }

    private void givenSessionUser(final String userId, final boolean active) {
        final User user = user(userId, active);
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
