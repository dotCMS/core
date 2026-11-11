package com.dotcms.rest.api.v1.system.websocket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * The handshake session never passes through the request filter, so the stored-id fallback of the configurator
 * must refuse the built-in account itself.
 */
public class DotCmsWebSocketConfiguratorInteractiveUserTest {

    private static User user(final String id) {
        final User user = new User();
        user.setUserId(id);
        return user;
    }

    private static Map<String, Object> handshake(final String storedId, final User loaded) throws Exception {
        final HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(storedId);
        final HandshakeRequest request = mock(HandshakeRequest.class);
        when(request.getHttpSession()).thenReturn(session);
        when(request.getHeaders()).thenReturn(Collections.emptyMap());
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(storedId)).thenReturn(loaded);
        final ServerEndpointConfig config = mock(ServerEndpointConfig.class);
        final Map<String, Object> properties = new HashMap<>();
        when(config.getUserProperties()).thenReturn(properties);
        try (MockedStatic<PortalUtil> portal = mockStatic(PortalUtil.class)) {
            portal.when(() -> PortalUtil.getUser(session)).thenReturn(null);
            new DotCmsWebSocketConfigurator(mock(WebSocketContainerAPI.class),
                    mock(JsonWebTokenAuthCredentialProcessor.class), userAPI)
                    .modifyHandshake(config, request, mock(HandshakeResponse.class));
        }
        return properties;
    }

    /** A session holding only the built-in account's id gets no user on the endpoint. */
    @Test
    public void stored_id_of_the_built_in_account_yields_no_user() throws Exception {
        final Map<String, Object> properties =
                handshake(UserAPI.SYSTEM_USER_ID, user(UserAPI.SYSTEM_USER_ID));
        assertFalse(properties.containsKey(SystemEventsWebSocketEndPoint.USER));
    }

    /** Control: a real user's stored id still resolves. */
    @Test
    public void stored_id_of_a_real_user_still_resolves() throws Exception {
        final User real = user("dotcms.org.1");
        final Map<String, Object> properties = handshake("dotcms.org.1", real);
        assertSame(real, properties.get(SystemEventsWebSocketEndPoint.USER));
    }
}
