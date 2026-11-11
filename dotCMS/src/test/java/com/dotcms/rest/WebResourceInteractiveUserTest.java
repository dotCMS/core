package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.dotAuth.rest.DotAuthSessionCredentialProcessorImpl;
import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * {@code WebResource.authenticate} re-resolves the identity on its own, so every source it reads (session-ref
 * processor, token processor, back-end and front-end session readers) must refuse the built-in account: the
 * request ends as anonymous (401 when anonymous is not allowed) and the account is never stored on the request.
 */
public class WebResourceInteractiveUserTest extends UnitTestBase {

    private String previousPrincipal;
    private UserWebAPI userWebAPI;
    private JsonWebTokenAuthCredentialProcessor jwtProcessor;
    private DotAuthSessionCredentialProcessorImpl sessionRefProcessor;
    private WebResource resource;
    private MockedStatic<APILocator> apis;
    private MockedStatic<DotAuthSessionCredentialProcessorImpl> sessions;

    private static User user(final String id) {
        final User user = new User();
        user.setUserId(id);
        return user;
    }

    /** Builds a resource whose four identity sources are mocks the test controls. */
    @Before
    public void setUp() {
        previousPrincipal = PrincipalThreadLocal.getName();
        userWebAPI = mock(UserWebAPI.class);
        jwtProcessor = mock(JsonWebTokenAuthCredentialProcessor.class);
        sessionRefProcessor = mock(DotAuthSessionCredentialProcessorImpl.class);
        final UserAPI userAPI = mock(UserAPI.class);
        try {
            when(userAPI.getAnonymousUserNoThrow()).thenReturn(user(UserAPI.CMS_ANON_USER_ID));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        final ApiProvider provider = mock(ApiProvider.class);
        when(provider.userAPI()).thenReturn(userAPI);
        when(provider.userWebAPI()).thenReturn(userWebAPI);
        apis = mockStatic(APILocator.class);
        apis.when(APILocator::getUserAPI).thenReturn(userAPI);
        sessions = mockStatic(DotAuthSessionCredentialProcessorImpl.class);
        sessions.when(DotAuthSessionCredentialProcessorImpl::getInstance).thenReturn(sessionRefProcessor);
        resource = new WebResource(provider, jwtProcessor);
    }

    /** Closes static mocks and restores the thread-bound principal. */
    @After
    public void tearDown() {
        apis.close();
        sessions.close();
        PrincipalThreadLocal.setName(previousPrincipal);
    }

    private void assertRefused(final HttpServletRequest request) {
        final SecurityException denied = assertThrows(SecurityException.class,
                () -> resource.authenticate(request, mock(HttpServletResponse.class), Collections.emptyMap(),
                        AnonymousAccess.NONE, WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL));
        assertEquals(401, denied.getResponse().getStatus());
        verify(request, never()).setAttribute(eq(WebKeys.USER), any());
    }

    /** A bearer token whose user is the built-in account is not an identity. */
    @Test
    public void token_for_the_built_in_account_is_refused() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtProcessor.processAuthHeaderFromJWT(request)).thenReturn(user(UserAPI.SYSTEM_USER_ID));
        assertRefused(request);
    }

    /** A session reference resolving to the built-in account is not an identity. */
    @Test
    public void session_ref_for_the_built_in_account_is_refused() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(sessionRefProcessor.processAuthHeaderFromSessionRef(request)).thenReturn(user(UserAPI.SYSTEM_USER_ID));
        assertRefused(request);
    }

    /** A back-end session user that is the built-in account is not an identity. */
    @Test
    public void back_end_session_user_for_the_built_in_account_is_refused() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(user(UserAPI.SYSTEM_USER_ID));
        assertRefused(request);
    }

    /** A front-end session user that is the built-in account is not an identity. */
    @Test
    public void front_end_session_user_for_the_built_in_account_is_refused() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(userWebAPI.getLoggedInFrontendUser(request)).thenReturn(user(UserAPI.SYSTEM_USER_ID));
        assertRefused(request);
    }

    /** Control: a real user from the token is still authenticated and stored on the request. */
    @Test
    public void token_for_a_real_user_is_unchanged() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final User real = user("dotcms.org.1");
        when(jwtProcessor.processAuthHeaderFromJWT(request)).thenReturn(real);
        assertSame(real, resource.authenticate(request, mock(HttpServletResponse.class), Collections.emptyMap(),
                AnonymousAccess.NONE, WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL));
        verify(request).setAttribute(WebKeys.USER, real);
    }
}
