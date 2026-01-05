package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.system.permission.SaveUserPermissionsForm;
import com.dotcms.rest.api.v1.system.permission.PermissionSaveHelper;
import com.dotcms.rest.api.v1.system.permission.SaveUserPermissionsView;
import com.dotcms.rest.api.v1.system.permission.UserPermissionAssetView;
import com.dotcms.rest.api.v1.system.permission.ResponseEntitySaveUserPermissionsView;
import com.dotcms.rest.api.v1.system.permission.UserPermissionsView;
import com.dotcms.rest.api.v1.system.permission.UserInfoView;
import com.dotcms.rest.api.v1.system.permission.PermissionMetadataView;
import com.dotcms.rest.api.v1.system.permission.ResponseEntityPermissionMetadataView;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
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
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.jersey.internal.util.Base64;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.exception.DotSecurityException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for PermissionResource endpoints.
 * Tests:
 * - GET /api/v1/permissions/ (permission metadata)
 * - GET /api/v1/permissions/user/{userId} (get user permissions)
 * - PUT /api/v1/permissions/user/{userId}/asset/{assetId} (update user permissions)
 * - PUT /api/v1/permissions/{assetId} (update asset permissions)
 */
public class PermissionResourceIntegrationTest {

    static HttpServletResponse response;
    static PermissionResource resource;
    static User adminUser;
    static Host testHost;

    // Test data for PUT permissions testing
    static Host updateTestHost;
    static Folder updateTestFolder;
    static Folder parentFolder;
    static Folder childFolder;
    static User updateTestUser;
    static User limitedUser;
    static User permissionTestUser;
    static Host permissionTestHost;

    // Test role for updateAssetPermissions tests
    static Role testRole;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        // Create resource instance using simple constructor (uses getInstance() defaults internally)
        resource = new PermissionResource(new PermissionSaveHelper());

        adminUser = TestUserUtils.getAdminUser();
        testHost = new SiteDataGen().nextPersisted();
        response = new MockHttpResponse();

        // Setup test data for PUT permission tests
        setupUpdatePermissionTestData();
    }

    private static void setupUpdatePermissionTestData() throws Exception {
        // Create test host for permission test user
        permissionTestHost = new SiteDataGen().nextPersisted();
        permissionTestUser = TestUserUtils.getChrisPublisherUser(permissionTestHost);

        // Limited user for security tests
        limitedUser = UserTestUtil.getUser("limiteduser", false, true);
        Role backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        if (!APILocator.getRoleAPI().doesUserHaveRole(limitedUser, backendRole)) {
            APILocator.getRoleAPI().addRoleToUser(backendRole, limitedUser);
        }

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

        // Create test role for updateAssetPermissions tests
        testRole = new RoleDataGen().nextPersisted();
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

    // ==================== PUT Permission Tests ====================

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user updates permissions for a user on a host with
     *     INDIVIDUAL scope containing READ, WRITE, and PUBLISH permissions.</li>
     *     <li><b>Expected Result:</b> Permissions are saved successfully, response contains
     *     the updated asset with all three permission levels, and cascade is not initiated.</li>
     * </ul>
     */
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
        SaveUserPermissionsView data = response.getEntity();
        assertNotNull(data);
        assertFalse("Cascade should not be initiated", data.cascadeInitiated());
        assertEquals(updateTestUser.getUserId(), data.userId());

        // Verify asset in response
        UserPermissionAssetView asset = data.asset();
        assertEquals(updateTestHost.getIdentifier(), asset.id());
        Set<String> individualPerms = asset.permissions().get("INDIVIDUAL");
        assertNotNull(individualPerms);
        assertEquals(3, individualPerms.size());
        assertTrue(individualPerms.containsAll(Set.of("READ", "WRITE", "PUBLISH")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user updates permissions for a user on a folder with
     *     multiple permission scopes (INDIVIDUAL, HOST, and FOLDER) in a single request.</li>
     *     <li><b>Expected Result:</b> All three scopes are saved successfully and appear in
     *     the response with their respective permission levels.</li>
     * </ul>
     */
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
        SaveUserPermissionsView data = response.getEntity();
        UserPermissionAssetView asset = data.asset();

        // Verify all 3 scopes present
        Map<String, Set<String>> permMap = asset.permissions();
        assertTrue("Should have INDIVIDUAL scope", permMap.containsKey("INDIVIDUAL"));
        assertTrue("Should have HOST scope", permMap.containsKey("HOST"));
        assertTrue("Should have FOLDER scope", permMap.containsKey("FOLDER"));

        // Verify INDIVIDUAL permissions
        assertTrue(permMap.get("INDIVIDUAL").containsAll(Set.of("READ", "WRITE")));

        // Verify HOST permissions
        assertTrue(permMap.get("HOST").contains("READ"));

        // Verify FOLDER permissions
        assertTrue(permMap.get("FOLDER").containsAll(Set.of("READ", "CAN_ADD_CHILDREN")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user updates permissions on a child folder that
     *     currently inherits permissions from its parent folder.</li>
     *     <li><b>Expected Result:</b> The permission inheritance is automatically broken before
     *     saving, the child folder now has its own individual permissions, and inheritsPermissions
     *     returns false in the response.</li>
     * </ul>
     */
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
        SaveUserPermissionsView data = response.getEntity();
        assertEquals(childFolder.getInode(), data.asset().id());

        // VERIFY inheritance broken after PUT (critical assertion)
        assertFalse("Child folder should NOT be inheriting after PUT",
                APILocator.getPermissionAPI().isInheritingPermissions(childFolder));

        // Verify permissions set on child
        UserPermissionAssetView childAsset = data.asset();
        assertFalse("Child should not be inheriting", childAsset.inheritsPermissions());
        assertTrue("Child should have READ and WRITE",
                childAsset.permissions().get("INDIVIDUAL").containsAll(Set.of("READ", "WRITE")));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user updates permissions on a parent host with
     *     cascade=true to propagate permissions to all descendant assets.</li>
     *     <li><b>Expected Result:</b> Permissions are saved and cascadeInitiated returns true,
     *     indicating that the CascadePermissionsJob has been triggered.</li>
     * </ul>
     */
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
        SaveUserPermissionsView data = response.getEntity();
        assertTrue("Cascade should be initiated for parent permissionable", data.cascadeInitiated());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> A user already has READ, WRITE, and PUBLISH permissions on
     *     a host. Admin then updates permissions to only include READ.</li>
     *     <li><b>Expected Result:</b> The existing permissions are replaced (not merged), so the
     *     user now only has READ permission. WRITE and PUBLISH are removed.</li>
     * </ul>
     */
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
        ResponseEntitySaveUserPermissionsView setupResponse = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestHost.getIdentifier(), setupForm
        );
        UserPermissionAssetView hostAsset1 = setupResponse.getEntity().asset();
        assertTrue("Setup should have all 3 permissions",
                hostAsset1.permissions().get("INDIVIDUAL").containsAll(Set.of("READ", "WRITE", "PUBLISH")));

        // Action: Update to ONLY READ (should remove WRITE and PUBLISH)
        Map<String, Set<String>> updatePermissions = new HashMap<>();
        updatePermissions.put("INDIVIDUAL", Set.of("READ"));
        SaveUserPermissionsForm updateForm = new SaveUserPermissionsForm(updatePermissions, false);

        ResponseEntitySaveUserPermissionsView response = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestHost.getIdentifier(), updateForm
        );

        // Assert: Should have ONLY READ (replacement not merge)
        assertNotNull(response);
        UserPermissionAssetView asset = response.getEntity().asset();
        Set<String> resultPerms = asset.permissions().get("INDIVIDUAL");
        assertEquals("Should have only 1 permission", 1, resultPerms.size());
        assertTrue("Should have READ", resultPerms.contains("READ"));
        assertFalse("Should NOT have WRITE", resultPerms.contains("WRITE"));
        assertFalse("Should NOT have PUBLISH", resultPerms.contains("PUBLISH"));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin attempts to update permissions using an invalid
     *     permission scope name that doesn't exist in the system.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown indicating the invalid
     *     permission scope.</li>
     * </ul>
     */
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

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin attempts to update permissions using an invalid
     *     permission level name that doesn't exist in the system.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown indicating the invalid
     *     permission level.</li>
     * </ul>
     */
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

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateUserPermissions}</li>
     *     <li><b>Given Scenario:</b> A non-admin user (limited user) attempts to update
     *     permissions for another user.</li>
     *     <li><b>Expected Result:</b> A DotSecurityException is thrown indicating that only
     *     admin users can update permissions.</li>
     * </ul>
     */
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

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link SaveUserPermissionsForm#checkValid()}</li>
     *     <li><b>Given Scenario:</b> A form is created with a null value in the permission
     *     levels set for a scope.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown during form validation
     *     indicating that permission level cannot be null.</li>
     * </ul>
     */
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

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link SaveUserPermissionsForm#checkValid()}</li>
     *     <li><b>Given Scenario:</b> A form is created with an empty set of permission levels
     *     for a scope.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown during form validation
     *     indicating that permission levels cannot be empty.</li>
     * </ul>
     */
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

    // ==================== GET User Permissions Tests ====================

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#getUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user retrieves permissions for another user
     *     who has permission assets.</li>
     *     <li><b>Expected Result:</b> Permissions are retrieved successfully with user info,
     *     role ID, and list of permission assets.</li>
     * </ul>
     */
    @Test
    public void test_getUserPermissions_adminAccessingOtherUser_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Get permissions for updateTestUser (who has permissions on updateTestHost)
        ResponseEntityPaginatedDataView response = resource.getUserPermissions(
                request, this.response, updateTestUser.getUserId(), 1, 40
        );

        // Assert response structure
        assertNotNull(response);
        UserPermissionsView data = (UserPermissionsView) response.getEntity();
        assertNotNull(data);

        // Verify user info
        UserInfoView userInfo = data.user();
        assertNotNull(userInfo);
        assertEquals(updateTestUser.getUserId(), userInfo.id());
        assertEquals(updateTestUser.getFullName(), userInfo.name());
        assertEquals(updateTestUser.getEmailAddress(), userInfo.email());

        // Verify role ID is present
        assertNotNull(data.roleId());
        assertFalse(data.roleId().isEmpty());

        // Verify assets list is returned (may contain permission assets from setup)
        List<UserPermissionAssetView> assets = data.assets();
        assertNotNull(assets);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#getUserPermissions}</li>
     *     <li><b>Given Scenario:</b> A non-admin user retrieves their own permissions.</li>
     *     <li><b>Expected Result:</b> Permissions are retrieved successfully since users
     *     can always view their own permissions.</li>
     * </ul>
     */
    @Test
    public void test_getUserPermissions_userAccessingSelf_success() throws Exception {
        // Setup request as limitedUser (non-admin) accessing their own permissions
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

        // User accessing their own permissions - should succeed
        ResponseEntityPaginatedDataView response = resource.getUserPermissions(
                request, this.response, limitedUser.getUserId(), 1, 40
        );

        // Assert response
        assertNotNull(response);
        UserPermissionsView data = (UserPermissionsView) response.getEntity();
        assertNotNull(data);

        // Verify correct user info returned
        assertEquals(limitedUser.getUserId(), data.user().id());
        assertEquals(limitedUser.getEmailAddress(), data.user().email());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#getUserPermissions}</li>
     *     <li><b>Given Scenario:</b> A non-admin user attempts to retrieve another user's
     *     permissions.</li>
     *     <li><b>Expected Result:</b> A DotSecurityException is thrown indicating that only
     *     admin users can view other users' permissions.</li>
     * </ul>
     */
    @Test
    public void test_getUserPermissions_userAccessingOther_forbidden() throws Exception {
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

        // Non-admin trying to access another user's permissions
        try {
            resource.getUserPermissions(
                    request, this.response, updateTestUser.getUserId(), 1, 40
            );
            fail("Should have thrown DotSecurityException");
        } catch (DotSecurityException e) {
            assertTrue("Error message should indicate admin-only access",
                    e.getMessage().contains("Only admin user can retrieve other users permissions"));
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#getUserPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user retrieves permissions with pagination
     *     parameters.</li>
     *     <li><b>Expected Result:</b> Paginated results are returned and pagination metadata
     *     is correct.</li>
     * </ul>
     */
    @Test
    public void test_getUserPermissions_pagination_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Request with specific pagination parameters
        int page = 1;
        int perPage = 10;

        ResponseEntityPaginatedDataView response = resource.getUserPermissions(
                request, this.response, adminUser.getUserId(), page, perPage
        );

        // Assert response
        assertNotNull(response);
        UserPermissionsView data = (UserPermissionsView) response.getEntity();
        assertNotNull(data);

        // Verify pagination object is present
        assertNotNull(response.getPagination());
        assertEquals(page, response.getPagination().getCurrentPage());
        assertEquals(perPage, response.getPagination().getPerPage());

        // Total entries should be set (could be 0 or more)
        assertTrue(response.getPagination().getTotalEntries() >= 0);
    }

    // ==================== GET Permission Metadata Tests ====================

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#getPermissionMetadata}</li>
     *     <li><b>Given Scenario:</b> Backend user requests permission metadata.</li>
     *     <li><b>Expected Result:</b> Available permission levels and scopes are returned.</li>
     * </ul>
     */
    @Test
    public void test_getPermissionMetadata_success() throws Exception {
        HttpServletRequest request = mockRequest();

        ResponseEntityPermissionMetadataView response = resource.getPermissionMetadata(
                request, this.response
        );

        // Assert response
        assertNotNull(response);
        PermissionMetadataView metadata = response.getEntity();
        assertNotNull(metadata);

        // Verify permission levels
        Set<PermissionAPI.Type> levels = metadata.levels();
        assertNotNull(levels);
        assertFalse("Should have permission levels", levels.isEmpty());
        assertTrue("Should include READ level", levels.contains(PermissionAPI.Type.READ));
        assertTrue("Should include WRITE level", levels.contains(PermissionAPI.Type.WRITE));
        assertTrue("Should include PUBLISH level", levels.contains(PermissionAPI.Type.PUBLISH));
        assertTrue("Should include EDIT_PERMISSIONS level", levels.contains(PermissionAPI.Type.EDIT_PERMISSIONS));
        assertTrue("Should include CAN_ADD_CHILDREN level", levels.contains(PermissionAPI.Type.CAN_ADD_CHILDREN));

        // Verify permission scopes
        Set<PermissionAPI.Scope> scopes = metadata.scopes();
        assertNotNull(scopes);
        assertFalse("Should have permission scopes", scopes.isEmpty());
        assertTrue("Should include INDIVIDUAL scope", scopes.contains(PermissionAPI.Scope.INDIVIDUAL));
        assertTrue("Should include HOST scope", scopes.contains(PermissionAPI.Scope.HOST));
        assertTrue("Should include FOLDER scope", scopes.contains(PermissionAPI.Scope.FOLDER));
    }

    // ==================== PUT Asset Permissions Tests (updateAssetPermissions) ====================

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateAssetPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user updates permissions for a host asset with
     *     a single role having READ, WRITE, and PUBLISH individual permissions.</li>
     *     <li><b>Expected Result:</b> Permissions are saved successfully, response contains
     *     message, permissionCount, and asset with correct permissions.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_basicHostUpdate_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Create form with single role having READ, WRITE, PUBLISH permissions
        List<RolePermissionForm> permissions = new ArrayList<>();
        permissions.add(new RolePermissionForm(
                testRole.getId(),
                List.of("READ", "WRITE", "PUBLISH"),
                null  // no inheritable
        ));
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(permissions);

        // Execute PUT
        ResponseEntityUpdatePermissionsView response = resource.updateAssetPermissions(
                request, this.response, updateTestHost.getIdentifier(), false, form
        );

        // Assert response metadata
        assertNotNull(response);
        UpdateAssetPermissionsView data = response.getEntity();
        assertNotNull(data);
        assertEquals("Permissions saved successfully", data.message());
        assertTrue("Permission count should be > 0", data.permissionCount() > 0);

        // Verify asset metadata in response
        AssetPermissionsView asset = data.asset();
        assertNotNull(asset);
        assertEquals(updateTestHost.getIdentifier(), asset.assetId());
        assertTrue("Host should be parent permissionable", asset.isParentPermissionable());

        // Verify the permissions were actually saved - check response contains our role with correct permissions
        assertFalse("Response should contain permissions", asset.permissions().isEmpty());
        RolePermissionView rolePermission = asset.permissions().stream()
                .filter(rp -> rp.roleId().equals(testRole.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull("Response should contain permissions for testRole", rolePermission);
        assertTrue("Should have READ permission", rolePermission.individual().contains(PermissionAPI.Type.READ));
        assertTrue("Should have WRITE permission", rolePermission.individual().contains(PermissionAPI.Type.WRITE));
        assertTrue("Should have PUBLISH permission", rolePermission.individual().contains(PermissionAPI.Type.PUBLISH));

        // Verify via PermissionAPI (ground truth) that permissions were actually persisted
        assertTrue("Role should have READ permission on host via API",
                APILocator.getPermissionAPI().doesRoleHavePermission(updateTestHost, PermissionAPI.PERMISSION_READ, testRole));
        assertTrue("Role should have WRITE permission on host via API",
                APILocator.getPermissionAPI().doesRoleHavePermission(updateTestHost, PermissionAPI.PERMISSION_WRITE, testRole));
        assertTrue("Role should have PUBLISH permission on host via API",
                APILocator.getPermissionAPI().doesRoleHavePermission(updateTestHost, PermissionAPI.PERMISSION_PUBLISH, testRole));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateAssetPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user updates permissions for a folder with
     *     both individual permissions and inheritable permissions for multiple scopes.</li>
     *     <li><b>Expected Result:</b> Both individual and inheritable permissions are saved,
     *     response asset contains permissions with inheritable map.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_multipleScopes_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Create form with individual and inheritable permissions
        Map<String, List<String>> inheritable = new HashMap<>();
        inheritable.put("FOLDER", List.of("READ", "CAN_ADD_CHILDREN"));
        inheritable.put("CONTENT", List.of("READ", "WRITE"));

        List<RolePermissionForm> permissions = new ArrayList<>();
        permissions.add(new RolePermissionForm(
                testRole.getId(),
                List.of("READ", "WRITE"),
                inheritable
        ));
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(permissions);

        // Execute PUT on folder (parent permissionable)
        ResponseEntityUpdatePermissionsView response = resource.updateAssetPermissions(
                request, this.response, updateTestFolder.getInode(), false, form
        );

        // Assert response
        assertNotNull(response);
        UpdateAssetPermissionsView data = response.getEntity();
        assertNotNull(data);
        assertEquals("Permissions saved successfully", data.message());
        assertTrue("Permission count should be > 0", data.permissionCount() > 0);

        // Verify asset is parent permissionable (can have inheritable)
        AssetPermissionsView asset = data.asset();
        assertTrue("Folder should be parent permissionable", asset.isParentPermissionable());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateAssetPermissions}</li>
     *     <li><b>Given Scenario:</b> Admin user updates permissions on a child folder that
     *     currently inherits permissions from its parent folder.</li>
     *     <li><b>Expected Result:</b> The permission inheritance is automatically broken,
     *     inheritanceBroken=true in response, and inheritanceMode is INDIVIDUAL.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_breaksInheritance_success() throws Exception {
        HttpServletRequest request = mockRequest();

        // Create a new child folder that inherits for this test (to avoid test pollution)
        Folder inheritingChild = new FolderDataGen()
                .site(updateTestHost)
                .parent(parentFolder)
                .title("inheriting-child-" + System.currentTimeMillis())
                .nextPersisted();

        // Reset to ensure it inherits
        APILocator.getPermissionAPI().resetPermissionsUnder(parentFolder);

        // VERIFY inheritance before test (critical assertion)
        assertTrue("Child folder should be inheriting before test",
                APILocator.getPermissionAPI().isInheritingPermissions(inheritingChild));

        // Create form
        List<RolePermissionForm> permissions = new ArrayList<>();
        permissions.add(new RolePermissionForm(
                testRole.getId(),
                List.of("READ", "WRITE"),
                null
        ));
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(permissions);

        // Execute PUT on inheriting folder
        ResponseEntityUpdatePermissionsView response = resource.updateAssetPermissions(
                request, this.response, inheritingChild.getInode(), false, form
        );

        // Assert response
        assertNotNull(response);
        UpdateAssetPermissionsView data = response.getEntity();
        assertTrue("inheritanceBroken should be true", data.inheritanceBroken());
        assertEquals(InheritanceMode.INDIVIDUAL, data.asset().inheritanceMode());

        // VERIFY inheritance broken after PUT (critical assertion)
        assertFalse("Child folder should NOT be inheriting after PUT",
                APILocator.getPermissionAPI().isInheritingPermissions(inheritingChild));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UpdateAssetPermissionsForm#checkValid()}</li>
     *     <li><b>Given Scenario:</b> A form is created with an empty permissions array.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown during form validation
     *     indicating that permissions cannot be empty.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_emptyPermissions_badRequest() throws Exception {
        // Create form with empty permissions list
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(new ArrayList<>());

        try {
            form.checkValid();
            fail("Should have thrown BadRequestException for empty permissions");
        } catch (BadRequestException e) {
            String entity = e.getResponse().getEntity().toString();
            assertTrue("Error message should contain 'empty'",
                    entity.toLowerCase().contains("empty"));
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UpdateAssetPermissionsForm#checkValid()}</li>
     *     <li><b>Given Scenario:</b> A form is created with an invalid permission scope
     *     in the inheritable map.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown during form validation
     *     indicating the invalid scope.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_invalidScope_badRequest() throws Exception {
        // Create form with invalid scope
        Map<String, List<String>> inheritable = new HashMap<>();
        inheritable.put("INVALID_SCOPE", List.of("READ"));

        List<RolePermissionForm> permissions = new ArrayList<>();
        permissions.add(new RolePermissionForm(
                testRole.getId(),
                null,  // no individual
                inheritable
        ));
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(permissions);

        try {
            form.checkValid();
            fail("Should have thrown BadRequestException for invalid scope");
        } catch (BadRequestException e) {
            String entity = e.getResponse().getEntity().toString();
            assertTrue("Error message should contain 'scope'",
                    entity.toLowerCase().contains("scope"));
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UpdateAssetPermissionsForm#checkValid()}</li>
     *     <li><b>Given Scenario:</b> A form is created with an invalid permission level.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown during form validation
     *     indicating the invalid level.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_invalidLevel_badRequest() throws Exception {
        // Create form with invalid permission level
        List<RolePermissionForm> permissions = new ArrayList<>();
        permissions.add(new RolePermissionForm(
                testRole.getId(),
                List.of("INVALID_LEVEL"),
                null
        ));
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(permissions);

        try {
            form.checkValid();
            fail("Should have thrown BadRequestException for invalid level");
        } catch (BadRequestException e) {
            String entity = e.getResponse().getEntity().toString();
            assertTrue("Error message should contain 'level'",
                    entity.toLowerCase().contains("level"));
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UpdateAssetPermissionsForm#checkValid()}</li>
     *     <li><b>Given Scenario:</b> A form is created with a permission entry missing roleId.</li>
     *     <li><b>Expected Result:</b> A BadRequestException is thrown during form validation
     *     indicating roleId is required.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_missingRoleId_badRequest() throws Exception {
        // Create form with missing roleId
        List<RolePermissionForm> permissions = new ArrayList<>();
        permissions.add(new RolePermissionForm(
                null,  // missing roleId
                List.of("READ"),
                null
        ));
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(permissions);

        try {
            form.checkValid();
            fail("Should have thrown BadRequestException for missing roleId");
        } catch (BadRequestException e) {
            String entity = e.getResponse().getEntity().toString();
            assertTrue("Error message should contain 'roleId'",
                    entity.toLowerCase().contains("roleid"));
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link PermissionResource#updateAssetPermissions}</li>
     *     <li><b>Given Scenario:</b> A non-admin user attempts to update asset permissions.</li>
     *     <li><b>Expected Result:</b> A DotSecurityException is thrown indicating that only
     *     admin users can update asset permissions.</li>
     * </ul>
     */
    @Test
    public void test_updateAssetPermissions_nonAdmin_forbidden() throws Exception {
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

        // Create valid form
        List<RolePermissionForm> permissions = new ArrayList<>();
        permissions.add(new RolePermissionForm(
                testRole.getId(),
                List.of("READ"),
                null
        ));
        UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(permissions);

        try {
            resource.updateAssetPermissions(
                    request, this.response, updateTestHost.getIdentifier(), false, form
            );
            fail("Should have thrown DotSecurityException");
        } catch (DotSecurityException e) {
            assertTrue("Error message should indicate admin-only",
                    e.getMessage().toLowerCase().contains("admin"));
        }
    }
}
