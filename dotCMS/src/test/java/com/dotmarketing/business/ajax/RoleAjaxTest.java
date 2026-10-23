package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the admin gates on {@link RoleAjax} role assignment and
 * removal. Non-admin callers must be rejected before any role mutation
 * happens, even when they hold the roles-portlet permission. Also covers the
 * refusal to delete the Getting Started section (#37353).
 */
public class RoleAjaxTest {

    private RoleAjax newRoleAjax() {
        // The default constructor resolves APIs through APILocator, which is not
        // initialized in a unit environment ("No Company!").
        return new RoleAjax(mock(SystemEventsAPI.class), mock(PortletAPI.class),
                mock(UserWebAPI.class));
    }

    @Test
    public void getUsersByRole_rejectsCallerWithoutMembershipPrivilege() {
        final User caller = mock(User.class);
        when(caller.getUserId()).thenReturn("plain-user");

        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class)) {
            dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(caller);
            dwrUtil.when(() -> DwrUtil.validateRoleMembershipPermissions(caller))
                    .thenThrow(new DotSecurityException("not authorized"));

            assertThrows(DotSecurityException.class,
                    () -> newRoleAjax().getUsersByRole(new String[] {"some-role"}));
        }
    }

    @Test
    public void addUserToRole_rejectsNonAdmin() {
        final User nonAdmin = mock(User.class);
        when(nonAdmin.isAdmin()).thenReturn(false);
        when(nonAdmin.getUserId()).thenReturn("non-admin-user");

        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<SecurityLogger> ignored = mockStatic(SecurityLogger.class)) {
            dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(nonAdmin);
            // validateRolesPortletPermissions passes (portlet access alone must not
            // be enough to grant roles) — the admin gate must still reject.

            assertThrows(DotSecurityException.class,
                    () -> newRoleAjax().addUserToRole("some-user", "some-role"));
        }
    }

    @Test
    public void removeUsersFromRole_rejectsNonAdmin() {
        final User nonAdmin = mock(User.class);
        when(nonAdmin.isAdmin()).thenReturn(false);
        when(nonAdmin.getUserId()).thenReturn("non-admin-user");

        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<SecurityLogger> ignored = mockStatic(SecurityLogger.class)) {
            dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(nonAdmin);

            assertThrows(DotSecurityException.class,
                    () -> newRoleAjax().removeUsersFromRole(
                            new String[] {"some-user"}, "some-role"));
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

    /**
     * Logs in a back-end user who is not a CMS administrator and holds, or not, the Roles portlet.
     * {@link DwrUtil} is fully mocked, so the portlet check only fails when it is told to.
     */
    private User givenRolesUser(final MockedStatic<DwrUtil> dwrUtil, final boolean rolesPortlet) {
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(false);
        when(user.getUserId()).thenReturn("roles-user");
        dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(user);
        if (!rolesPortlet) {
            dwrUtil.when(() -> DwrUtil.validateRolesPortletPermissions(user))
                    .thenThrow(new DotSecurityException("no access to the roles portlet"));
            dwrUtil.when(() -> DwrUtil.validatePortletPermissions("roles", user))
                    .thenThrow(new DotSecurityException("no access to the roles portlet"));
        }
        return user;
    }

    /**
     * Given: a back-end user without the Roles portlet
     * When: getAllLayouts is called
     * Then: a DotSecurityException is thrown and no layout is looked up
     */
    @Test
    public void getAllLayouts_requiresRolesPortlet() throws Exception {
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            givenRolesUser(dwrUtil, false);
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);

            assertThrows(DotSecurityException.class, () -> newRoleAjax().getAllLayouts());
            verify(layoutAPI, never()).findAllLayouts();
        }
    }

    /**
     * Given: a back-end user without the Roles portlet
     * When: loadRoleLayouts is called for a role
     * Then: a DotSecurityException is thrown and neither the role nor its layouts are looked up
     */
    @Test
    public void loadRoleLayouts_requiresRolesPortlet() throws Exception {
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            givenRolesUser(dwrUtil, false);
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertThrows(DotSecurityException.class, () -> newRoleAjax().loadRoleLayouts("some-role"));
            verify(roleAPI, never()).loadRoleById(anyString());
            verify(layoutAPI, never()).loadLayoutsForRole(any());
        }
    }

    /**
     * Given: a back-end user with the Roles portlet who is not a CMS administrator
     * When: getAllLayouts is called
     * Then: every layout is returned with its portlet titles
     */
    @Test
    public void getAllLayouts_allowsRolesPortletHolder() throws Exception {
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final Layout layout = mock(Layout.class);
        final Map<String, Object> layoutMap = new HashMap<>();
        layoutMap.put("id", "layout-id");
        when(layout.toMap()).thenReturn(layoutMap);
        when(layout.getPortletIds()).thenReturn(List.of("roles"));
        when(layoutAPI.findAllLayouts()).thenReturn(List.of(layout));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);

        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class);
                MockedStatic<WebAPILocator> webAPILocator = mockStatic(WebAPILocator.class);
                MockedStatic<WebContextFactory> webContextFactory = mockStatic(WebContextFactory.class);
                MockedStatic<LanguageUtil> languageUtil = mockStatic(LanguageUtil.class)) {
            final User user = givenRolesUser(dwrUtil, true);
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
            webContextFactory.when(WebContextFactory::get).thenReturn(webContext);
            webAPILocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);
            when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
            languageUtil.when(() -> LanguageUtil.get(user, "com.dotcms.repackage.javax.portlet.title.roles"))
                    .thenReturn("Roles");

            final List<Map<String, Object>> layouts = newRoleAjax().getAllLayouts();

            assertEquals(1, layouts.size());
            assertEquals("layout-id", layouts.get(0).get("id"));
            assertEquals(List.of("Roles"), layouts.get(0).get("portletTitles"));
        }
    }
}
