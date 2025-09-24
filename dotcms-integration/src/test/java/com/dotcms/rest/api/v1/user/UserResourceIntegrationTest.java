package com.dotcms.rest.api.v1.user;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotcms.rest.ResponseEntityView;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.internal.util.Base64;
import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class UserResourceIntegrationTest {

    static HttpServletResponse response;
    static HttpServletRequest request;
    static UserResource resource;
    static User user;
    static Host host;
    static User adminUser;
    
    // Additional test data for permissions testing
    static User permissionTestUser;
    static Host permissionTestHost;
    static Folder permissionTestFolder1;
    static Folder permissionTestFolder2;
    static User limitedUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        resource = new UserResource();
        adminUser = TestUserUtils.getAdminUser();
        host = new SiteDataGen().nextPersisted();
        user = TestUserUtils.getChrisPublisherUser(host);
        response = new MockHttpResponse();

        //Check if role has any layout, if is empty add one
        if(APILocator.getLayoutAPI().loadLayoutsForUser(user).isEmpty()) {
            APILocator.getRoleAPI()
                    .addLayoutToRole(APILocator.getLayoutAPI().findAllLayouts().get(0),
                            APILocator.getRoleAPI().getUserRole(user));
        }
        //Add permissions to the host
        final Permission readPermissionsPermission = new Permission( host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(user).getId(), PermissionAPI.PERMISSION_READ, true );
        APILocator.getPermissionAPI().save(readPermissionsPermission,host,adminUser,false);

        // Setup comprehensive permission test data
        setupPermissionTestData();
    }
    
    private static void setupPermissionTestData() throws Exception {
        // Create test host and folders with comprehensive permissions
        permissionTestHost = new SiteDataGen().nextPersisted();
        permissionTestFolder1 = new FolderDataGen()
                .site(permissionTestHost)
                .title("test-folder-1")
                .nextPersisted();
        permissionTestFolder2 = new FolderDataGen()
                .site(permissionTestHost)
                .title("test-folder-2")
                .nextPersisted();

        // Create test users
        permissionTestUser = TestUserUtils.getChrisPublisherUser(permissionTestHost);
        limitedUser = UserTestUtil.getUser("limiteduser", false, true);
        
        // Give limited user backend access role (required for REST API access)
        Role backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        if (!APILocator.getRoleAPI().doesUserHaveRole(limitedUser, backendRole)) {
            APILocator.getRoleAPI().addRoleToUser(backendRole, limitedUser);
        }

        // Set up comprehensive permissions
        Role userRole = APILocator.getRoleAPI().getUserRole(permissionTestUser);

        // Host permissions: READ, WRITE, PUBLISH
        Permission hostPerms = new Permission(
                permissionTestHost.getPermissionId(),
                userRole.getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH,
                true
        );
        APILocator.getPermissionAPI().save(hostPerms, permissionTestHost, adminUser, false);

        // Folder1: READ, WRITE, CAN_ADD_CHILDREN  
        Permission folder1Perms = new Permission(
                permissionTestFolder1.getInode(),
                userRole.getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,
                true
        );
        APILocator.getPermissionAPI().save(folder1Perms, permissionTestFolder1, adminUser, false);

        // Folder2: READ only
        Permission folder2Perms = new Permission(
                permissionTestFolder2.getInode(),
                userRole.getId(),
                PermissionAPI.PERMISSION_READ,
                true
        );
        APILocator.getPermissionAPI().save(folder2Perms, permissionTestFolder2, adminUser, false);
    }

    private static HttpServletRequest mockRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest(host.getHostname(), "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST,host);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID,host.getIdentifier());

        return request;
    }

    private void loginAs() throws Exception {
        final LoginAsForm loginAsForm = new LoginAsForm.Builder().userId(user.getUserId()).build();
        request = mockRequest();
        final Response resourceResponse = resource.loginAs(request,response,loginAsForm);
        assertNotNull(resourceResponse);
        assertEquals(Status.OK.getStatusCode(),resourceResponse.getStatus());
        assertEquals(user.getUserId(),request.getSession().getAttribute(WebKeys.USER_ID));
        assertNull(request.getSession().getAttribute(WebKeys.USER));
        assertEquals(adminUser.getUserId(),request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID));
    }

    @Test
    public void test_loginAs_success() throws Exception{
        loginAs();
    }

    @Test
    public void test_logoutAs_success() throws Exception {
        loginAs();
        final Response resourceResponse = resource.logoutAs(request,response);
        assertNotNull(resourceResponse);
        assertEquals(Status.OK.getStatusCode(),resourceResponse.getStatus());
        assertEquals(adminUser.getUserId(),request.getSession().getAttribute(WebKeys.USER_ID));
        assertNull(request.getSession().getAttribute(WebKeys.USER));
        assertNull(request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID));
    }

    @Test
    public void test_getUserPermissions_adminAccessingOtherUser_success() throws Exception {
        // Admin can view any user's permissions
        HttpServletRequest request = mockRequest(); // Already authenticated as admin
        Response response = resource.getUserPermissions(request, this.response, permissionTestUser.getUserId());
        
        assertNotNull(response);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof ResponseEntityView);
        
        ResponseEntityView responseView = (ResponseEntityView) response.getEntity();
        Map<String, Object> responseData = (Map<String, Object>) responseView.getEntity();
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) responseData.get("assets");
        
        // Validate structure
        assertNotNull(permissions);
        assertTrue(permissions.size() >= 3); // At least host + 2 folders
        
        // Validate specific assets
        validateHostPermissions(permissions);
        validateFolderPermissions(permissions);
    }

    @Test
    public void test_getUserPermissions_userAccessingSelf_success() throws Exception {
        // User can view their own permissions
        MockHeaderRequest request = new MockHeaderRequest(
            new MockSessionRequest(
                new MockAttributeRequest(
                    new MockHttpRequestIntegrationTest(permissionTestHost.getHostname(), "/").request()
                ).request()
            ).request()
        );
        
        // Authenticate as permissionTestUser
        String auth = permissionTestUser.getEmailAddress() + ":" + "password";
        request.setHeader("Authorization", "Basic " + new String(Base64.encode(auth.getBytes())));
        request.getSession().setAttribute(WebKeys.USER_ID, permissionTestUser.getUserId());
        
        Response response = resource.getUserPermissions(request, this.response, permissionTestUser.getUserId());
        
        assertNotNull(response);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        ResponseEntityView responseView = (ResponseEntityView) response.getEntity();
        Map<String, Object> responseData = (Map<String, Object>) responseView.getEntity();
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) responseData.get("assets");
        
        assertNotNull(permissions);
        assertTrue("Should have permissions", !permissions.isEmpty());
    }

    @Test
    public void test_getUserPermissions_userAccessingOther_forbidden() throws Exception {
        // Regular user cannot view other user's permissions
        MockHeaderRequest request = new MockHeaderRequest(
            new MockSessionRequest(
                new MockAttributeRequest(
                    new MockHttpRequestIntegrationTest(host.getHostname(), "/").request()
                ).request()
            ).request()
        );
        
        // Set up session for limited user (non-admin)
        request.getSession().setAttribute(WebKeys.USER_ID, limitedUser.getUserId());
        request.getSession().setAttribute(WebKeys.USER, limitedUser);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
        
        try {
            resource.getUserPermissions(request, this.response, permissionTestUser.getUserId());
            fail("Should have thrown ForbiddenException");
        } catch (Exception e) {
            assertTrue("Should be forbidden exception", 
                e instanceof ForbiddenException || 
                (e.getCause() != null && e.getCause() instanceof ForbiddenException));
        }
    }

    @Test
    public void test_getUserPermissions_systemHostAlwaysIncluded() throws Exception {
        // System host should always be in response
        HttpServletRequest request = mockRequest();
        Response response = resource.getUserPermissions(request, this.response, limitedUser.getUserId());
        
        assertNotNull(response);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        ResponseEntityView responseView = (ResponseEntityView) response.getEntity();
        Map<String, Object> responseData = (Map<String, Object>) responseView.getEntity();
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) responseData.get("assets");
        
        boolean hasSystemHost = permissions.stream()
            .anyMatch(p -> "HOST".equals(p.get("type")) && 
                          "System Host".equals(p.get("name")));
        
        assertTrue("System host must be included", hasSystemHost);
    }

    // Helper validation methods
    private void validateHostPermissions(List<Map<String, Object>> permissions) {
        Map<String, Object> hostPerm = permissions.stream()
            .filter(p -> "HOST".equals(p.get("type")) && 
                        permissionTestHost.getIdentifier().equals(p.get("id")))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Should have test host permissions", hostPerm);
        assertEquals(permissionTestHost.getHostname(), hostPerm.get("name"));
        
        Map<String, List<String>> perms = (Map<String, List<String>>) hostPerm.get("permissions");
        assertNotNull(perms);
        
        List<String> individualPerms = perms.get("INDIVIDUAL");
        assertNotNull(individualPerms);
        assertTrue(individualPerms.contains("READ"));
        assertTrue(individualPerms.contains("WRITE"));
        assertTrue(individualPerms.contains("PUBLISH"));
    }

    private void validateFolderPermissions(List<Map<String, Object>> permissions) {
        // Validate folder1 permissions
        Map<String, Object> folder1Perm = permissions.stream()
            .filter(p -> "FOLDER".equals(p.get("type")) && 
                        permissionTestFolder1.getInode().equals(p.get("id")))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Should have folder1 permissions", folder1Perm);
        Map<String, List<String>> perms1 = (Map<String, List<String>>) folder1Perm.get("permissions");
        List<String> individualPerms1 = perms1.get("INDIVIDUAL");
        assertTrue(individualPerms1.contains("READ"));
        assertTrue(individualPerms1.contains("WRITE"));
        assertTrue(individualPerms1.contains("CAN_ADD_CHILDREN"));
        
        // Validate folder2 permissions
        Map<String, Object> folder2Perm = permissions.stream()
            .filter(p -> "FOLDER".equals(p.get("type")) && 
                        permissionTestFolder2.getInode().equals(p.get("id")))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Should have folder2 permissions", folder2Perm);
        Map<String, List<String>> perms2 = (Map<String, List<String>>) folder2Perm.get("permissions");
        List<String> individualPerms2 = perms2.get("INDIVIDUAL");
        assertTrue(individualPerms2.contains("READ"));
        assertFalse(individualPerms2.contains("WRITE"));
    }
}
