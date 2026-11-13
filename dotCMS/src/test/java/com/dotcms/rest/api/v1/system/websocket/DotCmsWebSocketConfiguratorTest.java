package com.dotcms.rest.api.v1.system.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.HashMap;
import java.util.Map;
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
 * yields no user there yields no user on the socket either.
 */
class DotCmsWebSocketConfiguratorTest {

    private static final String USER_ID = "user_a";
    private static final String ADMIN_ID = "admin_a";

    private UserAPI userAPI;
    private HttpSession session;
    private Map<String, Object> userProperties;
    private ServerEndpointConfig endpointConfig;
    private HandshakeRequest request;
    private DotCmsWebSocketConfigurator configurator;

    @BeforeEach
    void setUp() {
        userAPI = mock(UserAPI.class);
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
}
