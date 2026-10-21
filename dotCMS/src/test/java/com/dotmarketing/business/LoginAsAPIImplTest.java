package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.system.AppContext;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link LoginAsAPIImpl}: the principal user is reported only from the Login As
 * record that the Login As endpoint stores, and only while it matches the current session user.
 */
public class LoginAsAPIImplTest {

    private static final String PRINCIPAL_ID = "dotcms.org.1";
    private static final String LOGIN_AS_ID = "impersonated-user";

    private UserAPI userAPI;
    private AppContext appContext;
    private LoginAsAPIImpl loginAsAPI;

    @Before
    public void setUp() {
        userAPI = mock(UserAPI.class);
        appContext = mock(AppContext.class);
        loginAsAPI = new LoginAsAPIImpl(userAPI);
        when(appContext.getAttribute(WebKeys.USER_ID)).thenReturn(LOGIN_AS_ID);
    }

    /**
     * Given Scenario: The session holds the Login As record, and its impersonated user is the
     * current session user.
     * ExpectedResult: Login As is reported as active and the principal user is loaded.
     */
    @Test
    public void getPrincipalUser_withMatchingLoginAsSession_returnsPrincipal() throws Exception {
        final User principal = new User();
        principal.setUserId(PRINCIPAL_ID);
        when(userAPI.loadUserById(PRINCIPAL_ID)).thenReturn(principal);
        when(appContext.getAttribute(LoginAsSession.SESSION_KEY))
                .thenReturn(new LoginAsSession(PRINCIPAL_ID, LOGIN_AS_ID));

        assertTrue(loginAsAPI.isLoginAsUser(appContext));
        assertEquals(PRINCIPAL_ID, loginAsAPI.getPrincipalUser(appContext).getUserId());
    }

    /**
     * Given Scenario: The session holds a plain String under the Login As record key.
     * ExpectedResult: No Login As is reported and no user is loaded.
     */
    @Test
    public void getPrincipalUser_withStringUnderLoginAsSessionKey_returnsNull() throws Exception {
        when(appContext.getAttribute(LoginAsSession.SESSION_KEY)).thenReturn(PRINCIPAL_ID);

        assertFalse(loginAsAPI.isLoginAsUser(appContext));
        assertNull(loginAsAPI.getPrincipalUser(appContext));
        verify(userAPI, never()).loadUserById(anyString());
    }

    /**
     * Given Scenario: The session holds a user ID under PRINCIPAL_USER_ID and no Login As record.
     * ExpectedResult: No Login As is reported and no user is loaded.
     */
    @Test
    public void getPrincipalUser_withPrincipalUserIdAndNoLoginAsSession_returnsNull() throws Exception {
        when(appContext.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn(PRINCIPAL_ID);

        assertFalse(loginAsAPI.isLoginAsUser(appContext));
        assertNull(loginAsAPI.getPrincipalUser(appContext));
        verify(userAPI, never()).loadUserById(anyString());
    }

    /**
     * Given Scenario: The session holds a Login As record whose impersonated user is not the
     * current session user.
     * ExpectedResult: No Login As is reported and no user is loaded.
     */
    @Test
    public void getPrincipalUser_withLoginAsSessionForAnotherUser_returnsNull() throws Exception {
        when(appContext.getAttribute(LoginAsSession.SESSION_KEY))
                .thenReturn(new LoginAsSession(PRINCIPAL_ID, "someone-else"));

        assertFalse(loginAsAPI.isLoginAsUser(appContext));
        assertNull(loginAsAPI.getPrincipalUser(appContext));
        verify(userAPI, never()).loadUserById(anyString());
    }
}
