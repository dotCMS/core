package com.dotcms.rest.api.v1.portlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ToolGroupResource}.
 * Verifies that the {@code /_addtouser} endpoint enforces CMS Administrator access,
 * closing the privilege-escalation path documented in private-issues#642.
 */
public class ToolGroupResourceTest extends IntegrationTestBase {

    private static ToolGroupResource resource;
    private static HttpServletResponse mockResponse;
    private static User adminUser;
    private static User backendUser;
    private static Layout testLayout;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new ToolGroupResource();
        mockResponse = new MockHttpResponse().response();

        adminUser = TestUserUtils.getAdminUser();

        backendUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), backendUser);

        testLayout = new LayoutDataGen().name("ToolGroupResourceTest-layout").nextPersisted();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (testLayout != null) {
            try {
                APILocator.getRoleAPI().removeLayoutFromRole(
                        testLayout, adminUser.getUserRole());
            } catch (Exception e) {
                // layout may not have been associated if the admin test did not run
            }
            APILocator.getLayoutAPI().removeLayout(testLayout);
        }
        if (backendUser != null) {
            APILocator.getUserAPI().delete(backendUser, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: {@link ToolGroupResource#addToolGroupToUser}
     * Given Scenario: A low-privilege backend user (non-admin) calls {@code PUT /{layoutId}/_addtouser}
     * Expected Result: A {@link SecurityException} is thrown and the user does NOT gain the layout,
     *                  closing the first step of private-issues#642 privilege escalation chain.
     */
    @Test(expected = SecurityException.class)
    public void test_addToolGroupToUser_lowPrivilegeUser_throwsSecurityException() throws Exception {
        final HttpServletRequest request = requestFor(backendUser);
        resource.addToolGroupToUser(request, mockResponse, testLayout.getId(), null);
    }

    /**
     * Method to test: {@link ToolGroupResource#addToolGroupToUser}
     * Given Scenario: A CMS Administrator calls {@code PUT /{layoutId}/_addtouser}
     * Expected Result: HTTP 200 OK is returned and the layout is added to the admin's user role.
     */
    @Test
    public void test_addToolGroupToUser_adminUser_succeeds() throws Exception {
        final HttpServletRequest request = requestFor(adminUser);
        final Response response = resource.addToolGroupToUser(
                request, mockResponse, testLayout.getId(), null);

        assertNotNull("Response must not be null", response);
        assertEquals("Admin should get HTTP 200", Response.Status.OK.getStatusCode(),
                response.getStatus());
    }

    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/toolgroups").request()
        ).request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }
}
