package com.dotcms.rest.api.v1.system.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for the user put on a WebSocket connection at handshake time. The user comes from
 * the HTTP session, checked through the same resolution every request uses, so a session that
 * yields no user there yields no user on the socket either. Without a session user, a bearer token
 * identifies the user of that one connection and leaves the HTTP session as it was.
 */
class DotCmsWebSocketConfiguratorTest {

    private static final String USER_ID = "user_a";
    private static final String ADMIN_ID = "admin_a";

    private UserAPI userAPI;
    private ApiTokenAPI tokenAPI;
    private HttpSession session;
    private Map<String, Object> userProperties;
    private ServerEndpointConfig endpointConfig;
    private HandshakeRequest request;
    private DotCmsWebSocketConfigurator configurator;

    @BeforeEach
    void setUp() {
        userAPI = mock(UserAPI.class);
        tokenAPI = mock(ApiTokenAPI.class);
        session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session_1");
        userProperties = new HashMap<>();
        endpointConfig = mock(ServerEndpointConfig.class);
        when(endpointConfig.getUserProperties()).thenReturn(userProperties);
        request = mock(HandshakeRequest.class);
        when(request.getHttpSession()).thenReturn(session);
        configurator = new DotCmsWebSocketConfigurator(mock(WebSocketContainerAPI.class),
                mock(JsonWebTokenAuthCredentialProcessor.class));
    }

    private static User user(final String userId, final boolean active) {
        final User user = new User();
        user.setUserId(userId);
        user.setActive(active);
        return user;
    }

    private void handshake() {
        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
            apiLocator.when(APILocator::getApiTokenAPI).thenReturn(tokenAPI);
            configurator.modifyHandshake(endpointConfig, request, mock(HandshakeResponse.class));
        }
    }

    @Test
    void userIdOnlySession_activeUser_isPutOnTheSocket() throws DotDataException, DotSecurityException {
        final User activeUser = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(activeUser);

        handshake();

        assertSame(activeUser, userProperties.get(SystemEventsWebSocketEndPoint.USER));
        assertEquals("session_1", userProperties.get(SystemEventsWebSocketEndPoint.USER_SESSION_ID));
    }

    @Test
    void loginAsSession_principalLookupError_putsNoUserOnTheSocket() throws DotDataException, DotSecurityException {
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(session.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn(ADMIN_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user(USER_ID, true));
        when(userAPI.loadUserById(ADMIN_ID)).thenThrow(new DotDataException("lookup failed"));

        handshake();

        assertFalse(userProperties.containsKey(SystemEventsWebSocketEndPoint.USER));
        assertEquals("session_1", userProperties.get(SystemEventsWebSocketEndPoint.USER_SESSION_ID));
    }

    @Test
    void sessionUser_lookupError_putsNoUserOnTheSocket() throws DotDataException, DotSecurityException {
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenThrow(new DotDataException("lookup failed"));

        handshake();

        assertFalse(userProperties.containsKey(SystemEventsWebSocketEndPoint.USER));
    }

    @Test
    void sessionWithoutUser_putsNoUserOnTheSocket() {
        handshake();

        assertFalse(userProperties.containsKey(SystemEventsWebSocketEndPoint.USER));
        assertEquals("session_1", userProperties.get(SystemEventsWebSocketEndPoint.USER_SESSION_ID));
    }

    /** A session that keeps its attributes, so what one handshake writes the next one reads. */
    private static HttpSession attributeKeepingSession() {
        final Map<String, Object> attributes = new HashMap<>();
        final HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session_2");
        when(session.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.<String>getArgument(0)));
        doAnswer(call -> attributes.put(call.getArgument(0), call.getArgument(1)))
                .when(session).setAttribute(anyString(), any());
        doAnswer(call -> attributes.remove(call.<String>getArgument(0)))
                .when(session).removeAttribute(anyString());
        return session;
    }

    /** Sends the bearer header on the handshake and resolves it with the real token processor. */
    private void withBearerToken(final HttpSession httpSession) {
        when(request.getHttpSession()).thenReturn(httpSession);
        when(request.getHeaders()).thenReturn(Map.of("Authorization", List.of("Bearer abc")));
        configurator = new DotCmsWebSocketConfigurator(mock(WebSocketContainerAPI.class),
                new JsonWebTokenAuthCredentialProcessorImpl(mock(JsonWebTokenUtils.class)) { });
    }

    private void tokenResolvesTo(final User user) {
        final JWToken token = mock(JWToken.class);
        when(token.getActiveUser()).thenReturn(Optional.of(user));
        when(tokenAPI.fromJwt(anyString(), anyString())).thenReturn(Optional.of(token));
    }

    @Test
    void tokenOnlyHandshake_putsTheUserOnTheSocket_andLeavesTheSessionWithoutUser()
            throws DotDataException, DotSecurityException {
        final User tokenUser = user(USER_ID, true);
        when(userAPI.loadUserById(USER_ID)).thenReturn(tokenUser);
        tokenResolvesTo(tokenUser);
        final HttpSession anonymousSession = attributeKeepingSession();
        withBearerToken(anonymousSession);

        handshake();

        assertSame(tokenUser, userProperties.get(SystemEventsWebSocketEndPoint.USER));
        assertNull(anonymousSession.getAttribute(WebKeys.USER_ID));
        assertNull(anonymousSession.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER));
        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
            assertNull(PortalUtil.getUser(anonymousSession));
        }
    }

    @Test
    void afterTheTokenIsRevoked_theSameSessionGetsNoUserOnANewSocket()
            throws DotDataException, DotSecurityException {
        final User tokenUser = user(USER_ID, true);
        when(userAPI.loadUserById(USER_ID)).thenReturn(tokenUser);
        tokenResolvesTo(tokenUser);
        final HttpSession anonymousSession = attributeKeepingSession();
        withBearerToken(anonymousSession);
        handshake();

        when(tokenAPI.fromJwt(anyString(), anyString())).thenReturn(Optional.empty());
        userProperties.clear();
        handshake();

        assertFalse(userProperties.containsKey(SystemEventsWebSocketEndPoint.USER));
        assertEquals("session_2", userProperties.get(SystemEventsWebSocketEndPoint.USER_SESSION_ID));
    }

    @Test
    void loggedInSession_withBearerToken_keepsTheSessionUser() throws DotDataException, DotSecurityException {
        final User sessionUser = user(ADMIN_ID, true);
        when(userAPI.loadUserById(ADMIN_ID)).thenReturn(sessionUser);
        tokenResolvesTo(user(USER_ID, true));
        final HttpSession loggedInSession = attributeKeepingSession();
        loggedInSession.setAttribute(WebKeys.USER_ID, ADMIN_ID);
        withBearerToken(loggedInSession);

        handshake();

        assertSame(sessionUser, userProperties.get(SystemEventsWebSocketEndPoint.USER));
        assertEquals(ADMIN_ID, loggedInSession.getAttribute(WebKeys.USER_ID));
    }
}
