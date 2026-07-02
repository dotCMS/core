package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.servlet.SessionMessages;
import java.util.LinkedHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Regression tests for the privilege-escalation fix in {@link RoleAjax#addUserToRole}.
 *
 * The attack chain from private-issues#642 requires that a non-admin backend user first
 * self-assigns a layout that includes the "roles" portlet via the {@code _addtouser} endpoint
 * (now blocked), and then calls {@code addUserToRole} to add themselves to CMS Administrator.
 * This test verifies that step 2 is independently blocked by the {@code getAdminUser()} check,
 * even when the caller already holds roles-portlet access.
 */
public class RoleAjaxSecurityTest extends IntegrationTestBase {

    private static User backendUser;
    private static Layout rolesPortletLayout;
    private static String cmsAdminRoleId;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        backendUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), backendUser);

        // Simulate the post-step-1 state: give the backend user access to the roles portlet
        // (this is what _addtouser would have provided before it was hardened).
        rolesPortletLayout = new LayoutDataGen().name("RoleAjaxSecurityTest-roles-layout")
                .portletIds("roles", "users")
                .nextPersisted();
        APILocator.getRoleAPI().addLayoutToRole(rolesPortletLayout, backendUser.getUserRole());

        assertTrue("backend user should now have roles portlet access",
                APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("roles", backendUser));

        cmsAdminRoleId = APILocator.getRoleAPI().loadCMSAdminRole().getId();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (rolesPortletLayout != null) {
            APILocator.getRoleAPI().removeLayoutFromRole(rolesPortletLayout,
                    backendUser.getUserRole());
            APILocator.getLayoutAPI().removeLayout(rolesPortletLayout);
        }
        if (backendUser != null) {
            APILocator.getUserAPI().delete(backendUser, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: {@link RoleAjax#addUserToRole}
     * Given Scenario: A low-privilege backend user has already obtained roles-portlet access
     *                 (simulating a successful step 1), then calls the DWR endpoint to add
     *                 themselves to the CMS Administrator role (step 2 of the attack chain).
     * Expected Result: {@link DotSecurityException} is thrown by the {@code getAdminUser()}
     *                  admin check — the self-escalation is blocked.
     */
    @Test(expected = DotSecurityException.class)
    public void test_addUserToRole_withRolesPortletAccess_cannotSelfEscalateToAdmin()
            throws Exception {
        setUpDwrContext(backendUser);

        final RoleAjax roleAjax = new RoleAjax();
        roleAjax.addUserToRole(backendUser.getUserId(), cmsAdminRoleId);
    }

    private static void setUpDwrContext(final User user) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionMessages.KEY)).thenReturn(new LinkedHashMap<>());

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);

        final WebContextFactory.WebContextBuilder builderMock =
                mock(WebContextFactory.WebContextBuilder.class);
        when(builderMock.get()).thenReturn(webContext);

        final com.dotcms.repackage.org.directwebremoting.Container containerMock =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(containerMock.getBean(WebContextFactory.WebContextBuilder.class))
                .thenReturn(builderMock);

        WebContextFactory.attach(containerMock);
    }
}
