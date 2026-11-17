package com.dotcms.cms.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.adminsite.AdminSiteAPI;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.lang.reflect.Constructor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/** Exercises login policy before a session or cookie can be issued. */
class LoginAdminSiteTest {

    private static final String USER_ID = "editor";
    private static final String PASSWORD = "test-password";
    private static final String AUTHENTICATION_REACHED = "session creation reached";

    private enum Flow { PASSWORD, EMAIL, COOKIE }
    private enum Host { ADMIN, PUBLIC }
    private enum Transport { SECURE, INSECURE }
    private enum Account { BACKEND, FRONTEND, MIXED, ADMIN }
    private enum Outcome { REJECT, AUTHENTICATE }

    /** Password and SSO logins enforce the same role, host and transport policy before session creation. */
    @ParameterizedTest
    @CsvSource({
            "PASSWORD, PUBLIC, SECURE, MIXED, REJECT",
            "PASSWORD, ADMIN, INSECURE, BACKEND, REJECT",
            "PASSWORD, ADMIN, SECURE, BACKEND, AUTHENTICATE",
            "PASSWORD, PUBLIC, SECURE, FRONTEND, AUTHENTICATE",
            "PASSWORD, ADMIN, INSECURE, ADMIN, REJECT",
            "EMAIL, ADMIN, SECURE, BACKEND, AUTHENTICATE",
            "COOKIE, PUBLIC, SECURE, MIXED, REJECT",
            "COOKIE, ADMIN, INSECURE, BACKEND, REJECT",
            "COOKIE, ADMIN, SECURE, BACKEND, AUTHENTICATE"
    })
    void loginHonorsAdminPolicy(final Flow flow, final Host host, final Transport transport,
            final Account account, final Outcome outcome) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isSecure()).thenReturn(transport == Transport.SECURE);
        // Stop at session creation: permitted cases must reach it; rejected cases must not.
        when(request.getSession()).thenThrow(new IllegalStateException(AUTHENTICATION_REACHED));

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.getEmailAddress()).thenReturn("editor@example.com");
        when(user.isActive()).thenReturn(true);
        when(user.isFrontendUser()).thenReturn(account != Account.BACKEND);
        when(user.isBackendUser()).thenReturn(host == Host.ADMIN
                && (account == Account.BACKEND || account == Account.MIXED));
        when(user.isAdmin()).thenReturn(host == Host.ADMIN && account == Account.ADMIN);
        final User systemUser = mock(User.class);
        when(systemUser.getUserId()).thenReturn("system");
        when(systemUser.getEmailAddress()).thenReturn("system@example.com");
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user);
        when(userAPI.loadUserById(USER_ID, systemUser, false)).thenReturn(user);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        final Role backendRole = new Role();
        backendRole.setId("backend-role");
        when(roleAPI.loadBackEndUserRole()).thenReturn(backendRole);
        when(roleAPI.doesUserHaveRole(user, backendRole))
                .thenReturn(account == Account.BACKEND || account == Account.MIXED);
        final Role adminRole = new Role();
        adminRole.setId("admin-role");
        when(roleAPI.loadCMSAdminRole()).thenReturn(adminRole);
        when(roleAPI.doesUserHaveRole(user, adminRole)).thenReturn(account == Account.ADMIN);
        final Company company = mock(Company.class);
        when(company.getCompanyId()).thenReturn("dotcms.org");
        when(company.getAuthType()).thenReturn(flow == Flow.EMAIL ? Company.AUTH_TYPE_EA : Company.AUTH_TYPE_ID);
        final AdminSiteAPI adminSite = mock(AdminSiteAPI.class, CALLS_REAL_METHODS);
        when(adminSite.isAdminSiteEnabled()).thenReturn(true);
        when(adminSite.isAdminSite(request)).thenReturn(host == Host.ADMIN);
        when(adminSite.allowInsecureRequests()).thenReturn(false);
        when(adminSite.allowBackendLoginsOnNonAdminSites()).thenReturn(false);
        final PreventSessionFixationUtil sessions = mock(PreventSessionFixationUtil.class);
        when(sessions.preventSessionFixation(request, true)).thenReturn(mock(HttpSession.class));

        try (MockedStatic<APILocator> locator = mockStatic(APILocator.class);
                MockedStatic<PortalUtil> portal = mockStatic(PortalUtil.class);
                MockedStatic<UserManagerUtil> users = mockStatic(UserManagerUtil.class);
                MockedStatic<PublicEncryptionFactory> encryption = mockStatic(PublicEncryptionFactory.class);
                MockedStatic<PreventSessionFixationUtil> fixation = mockStatic(PreventSessionFixationUtil.class)) {
            locator.when(APILocator::systemUser).thenReturn(systemUser);
            locator.when(APILocator::getUserAPI).thenReturn(userAPI);
            locator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
            locator.when(APILocator::getAdminSiteAPI).thenReturn(adminSite);
            portal.when(() -> PortalUtil.getCompany(request)).thenReturn(company);
            if (flow == Flow.COOKIE && host == Host.PUBLIC) {
                // SSO must check its incoming identity, not an existing frontend-only session.
                final User frontendSession = mock(User.class);
                when(frontendSession.getUserId()).thenReturn("frontend-session");
                when(frontendSession.isActive()).thenReturn(true);
                portal.when(() -> PortalUtil.getUserId(request)).thenReturn("frontend-session");
                when(userAPI.loadUserById("frontend-session", systemUser, false)).thenReturn(frontendSession);
            }
            users.when(() -> UserManagerUtil.authenticateByUserId("dotcms.org", USER_ID, PASSWORD))
                    .thenReturn(Authenticator.SUCCESS);
            users.when(() -> UserManagerUtil.authenticateByEmailAddress("dotcms.org", "editor@example.com", PASSWORD))
                    .thenReturn(Authenticator.SUCCESS);
            users.when(() -> UserManagerUtil.getUserId("dotcms.org", "editor@example.com")).thenReturn(USER_ID);
            encryption.when(() -> PublicEncryptionFactory.decryptString(USER_ID)).thenReturn(USER_ID);
            fixation.when(PreventSessionFixationUtil::getInstance).thenReturn(sessions);
            final LoginServiceAPI login = loginService(userAPI);

            if (flow == Flow.COOKIE) {
                assertEquals(outcome == Outcome.AUTHENTICATE, login.doCookieLogin(USER_ID, request, response));
                if (outcome == Outcome.REJECT) {
                    verify(sessions, never()).preventSessionFixation(request, true);
                }
                return;
            }

            final String loginName = flow == Flow.EMAIL ? "editor@example.com" : USER_ID;
            if (outcome == Outcome.REJECT) {
                assertFalse(login.doActionLogin(loginName, PASSWORD, false, request, response));
                verify(request, never()).getSession();
                return;
            }

            final AuthException stopped = assertThrows(AuthException.class,
                    () -> login.doActionLogin(loginName, PASSWORD, false, request, response));
            assertEquals(AUTHENTICATION_REACHED, stopped.getMessage());
            verify(request).getSession();
        }
    }

    /** Uses the existing dependency-injection constructor without widening the private implementation. */
    private LoginServiceAPI loginService(final UserAPI userAPI) throws Exception {
        final Class<?> implementation = Class.forName(LoginServiceAPIFactory.class.getName() + "$LoginServiceImpl");
        final Constructor<?> constructor = implementation.getDeclaredConstructor(LoginServiceAPIFactory.class,
                ApiProvider.class, JsonWebTokenUtils.class, HttpServletRequestThreadLocal.class, UserAPI.class);
        constructor.setAccessible(true);
        return (LoginServiceAPI) constructor.newInstance(LoginServiceAPIFactory.getInstance(),
                mock(ApiProvider.class), mock(JsonWebTokenUtils.class), HttpServletRequestThreadLocal.INSTANCE, userAPI);
    }
}
