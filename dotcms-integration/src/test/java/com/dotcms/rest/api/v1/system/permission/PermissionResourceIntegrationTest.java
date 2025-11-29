package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.system.permission.SaveUserPermissionsForm;
import com.dotcms.rest.api.v1.system.permission.PermissionSaveHelper;
import com.dotcms.rest.api.v1.system.permission.SaveUserPermissionsView;
import com.dotcms.rest.api.v1.system.permission.UserPermissionAssetView;
import com.dotcms.rest.api.v1.system.permission.ResponseEntitySaveUserPermissionsView;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for PermissionResource user permissions endpoints.
 * Tests PUT /api/v1/permissions/user/{userId}/asset/{assetId}
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
        assertFalse("Cascade should not be initiated", data.isCascadeInitiated());
        assertEquals(updateTestUser.getUserId(), data.getUserId());

        // Verify asset in response
        UserPermissionAssetView asset = data.getAsset();
        assertEquals(updateTestHost.getIdentifier(), asset.getId());
        Set<String> individualPerms = asset.getPermissions().get("INDIVIDUAL");
        assertNotNull(individualPerms);
        assertEquals(3, individualPerms.size());
        assertTrue(individualPerms.containsAll(Set.of("READ", "WRITE", "PUBLISH")));
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
        SaveUserPermissionsView data = response.getEntity();
        UserPermissionAssetView asset = data.getAsset();

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
        SaveUserPermissionsView data = response.getEntity();
        assertEquals(childFolder.getInode(), data.getAsset().getId());

        // VERIFY inheritance broken after PUT (critical assertion)
        assertFalse("Child folder should NOT be inheriting after PUT",
                APILocator.getPermissionAPI().isInheritingPermissions(childFolder));

        // Verify permissions set on child
        UserPermissionAssetView childAsset = data.getAsset();
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
        SaveUserPermissionsView data = response.getEntity();
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
        ResponseEntitySaveUserPermissionsView setupResponse = resource.updateUserPermissions(
                request, this.response, updateTestUser.getUserId(), updateTestHost.getIdentifier(), setupForm
        );
        UserPermissionAssetView hostAsset1 = setupResponse.getEntity().getAsset();
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
        UserPermissionAssetView asset = response.getEntity().getAsset();
        Set<String> resultPerms = asset.getPermissions().get("INDIVIDUAL");
        assertEquals("Should have only 1 permission", 1, resultPerms.size());
        assertTrue("Should have READ", resultPerms.contains("READ"));
        assertFalse("Should NOT have WRITE", resultPerms.contains("WRITE"));
        assertFalse("Should NOT have PUBLISH", resultPerms.contains("PUBLISH"));
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
