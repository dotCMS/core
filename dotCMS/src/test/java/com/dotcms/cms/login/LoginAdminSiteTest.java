package com.dotcms.cms.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.lang.reflect.Constructor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/** Exercises login policy before a session or cookie can be issued. */
class LoginAdminSiteTest {

    private static final String USER_ID = "editor";
    private static final String PASSWORD = "test-password";
    private static final String AUTHENTICATION_REACHED = "session creation reached";

    private enum Host { ADMIN, PUBLIC }
    private enum Transport { SECURE, INSECURE }
    private enum Account { BACKEND, FRONTEND, MIXED }
    private enum Outcome { REJECT, AUTHENTICATE }

    /** Backend membership remains restricted even when the account also has a frontend role. */
    @ParameterizedTest
    @CsvSource({
            "PUBLIC, SECURE, MIXED, REJECT",
            "ADMIN, INSECURE, BACKEND, REJECT",
            "ADMIN, SECURE, BACKEND, AUTHENTICATE",
            "PUBLIC, SECURE, FRONTEND, AUTHENTICATE"
    })
    void loginHonorsAdminPolicy(final Host host, final Transport transport,
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
        final User systemUser = mock(User.class);
        when(systemUser.getUserId()).thenReturn("system");
        when(systemUser.getEmailAddress()).thenReturn("system@example.com");
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        final Role backendRole = new Role();
        when(roleAPI.loadBackEndUserRole()).thenReturn(backendRole);
        when(roleAPI.doesUserHaveRole(user, backendRole)).thenReturn(account != Account.FRONTEND);
        final Company company = mock(Company.class);
        when(company.getCompanyId()).thenReturn("dotcms.org");
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
        final AdminSiteAPI adminSite = mock(AdminSiteAPI.class);
        when(adminSite.isAdminSiteEnabled()).thenReturn(true);
        when(adminSite.isAdminSite(request)).thenReturn(host == Host.ADMIN);

        try (MockedStatic<APILocator> locator = mockStatic(APILocator.class);
                MockedStatic<PortalUtil> portal = mockStatic(PortalUtil.class);
                MockedStatic<UserManagerUtil> users = mockStatic(UserManagerUtil.class)) {
            locator.when(APILocator::systemUser).thenReturn(systemUser);
            locator.when(APILocator::getUserAPI).thenReturn(userAPI);
            locator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
            locator.when(APILocator::getAdminSiteAPI).thenReturn(adminSite);
            portal.when(() -> PortalUtil.getCompany(request)).thenReturn(company);
            users.when(() -> UserManagerUtil.authenticateByUserId("dotcms.org", USER_ID, PASSWORD))
                    .thenReturn(Authenticator.SUCCESS);
            final LoginServiceAPI login = loginService(userAPI);

            if (outcome == Outcome.REJECT) {
                assertFalse(login.doActionLogin(USER_ID, PASSWORD, false, request, response));
                verify(request, never()).getSession();
                return;
            }

            final AuthException stopped = assertThrows(AuthException.class,
                    () -> login.doActionLogin(USER_ID, PASSWORD, false, request, response));
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
