package com.dotcms.auth.dotAuth.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.dotAuth.session.DotAuthSession;
import com.dotcms.auth.dotAuth.session.DotAuthSessionCache;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * A dotAuth session reference that resolves to the built-in account is refused like an unusable user
 * (401, reference invalidated) and writes nothing to the request.
 */
class DotAuthSessionRefInteractiveUserTest {

    @Test
    void sessionRefForBuiltInAccount_isRefusedAndWritesNothing() throws Exception {
        final String ref = DotAuthSessionCache.SESSION_REF_PREFIX + "abc";
        final DotAuthSessionCache cache = mock(DotAuthSessionCache.class);
        when(cache.get(ref)).thenReturn(Optional.of(new DotAuthSession(UserAPI.SYSTEM_USER_ID, 0L, Long.MAX_VALUE)));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer " + ref);

        final User system = new User();
        system.setUserId(UserAPI.SYSTEM_USER_ID);
        system.setActive(true);
        final UserAPI userAPI = mock(UserAPI.class);
        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            when(userAPI.loadUserById(UserAPI.SYSTEM_USER_ID)).thenReturn(system);

            final SecurityException ex = assertThrows(SecurityException.class,
                    () -> new DotAuthSessionCredentialProcessorImpl(cache).processAuthHeaderFromSessionRef(request));
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getResponse().getStatus());
        }
        verify(request, never()).setAttribute(anyString(), any());
        verify(cache).invalidate(ref);
    }
}
