package com.dotcms.auth.providers.jwt.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * A token whose active user is the built-in account is not a usable credential: the processor returns no
 * user and writes nothing to the request or session, so the chain ends anonymous. A real user is unchanged.
 */
class JsonWebTokenAuthCredentialProcessorInteractiveUserTest {

    private static User user(final String id) {
        final User user = new User();
        user.setUserId(id);
        return user;
    }

    private static JWToken tokenFor(final User user) {
        final JWToken token = mock(JWToken.class);
        when(token.getActiveUser()).thenReturn(Optional.of(user));
        return token;
    }

    private static HttpServletRequest request() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer abc");
        when(request.getRemoteAddr()).thenReturn("192.0.2.1");
        return request;
    }

    private interface Call {
        void run(JsonWebTokenAuthCredentialProcessorImpl processor, HttpServletRequest request,
                HttpSession session);
    }

    private static void withToken(final User tokenUser, final Call call, final HttpServletRequest request,
            final HttpSession session) throws Exception {
        final ApiTokenAPI tokenAPI = mock(ApiTokenAPI.class);
        final JWToken token = tokenFor(tokenUser);
        when(tokenAPI.fromJwt(anyString(), anyString())).thenReturn(Optional.of(token));
        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getApiTokenAPI).thenReturn(tokenAPI);
            call.run(new JsonWebTokenAuthCredentialProcessorImpl(mock(JsonWebTokenUtils.class)), request,
                    session);
        }
    }

    @Test
    void sessionOverload_builtInAccount_returnsNullAndWritesNothing() throws Exception {
        final HttpSession session = mock(HttpSession.class);
        withToken(user(UserAPI.SYSTEM_USER_ID), (p, r, s) ->
                assertNull(p.processAuthHeaderFromJWT("Bearer abc", s, "192.0.2.1")), request(), session);
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void requestOverload_builtInAccount_returnsNullAndWritesNothing() throws Exception {
        final HttpServletRequest request = request();
        withToken(user(UserAPI.SYSTEM_USER_ID), (p, r, s) ->
                assertNull(p.processAuthHeaderFromJWT(r)), request, null);
        verify(request, never()).setAttribute(anyString(), any());
    }

    @Test
    void jwtHeaderOverload_builtInAccount_writesNothing() throws Exception {
        final HttpServletRequest request = request();
        withToken(user(UserAPI.SYSTEM_USER_ID), (p, r, s) ->
                assertFalse(p.processJWTAuthHeader(r).isPresent()), request, null);
        verify(request, never()).setAttribute(anyString(), any());
    }

    @Test
    void requestOverload_regularUser_isUnchanged() throws Exception {
        final HttpServletRequest request = request();
        final User real = user("dotcms.org.1");
        final ApiTokenAPI tokenAPI = mock(ApiTokenAPI.class);
        final JWToken token = tokenFor(real);
        when(tokenAPI.fromJwt(anyString(), anyString())).thenReturn(Optional.of(token));
        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getApiTokenAPI).thenReturn(tokenAPI);
            final User resolved = new JsonWebTokenAuthCredentialProcessorImpl(mock(JsonWebTokenUtils.class))
                    .processAuthHeaderFromJWT(request);
            assertSame(real, resolved);
        }
        verify(request).setAttribute(WebKeys.USER, real);
    }
}
