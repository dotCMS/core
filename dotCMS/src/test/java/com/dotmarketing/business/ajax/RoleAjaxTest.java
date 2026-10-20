package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertThrows;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.business.web.UserWebAPI;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.MockedStatic;

/**
 * Unit tests for the admin gates on {@link RoleAjax} role assignment and
 * removal, role layouts, layouts and role permissions. Non-admin callers must
 * be rejected before any change happens, even when they hold the
 * roles-portlet permission. Also covers the refusal to delete the Getting
 * Started section (#37353).
 */
public class RoleAjaxTest {

    private RoleAjax newRoleAjax() {
        // The default constructor resolves APIs through APILocator, which is not
        // initialized in a unit environment ("No Company!").
        return new RoleAjax(mock(SystemEventsAPI.class), mock(PortletAPI.class),
                mock(UserWebAPI.class));
    }

    @Test
    public void addUserToRole_rejectsNonAdmin() {
        assertNonAdminRejectedAndLogged(
                () -> newRoleAjax().addUserToRole("some-user", "some-role"));
    }

    @Test
    public void removeUsersFromRole_rejectsNonAdmin() {
        assertNonAdminRejectedAndLogged(() -> newRoleAjax().removeUsersFromRole(
                new String[] {"some-user"}, "some-role"));
    }

    @Test
    public void saveRoleLayouts_rejectsNonAdmin() {
        assertNonAdminRejectedAndLogged(() -> newRoleAjax().saveRoleLayouts(
                "some-role", new String[] {"some-layout"}));
    }

    @Test
    public void updateLayout_rejectsNonAdmin() {
        assertNonAdminRejectedAndLogged(() -> newRoleAjax().updateLayout(
                "some-layout", "name", "description", 0, List.of("roles")));
    }

    @Test
    public void deleteLayout_rejectsNonAdmin() {
        assertNonAdminRejectedAndLogged(() -> newRoleAjax().deleteLayout("some-layout"));
    }

    @Test
    public void saveRolePermission_rejectsNonAdmin() {
        assertNonAdminRejectedAndLogged(() -> newRoleAjax().saveRolePermission(
                "some-role", "some-site", Map.of("individual", "1"), false));
    }

    /**
     * Runs {@code call} as a non-admin who passes the roles-portlet check and
     * asserts it is refused with {@link DotSecurityException} and recorded as a
     * warning in the security log, with the user and remote address.
     */
    private void assertNonAdminRejectedAndLogged(final ThrowingRunnable call) {
        final User nonAdmin = mock(User.class);
        when(nonAdmin.isAdmin()).thenReturn(false);
        when(nonAdmin.getUserId()).thenReturn("non-admin-user");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteHost()).thenReturn("10.0.0.7");

        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class)) {
            // validateRolesPortletPermissions passes (portlet access alone must not
            // be enough) — the admin gate must still reject.
            dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(nonAdmin);
            dwrUtil.when(DwrUtil::getHttpServletRequest).thenReturn(request);

            assertThrows(DotSecurityException.class, call);
            securityLogger.verify(() -> SecurityLogger.logWarn(eq(RoleAjax.class),
                    and(contains("non-admin-user"), contains("10.0.0.7"))));
        }
    }

    /**
     * Method to test: {@link RoleAjax#deleteLayout(String)}
     * Given: an admin deletes the Getting Started section (its fixed id) from the Roles &amp; Tools screen
     * Expected: refused before any section is loaded or removed
     */
    @Test
    public void deleteLayout_gettingStarted_refused() {
        final User admin = mock(User.class);
        when(admin.isAdmin()).thenReturn(true);

        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class)) {
            dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(admin);

            assertThrows(DotStateException.class,
                    () -> newRoleAjax().deleteLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID));
        }
    }
}
