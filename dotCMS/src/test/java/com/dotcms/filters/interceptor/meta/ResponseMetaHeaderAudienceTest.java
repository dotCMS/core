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
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Checks which requests {@link ResponseMetaDataWebInterceptor} sends {@code x-dot-server} to, including when the
 * request asks for it with {@code ?nodeid=true}.
 */
public class ResponseMetaHeaderAudienceTest extends UnitTestBase {

    private static final String REMOTE_ADDRESS = "10.1.2.3";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    /**
     * Starts every test from the shipped default: the anonymous flag and the node header settings
     * are unset, whatever an earlier test class in the same JVM or a test properties file left behind.
     */
    @Before
    public void clearAudienceSettings() {
        clearSettings();
    }

    @After
    public void tearDown() {
        clearSettings();
    }

    private static void clearSettings() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, null);
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, null);
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, null);
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
     * Method to test: {@link ResponseMetaHeaderAudience#includes}
     * Given scenario: both interceptors handle the same request that carries a bearer token
     * Expected result: the token is resolved to a user once, and both interceptors get the answer
     */
    @Test
    public void testIncludes_whenAskedTwiceForOneRequest_shouldResolveCredentialsOnce() {
        final Map<String, Object> attributes = rememberRequestAttributes();
        when(request.getHeader("Authorization")).thenReturn("Bearer a-token");
        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);
        when(request.getParameter("nodeid")).thenReturn("true");
        final JsonWebTokenService tokenService =
                tokenServiceAccepting("a-token", user("dotcms.org.1", true));

        try (MockedStatic<JsonWebTokenFactory> factory = givenTokenService(tokenService);
                MockedStatic<APILocator> apiLocator = givenNodeHeaderInputs()) {
            new ResponseMetaDataWebInterceptor().intercept(request, response);
        }

        verify(tokenService, times(1)).parseToken("a-token", REMOTE_ADDRESS);

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
     * Given scenario: the node header is switched off with RESPONSE_HEADER_ADD_NODE_ID=false, the
     * anonymous feature flag is on, and the request has no nodeid parameter
     * Expected result: the x-dot-server header is not added, the flag does not override the switch
     */
    @Test
    public void testIntercept_whenNodeHeaderConfigOffAndAnonymousFlagOn_shouldNotAddNodeHeader() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, true);
        Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, false);

        try (MockedStatic<APILocator> apiLocator = givenNodeHeaderInputs()) {
            new ResponseMetaDataWebInterceptor().intercept(request, response);
        }

        verify(response, never()).addHeader(anyString(), anyString());
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

    /** A token service that resolves {@code token} to {@code user} only from {@link #REMOTE_ADDRESS}. */
    private static JsonWebTokenService tokenServiceAccepting(final String token, final User user) {
        final JWToken jwToken = mock(JWToken.class);
        when(jwToken.getActiveUser()).thenReturn(Optional.of(user));
        final JsonWebTokenService tokenService = mock(JsonWebTokenService.class);
        when(tokenService.parseToken(token, REMOTE_ADDRESS)).thenReturn(jwToken);
        return tokenService;
    }

    /** Serves {@code tokenService} from {@link JsonWebTokenFactory#getInstance()}. */
    private static MockedStatic<JsonWebTokenFactory> givenTokenService(
            final JsonWebTokenService tokenService) {
        final JsonWebTokenFactory factory = mock(JsonWebTokenFactory.class);
        when(factory.getJsonWebTokenService()).thenReturn(tokenService);
        final MockedStatic<JsonWebTokenFactory> mocked = Mockito.mockStatic(JsonWebTokenFactory.class);
        mocked.when(JsonWebTokenFactory::getInstance).thenReturn(factory);
        return mocked;
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


}
