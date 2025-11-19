package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.user.UserPermissionHelper;
import com.dotcms.rest.api.v1.user.UserResourceHelper;
import com.dotcms.rest.api.v1.user.UserPermissions;
import com.dotcms.rest.api.v1.user.UserPermissionAsset;
import com.dotcms.rest.api.v1.user.ResponseEntityUserPermissionsView;
import com.dotcms.rest.api.v1.user.SaveUserPermissionsForm;
import com.dotcms.rest.api.v1.user.SaveUserPermissionsResponse;
import com.dotcms.rest.api.v1.user.ResponseEntitySaveUserPermissionsView;
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
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.jersey.internal.util.Base64;
import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for PermissionResource user permissions endpoints.
 * Tests both GET /api/v1/permissions/user/{userId} and
 * PUT /api/v1/permissions/user/{userId}/asset/{assetId}
 */
public class PermissionResourceIntegrationTest {

    static HttpServletResponse response;
    static PermissionResource resource;
    static User adminUser;
    static Host testHost;

    // Test data for GET permissions testing
    static User permissionTestUser;
    static Host permissionTestHost;
    static Folder permissionTestFolder1;
    static Folder permissionTestFolder2;
    static User limitedUser;

    // Test data for PUT permissions testing
    static Host updateTestHost;
    static Folder updateTestFolder;
    static Folder parentFolder;
    static Folder childFolder;
    static User updateTestUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        // Create resource instance
        resource = new PermissionResource(
            new UserPermissionHelper(),
            UserResourceHelper.getInstance()
        );

        adminUser = TestUserUtils.getAdminUser();
        testHost = new SiteDataGen().nextPersisted();
        response = new MockHttpResponse();

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

        // Setup test data for PUT permission tests
        setupUpdatePermissionTestData();
    }

    private static void setupUpdatePermissionTestData() throws Exception {
        // Host for update tests
        updateTestHost = new SiteDataGen().nextPersisted();

        // Folder for update tests
        updateTestFolder = new FolderDataGen()
                .site(updateTestHost)
                .title("update-test-folder")
                .nextPersisted();

        // Parent and child folders for inheritance test
        parentFolder = new FolderDataGen()
                .site(updateTestHost)
                .title("parent-folder")
                .nextPersisted();

        childFolder = new FolderDataGen()
                .site(updateTestHost)
                .parent(parentFolder)
                .title("child-folder")
                .nextPersisted();

        // Ensure child inherits from parent
        APILocator.getPermissionAPI().resetPermissionsUnder(parentFolder);

        // User for update tests
        updateTestUser = UserTestUtil.getUser("updateuser", false, true);
        Role backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        if (!APILocator.getRoleAPI().doesUserHaveRole(updateTestUser, backendRole)) {
            APILocator.getRoleAPI().addRoleToUser(backendRole, updateTestUser);
        }

        // Give initial READ permission on updateTestHost
        Role updateUserRole = APILocator.getRoleAPI().getUserRole(updateTestUser);
        Permission initialPerm = new Permission(
                updateTestHost.getPermissionId(),
                updateUserRole.getId(),
                PermissionAPI.PERMISSION_READ,
                true
        );
        APILocator.getPermissionAPI().save(initialPerm, updateTestHost, adminUser, false);

        // Give limitedUser only READ permission (no EDIT_PERMISSIONS) on updateTestHost
        Role limitedUserRole = APILocator.getRoleAPI().getUserRole(limitedUser);
        Permission limitedPerm = new Permission(
                updateTestHost.getPermissionId(),
                limitedUserRole.getId(),
                PermissionAPI.PERMISSION_READ,
                true
        );
        APILocator.getPermissionAPI().save(limitedPerm, updateTestHost, adminUser, false);
    }

    @After
    public void tearDown() {
        // Clean up HttpServletRequestThreadLocal to prevent test pollution
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    private static HttpServletRequest mockRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, testHost);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, testHost.getIdentifier());

        return request;
    }

    // ==================== GET Permission Tests ====================

    @Test
    public void test_getUserPermissions_adminAccessingOtherUser_success() throws Exception {
        // Admin can view any user's permissions
        HttpServletRequest request = mockRequest();
        ResponseEntityUserPermissionsView responseEntity = resource.getUserPermissions(
            request, response, permissionTestUser.getUserId()
        );

        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getEntity());

        UserPermissions responseData = responseEntity.getEntity();
        List<UserPermissionAsset> permissions = responseData.getAssets();

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

        ResponseEntityUserPermissionsView responseEntity = resource.getUserPermissions(
            request, response, permissionTestUser.getUserId()
        );

        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getEntity());

        UserPermissions responseData = responseEntity.getEntity();
        List<UserPermissionAsset> permissions = responseData.getAssets();

        assertNotNull(permissions);
        assertTrue("Should have permissions", !permissions.isEmpty());
    }

    @Test
    public void test_getUserPermissions_userAccessingOther_forbidden() throws Exception {
        // Regular user cannot view other user's permissions
        MockHeaderRequest request = new MockHeaderRequest(
            new MockSessionRequest(
                new MockAttributeRequest(
                    new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request()
                ).request()
            ).request()
        );

        // Set up session for limited user (non-admin)
        request.getSession().setAttribute(WebKeys.USER_ID, limitedUser.getUserId());
        request.getSession().setAttribute(WebKeys.USER, limitedUser);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, testHost);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, testHost.getIdentifier());

        try {
            resource.getUserPermissions(request, response, permissionTestUser.getUserId());
            fail("Should have thrown DotSecurityException");
        } catch (Exception e) {
            assertTrue("Should be security exception",
                e.getMessage().contains("can only access their own permissions"));
        }
    }

    @Test
    public void test_getUserPermissions_systemHostAlwaysIncluded() throws Exception {
        // System host should always be in response
        HttpServletRequest request = mockRequest();
        ResponseEntityUserPermissionsView responseEntity = resource.getUserPermissions(
            request, response, limitedUser.getUserId()
        );

        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getEntity());

        UserPermissions responseData = responseEntity.getEntity();
        List<UserPermissionAsset> permissions = responseData.getAssets();

        boolean hasSystemHost = permissions.stream()
            .anyMatch(p -> "HOST".equals(p.getType()) &&
                          "System Host".equals(p.getName()));

        assertTrue("System host must be included", hasSystemHost);
    }

    // Helper validation methods
    private void validateHostPermissions(List<UserPermissionAsset> permissions) {
        UserPermissionAsset hostPerm = permissions.stream()
            .filter(p -> "HOST".equals(p.getType()) &&
                        permissionTestHost.getIdentifier().equals(p.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull("Should have test host permissions", hostPerm);
        assertEquals(permissionTestHost.getHostname(), hostPerm.getName());

        Map<String, Set<String>> perms = hostPerm.getPermissions();
        assertNotNull(perms);

        Set<String> individualPerms = perms.get("INDIVIDUAL");
        assertNotNull(individualPerms);
        assertTrue(individualPerms.contains("READ"));
        assertTrue(individualPerms.contains("WRITE"));
        assertTrue(individualPerms.contains("PUBLISH"));
    }

    private void validateFolderPermissions(List<UserPermissionAsset> permissions) {
        // Validate folder1 permissions
        UserPermissionAsset folder1Perm = permissions.stream()
            .filter(p -> "FOLDER".equals(p.getType()) &&
                        permissionTestFolder1.getInode().equals(p.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull("Should have folder1 permissions", folder1Perm);
        Map<String, Set<String>> perms1 = folder1Perm.getPermissions();
        Set<String> individualPerms1 = perms1.get("INDIVIDUAL");
        assertTrue(individualPerms1.contains("READ"));
        assertTrue(individualPerms1.contains("WRITE"));
        assertTrue(individualPerms1.contains("CAN_ADD_CHILDREN"));

        // Validate folder2 permissions
        UserPermissionAsset folder2Perm = permissions.stream()
            .filter(p -> "FOLDER".equals(p.getType()) &&
                        permissionTestFolder2.getInode().equals(p.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull("Should have folder2 permissions", folder2Perm);
        Map<String, Set<String>> perms2 = folder2Perm.getPermissions();
        Set<String> individualPerms2 = perms2.get("INDIVIDUAL");
        assertTrue(individualPerms2.contains("READ"));
        assertFalse(individualPerms2.contains("WRITE"));
    }

    // ==================== PUT Permission Tests ====================

    @Test
    public void test_updateUserPermissions_basicHostUpdate_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Create form with READ, WRITE, PUBLISH permissions
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Set.of("READ", "WRITE", "PUBLISH"));
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        // Execute PUT
        ResponseEntitySaveUserPermissionsView response = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestHost.getIdentifier(), form
        );

        // Assert response
        assertNotNull(response);
        SaveUserPermissionsResponse data = response.getEntity();
        assertNotNull(data);
        assertFalse("Cascade should not be initiated", data.isCascadeInitiated());
        assertEquals(updateTestUser.getUserId(), data.getUserId());

        // Verify asset in response
        UserPermissionAsset asset = data.getAsset();
        assertEquals(updateTestHost.getIdentifier(), asset.getId());
        Set<String> individualPerms = asset.getPermissions().get("INDIVIDUAL");
        assertNotNull(individualPerms);
        assertEquals(3, individualPerms.size());
        assertTrue(individualPerms.containsAll(Set.of("READ", "WRITE", "PUBLISH")));

        // END-TO-END: Verify via GET API
        ResponseEntityUserPermissionsView getResponse = resource.getUserPermissions(
                request, this.response, updateTestUser.getUserId()
        );
        UserPermissions getUserData = getResponse.getEntity();
        UserPermissionAsset hostAsset = getUserData.getAssets().stream()
                .filter(a -> updateTestHost.getIdentifier().equals(a.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull("Host asset should be in GET response", hostAsset);
        Set<String> getPerms = hostAsset.getPermissions().get("INDIVIDUAL");
        assertTrue("GET should show all 3 permissions", getPerms.containsAll(Set.of("READ", "WRITE", "PUBLISH")));
    }

    @Test
    public void test_updateUserPermissions_multipleScopes_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Create form with INDIVIDUAL, HOST, and FOLDER scopes
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Set.of("READ", "WRITE"));
        permissions.put("HOST", Set.of("READ"));
        permissions.put("FOLDER", Set.of("READ", "CAN_ADD_CHILDREN"));
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        // Execute PUT on folder
        ResponseEntitySaveUserPermissionsView response = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestFolder.getInode(), form
        );

        // Assert response
        assertNotNull(response);
        SaveUserPermissionsResponse data = response.getEntity();
        UserPermissionAsset asset = data.getAsset();

        // Verify all 3 scopes present
        Map<String, Set<String>> permMap = asset.getPermissions();
        assertTrue("Should have INDIVIDUAL scope", permMap.containsKey("INDIVIDUAL"));
        assertTrue("Should have HOST scope", permMap.containsKey("HOST"));
        assertTrue("Should have FOLDER scope", permMap.containsKey("FOLDER"));

        // Verify INDIVIDUAL permissions
        assertTrue(permMap.get("INDIVIDUAL").containsAll(Set.of("READ", "WRITE")));

        // Verify HOST permissions
        assertTrue(permMap.get("HOST").contains("READ"));

        // Verify FOLDER permissions
        assertTrue(permMap.get("FOLDER").containsAll(Set.of("READ", "CAN_ADD_CHILDREN")));

        // END-TO-END: Verify via GET API
        ResponseEntityUserPermissionsView getResponse = resource.getUserPermissions(
                request, this.response, updateTestUser.getUserId()
        );
        UserPermissions getUserData = getResponse.getEntity();
        UserPermissionAsset folderAsset = getUserData.getAssets().stream()
                .filter(a -> updateTestFolder.getInode().equals(a.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull("Folder asset should be in GET response", folderAsset);
        assertTrue("GET should show all scopes", folderAsset.getPermissions().keySet().containsAll(Set.of("INDIVIDUAL", "HOST", "FOLDER")));
    }

    @Test
    public void test_updateUserPermissions_breaksInheritance_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // VERIFY inheritance before test (critical assertion)
        assertTrue("Child folder should be inheriting before test",
                APILocator.getPermissionAPI().isInheritingPermissions(childFolder));

        // Execute PUT on inheriting folder
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Set.of("READ", "WRITE"));
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        ResponseEntitySaveUserPermissionsView response = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), childFolder.getInode(), form
        );

        // Assert response successful
        assertNotNull(response);
        SaveUserPermissionsResponse data = response.getEntity();
        assertEquals(childFolder.getInode(), data.getAsset().getId());

        // VERIFY inheritance broken after PUT (critical assertion)
        assertFalse("Child folder should NOT be inheriting after PUT",
                APILocator.getPermissionAPI().isInheritingPermissions(childFolder));

        // Verify permissions set on child
        ResponseEntityUserPermissionsView getResponse = resource.getUserPermissions(
                request, this.response, updateTestUser.getUserId()
        );
        UserPermissions getUserData = getResponse.getEntity();
        UserPermissionAsset childAsset = getUserData.getAssets().stream()
                .filter(a -> childFolder.getInode().equals(a.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull("Child folder should have individual permissions", childAsset);
        assertFalse("Child should not be inheriting", childAsset.isInheritsPermissions());
        assertTrue("Child should have READ and WRITE",
                childAsset.getPermissions().get("INDIVIDUAL").containsAll(Set.of("READ", "WRITE")));
    }

    @Test
    public void test_updateUserPermissions_cascade_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Set ThreadLocal to simulate production environment
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        // Create form with cascade=true
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Set.of("READ", "WRITE"));
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, true);

        // Execute PUT with cascade on parent host
        ResponseEntitySaveUserPermissionsView response = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestHost.getIdentifier(), form
        );

        // Assert cascade was initiated
        assertNotNull(response);
        SaveUserPermissionsResponse data = response.getEntity();
        assertTrue("Cascade should be initiated for parent permissionable", data.isCascadeInitiated());
    }

    @Test
    public void test_updateUserPermissions_replacesExisting_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Setup: Give user READ+WRITE+PUBLISH on updateTestHost
        Map<String, Set<String>> setupPermissions = new HashMap<>();
        setupPermissions.put("INDIVIDUAL", Set.of("READ", "WRITE", "PUBLISH"));
        SaveUserPermissionsForm setupForm = new SaveUserPermissionsForm(setupPermissions, false);
        resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestHost.getIdentifier(), setupForm
        );

        // Verify setup worked
        ResponseEntityUserPermissionsView getResponse1 = resource.getUserPermissions(
                request, this.response, updateTestUser.getUserId()
        );
        UserPermissionAsset hostAsset1 = getResponse1.getEntity().getAssets().stream()
                .filter(a -> updateTestHost.getIdentifier().equals(a.getId()))
                .findFirst()
                .orElse(null);
        assertTrue("Setup should have all 3 permissions",
                hostAsset1.getPermissions().get("INDIVIDUAL").containsAll(Set.of("READ", "WRITE", "PUBLISH")));

        // Action: Update to ONLY READ (should remove WRITE and PUBLISH)
        Map<String, Set<String>> updatePermissions = new HashMap<>();
        updatePermissions.put("INDIVIDUAL", Set.of("READ"));
        SaveUserPermissionsForm updateForm = new SaveUserPermissionsForm(updatePermissions, false);

        ResponseEntitySaveUserPermissionsView response = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestHost.getIdentifier(), updateForm
        );

        // Assert: Should have ONLY READ (replacement not merge)
        assertNotNull(response);
        UserPermissionAsset asset = response.getEntity().getAsset();
        Set<String> resultPerms = asset.getPermissions().get("INDIVIDUAL");
        assertEquals("Should have only 1 permission", 1, resultPerms.size());
        assertTrue("Should have READ", resultPerms.contains("READ"));
        assertFalse("Should NOT have WRITE", resultPerms.contains("WRITE"));
        assertFalse("Should NOT have PUBLISH", resultPerms.contains("PUBLISH"));

        // END-TO-END: Verify via GET API
        ResponseEntityUserPermissionsView getResponse2 = resource.getUserPermissions(
                request, this.response, updateTestUser.getUserId()
        );
        UserPermissionAsset hostAsset2 = getResponse2.getEntity().getAssets().stream()
                .filter(a -> updateTestHost.getIdentifier().equals(a.getId()))
                .findFirst()
                .orElse(null);
        Set<String> getPerms = hostAsset2.getPermissions().get("INDIVIDUAL");
        assertEquals("GET should show only READ", 1, getPerms.size());
        assertTrue(getPerms.contains("READ"));
    }

    @Test
    public void test_updateUserPermissions_invalidScope_badRequest() throws Exception {
        HttpServletRequest request = mockRequest();

        // Create form with invalid scope
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INVALID_SCOPE", Set.of("READ"));
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        try {
            resource.updateUserPermissions(
                    request, this.response, updateTestUser.getUserId(),
                    updateTestHost.getIdentifier(), form
            );
            fail("Should have thrown BadRequestException for invalid scope");
        } catch (Exception e) {
            assertTrue("Should be BadRequestException",
                    e instanceof BadRequestException ||
                    e.getMessage().contains("Invalid permission scope"));
        }
    }

    @Test
    public void test_updateUserPermissions_invalidLevel_badRequest() throws Exception {
        HttpServletRequest request = mockRequest();

        // Create form with invalid permission level
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Set.of("INVALID_LEVEL"));
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        try {
            resource.updateUserPermissions(
                    request, this.response, updateTestUser.getUserId(),
                    updateTestHost.getIdentifier(), form
            );
            fail("Should have thrown BadRequestException for invalid level");
        } catch (Exception e) {
            assertTrue("Should be BadRequestException",
                    e instanceof BadRequestException ||
                    e.getMessage().contains("Invalid permission level"));
        }
    }

    @Test
    public void test_updateUserPermissions_nonAdminUpdatingOther_forbidden() throws Exception {
        // Setup request as limitedUser (non-admin)
        MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request()
                        ).request()
                ).request()
        );

        request.getSession().setAttribute(WebKeys.USER_ID, limitedUser.getUserId());
        request.getSession().setAttribute(WebKeys.USER, limitedUser);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, testHost);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, testHost.getIdentifier());

        // Try to update another user's permissions
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Set.of("READ"));
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        try {
            resource.updateUserPermissions(
                    request, this.response, permissionTestUser.getUserId(),
                    updateTestHost.getIdentifier(), form
            );
            fail("Should have thrown DotSecurityException");
        } catch (Exception e) {
            assertTrue("Should be security exception",
                    e.getMessage().contains("Only admin users can update permissions"));
        }
    }

    // Note: test_updateUserPermissions_noEditPermissionsOnAsset_forbidden has been removed
    // Reason: Only admin users can update permissions via this API, and system admins
    // always have EDIT_PERMISSIONS on all assets. The scenario (admin without EDIT_PERMISSIONS)
    // cannot be tested with system administrators.

    @Test
    public void test_updateUserPermissions_nullPermissionLevel_badRequest() throws Exception {
        // Create form with null permission level
        Map<String, Set<String>> permissions = new HashMap<>();
        Set<String> levels = new HashSet<>();
        levels.add("READ");
        levels.add(null);  // Invalid null level
        permissions.put("INDIVIDUAL", levels);
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        try {
            form.checkValid();
            fail("Should have thrown BadRequestException for null permission level");
        } catch (BadRequestException e) {
            String entity = e.getResponse().getEntity().toString();
            assertTrue("Error message should contain 'cannot be null' or scope validation error",
                    entity.contains("cannot be null") || entity.contains("Invalid permission scope"));
        }
    }

    @Test
    public void test_updateUserPermissions_emptyPermissionList_badRequest() throws Exception {
        // Create form with empty permission list
        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Set.of());  // Empty list
        SaveUserPermissionsForm form = new SaveUserPermissionsForm(permissions, false);

        try {
            form.checkValid();
            fail("Should have thrown BadRequestException for empty permission list");
        } catch (BadRequestException e) {
            String entity = e.getResponse().getEntity().toString();
            assertTrue("Error message should contain 'cannot be empty' or scope validation error",
                    entity.contains("cannot be empty") || entity.contains("Invalid permission scope"));
        }
    }
}
