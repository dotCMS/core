package com.dotcms.auth.dotAuth.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link DotAuthSessionCredentialProcessorImpl}: prefix-based
 * short-circuit to dotAuth sessions, 401 on unknown / inactive sessions, and
 * null-return for any bearer that isn't one of ours (so the JWT processor gets
 * its turn downstream).
 */
class DotAuthSessionCredentialProcessorImplTest {

    private DotAuthSessionCache cache;
    private DotAuthSessionCredentialProcessorImpl processor;

    @BeforeEach
    void setUp() {
        cache = mock(DotAuthSessionCache.class);
        processor = new DotAuthSessionCredentialProcessorImpl(cache);
    }

    @Test
    void noAuthorizationHeader_returnsNull_soChainCanContinue() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn(null);

        assertNull(processor.processAuthHeaderFromSessionRef(request));
    }

    @Test
    void jwtBearer_returnsNull_soJwtProcessorRunsNext() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION))
                .thenReturn("Bearer eyJhbGciOiJIUzI1NiJ9.fake.signature");

        assertNull(processor.processAuthHeaderFromSessionRef(request));
    }

    @Test
    void sessionRefUnknown_throwsUnauthorized() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION))
                .thenReturn("Bearer " + DotAuthSessionCache.SESSION_REF_PREFIX + "deadbeef");
        when(cache.get(any())).thenReturn(Optional.empty());

        final SecurityException ex = assertThrows(SecurityException.class,
                () -> processor.processAuthHeaderFromSessionRef(request));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                ex.getResponse().getStatus(),
                "unknown session-ref must surface as 401, not be ignored");
    }

    @Test
    void sessionRefInactiveUser_invalidatesAndThrows() throws Exception {
        final String ref = DotAuthSessionCache.SESSION_REF_PREFIX + "abc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer " + ref);
        when(cache.get(ref))
                .thenReturn(Optional.of(new DotAuthSession("user-1", 0L, Long.MAX_VALUE)));

        final User user = mock(User.class);
        when(user.isActive()).thenReturn(false);

        final UserAPI userAPI = mock(UserAPI.class);
        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            when(userAPI.loadUserById("user-1")).thenReturn(user);

            final SecurityException ex = assertThrows(SecurityException.class,
                    () -> processor.processAuthHeaderFromSessionRef(request));
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                    ex.getResponse().getStatus());
        }
        verify(cache).invalidate(ref);
    }

    @Test
    void sessionRefActiveUser_returnsUserAndStampsRequest() throws Exception {
        final String ref = DotAuthSessionCache.SESSION_REF_PREFIX + "xyz";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer " + ref);
        when(cache.get(ref))
                .thenReturn(Optional.of(new DotAuthSession("user-7", 0L, Long.MAX_VALUE)));

        final User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(user.getUserId()).thenReturn("user-7");

        final UserAPI userAPI = mock(UserAPI.class);
        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            when(userAPI.loadUserById("user-7")).thenReturn(user);

            final User resolved = processor.processAuthHeaderFromSessionRef(request);
            assertTrue(resolved == user, "must return the exact user resolved from the session");
        }
        verify(request).setAttribute(com.liferay.portal.util.WebKeys.USER_ID, "user-7");
        verify(request).setAttribute(com.liferay.portal.util.WebKeys.USER, user);
    }

    @Test
    void bearerPrefixWithoutSessionRefPrefix_returnsNull() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ContainerRequest.AUTHORIZATION))
                .thenReturn("Bearer some-opaque-not-a-jwt-and-not-ours");

        assertNull(processor.processAuthHeaderFromSessionRef(request));
    }
}
