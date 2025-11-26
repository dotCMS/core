package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for the PermissionResource PUT endpoint.
 * Tests the update asset permissions functionality.
 *
 * @author dotCMS
 * @since 24.01
 */
public class PermissionResourceIntegrationTest {

    private static HttpServletResponse response;
    private static PermissionResource resource;
    private static PermissionAPI permissionAPI;
    private static RoleAPI roleAPI;
    private static User adminUser;
    private static Host testSite;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        response = new MockHttpResponse();
        resource = new PermissionResource();
        permissionAPI = APILocator.getPermissionAPI();
        roleAPI = APILocator.getRoleAPI();
        adminUser = TestUserUtils.getAdminUser();
        testSite = new SiteDataGen().nextPersisted();
    }

    private HttpServletRequest getHttpRequest(final String userEmail, final String password) {
        final String userEmailAndPassword = userEmail + ":" + password;
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode(userEmailAndPassword.getBytes())));

        return request;
    }

    /**
     * Method to test: updateAssetPermissions in the PermissionResource
     * Given Scenario: Update folder permissions with valid admin user and role
     * ExpectedResult: Permissions saved successfully, response contains message, permissionCount, inheritanceBroken, and asset
     */
    @Test
    public void test_updateAssetPermissions_success() throws DotDataException, DotSecurityException {
        // Create a test folder
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

        // Create a test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build the form
        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ", "WRITE", "PUBLISH"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Call the endpoint
        final ResponseEntityUpdatePermissionsView responseView = resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testFolder.getInode(),
                false,
                form
        );

        // Verify response
        assertNotNull("Response should not be null", responseView);
        final Map<String, Object> entity = responseView.getEntity();
        assertNotNull("Entity should not be null", entity);

        // Verify response fields
        assertEquals("Permissions saved successfully", entity.get("message"));
        assertNotNull("permissionCount should be present", entity.get("permissionCount"));
        assertNotNull("inheritanceBroken should be present", entity.get("inheritanceBroken"));
        assertNotNull("asset should be present", entity.get("asset"));

        // Verify asset has permissions
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        assertNotNull("asset.permissions should be present", asset.get("permissions"));
        assertEquals("INDIVIDUAL", asset.get("inheritanceMode"));
    }

    /**
     * Method to test: updateAssetPermissions in the PermissionResource
     * Given Scenario: Non-admin user tries to update permissions
     * ExpectedResult: DotSecurityException is thrown with appropriate message
     */
    @Test(expected = DotSecurityException.class)
    public void test_updateAssetPermissions_nonAdminUser_throws403() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Create a non-admin user with backend role (so they can authenticate) but NOT admin
        // Must use a known plaintext password and assign frontend/backend roles for authentication
        final String knownPassword = "testPassword123";
        final User nonAdminUser = new UserDataGen()
                .password(knownPassword)
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole())
                .nextPersisted();

        // Build the form
        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Call the endpoint - should throw DotSecurityException because user is not admin
        resource.updateAssetPermissions(
                getHttpRequest(nonAdminUser.getEmailAddress(), knownPassword),
                response,
                testFolder.getInode(),
                false,
                form
        );
    }

    /**
     * Method to test: updateAssetPermissions in the PermissionResource
     * Given Scenario: Invalid role ID in the request
     * ExpectedResult: IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_updateAssetPermissions_invalidRoleId_throws400() throws DotDataException, DotSecurityException {
        // Create a test folder
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

        // Build form with invalid role ID
        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                "invalid-role-id-12345",
                Arrays.asList("READ"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Call the endpoint - should throw IllegalArgumentException
        resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testFolder.getInode(),
                false,
                form
        );
    }

    /**
     * Method to test: updateAssetPermissions in the PermissionResource
     * Given Scenario: Asset ID does not exist
     * ExpectedResult: NotFoundInDbException is thrown
     */
    @Test(expected = com.dotcms.contenttype.exception.NotFoundInDbException.class)
    public void test_updateAssetPermissions_assetNotFound_throws404() throws DotDataException, DotSecurityException {
        // Create a test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build the form
        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Call with non-existent asset ID
        resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                "non-existent-asset-id-12345",
                false,
                form
        );
    }

    /**
     * Method to test: updateAssetPermissions in the PermissionResource
     * Given Scenario: Update permissions on an inheriting folder
     * ExpectedResult: inheritanceBroken should be true in response
     */
    @Test
    public void test_updateAssetPermissions_breaksInheritance() throws DotDataException, DotSecurityException {
        // Create a parent folder and child folder (child inherits by default)
        final Folder parentFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Folder childFolder = new FolderDataGen().parent(parentFolder).nextPersisted();

        // Verify child is inheriting
        assertTrue("Child folder should initially inherit permissions",
                permissionAPI.isInheritingPermissions(childFolder));

        // Create a test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build the form
        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ", "WRITE"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Update permissions on child folder
        final ResponseEntityUpdatePermissionsView responseView = resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                childFolder.getInode(),
                false,
                form
        );

        // Verify response
        final Map<String, Object> entity = responseView.getEntity();
        assertTrue("inheritanceBroken should be true", (Boolean) entity.get("inheritanceBroken"));

        // Verify folder is no longer inheriting
        assertFalse("Child folder should no longer inherit permissions",
                permissionAPI.isInheritingPermissions(childFolder));
    }

    /**
     * Method to test: updateAssetPermissions in the PermissionResource
     * Given Scenario: Update permissions with inheritable permissions on a folder
     * ExpectedResult: Both individual and inheritable permissions are saved
     */
    @Test
    public void test_updateAssetPermissions_withInheritablePermissions() throws DotDataException, DotSecurityException {
        // Create a test folder
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

        // Create a test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build form with both individual and inheritable permissions
        final Map<String, List<String>> inheritable = new HashMap<>();
        inheritable.put("FOLDER", Arrays.asList("READ", "CAN_ADD_CHILDREN"));
        inheritable.put("CONTENT", Arrays.asList("READ", "WRITE", "PUBLISH"));

        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ", "WRITE", "PUBLISH", "EDIT_PERMISSIONS"),
                inheritable
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Call the endpoint
        final ResponseEntityUpdatePermissionsView responseView = resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testFolder.getInode(),
                false,
                form
        );

        // Verify response
        final Map<String, Object> entity = responseView.getEntity();
        assertEquals("Permissions saved successfully", entity.get("message"));

        // Permission count should include individual + inheritable
        final int permissionCount = (Integer) entity.get("permissionCount");
        assertTrue("Permission count should be > 1 (individual + inheritable)", permissionCount > 1);

        // Verify asset permissions in response
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        final List<Map<String, Object>> permissions = (List<Map<String, Object>>) asset.get("permissions");
        assertFalse("Permissions should not be empty", permissions.isEmpty());
    }

    /**
     * Method to test: updateAssetPermissions in the PermissionResource
     * Given Scenario: Invalid permission level name
     * ExpectedResult: IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_updateAssetPermissions_invalidPermissionLevel_throws400() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build form with invalid permission level
        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ", "INVALID_PERMISSION"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Call the endpoint - should throw IllegalArgumentException
        resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testFolder.getInode(),
                false,
                form
        );
    }

    // ========================================================================
    // RESET ASSET PERMISSIONS TESTS
    // ========================================================================

    /**
     * Method to test: resetAssetPermissions in the PermissionResource
     * Given Scenario: Reset permissions on a folder with individual permissions
     * ExpectedResult: Permissions reset successfully, folder now inherits
     */
    @Test
    public void test_resetAssetPermissions_success() throws DotDataException, DotSecurityException {
        // Create parent and child folder
        final Folder parentFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Folder childFolder = new FolderDataGen().parent(parentFolder).nextPersisted();

        // Create a test role and add individual permissions to child folder
        final Role testRole = new RoleDataGen().nextPersisted();
        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ", "WRITE"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // First, set individual permissions on child folder
        resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                childFolder.getInode(),
                false,
                form
        );

        // Verify child has individual permissions
        assertFalse("Child folder should have individual permissions",
                permissionAPI.isInheritingPermissions(childFolder));

        // Now reset the permissions
        final ResponseEntityResetPermissionsView responseView = resource.resetAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                childFolder.getInode()
        );

        // Verify response
        assertNotNull("Response should not be null", responseView);
        final Map<String, Object> entity = responseView.getEntity();
        assertNotNull("Entity should not be null", entity);

        // Verify response fields
        assertEquals("Individual permissions removed. Asset now inherits from parent.",
                entity.get("message"));
        assertEquals(childFolder.getInode(), entity.get("assetId"));
        assertNotNull("previousPermissionCount should be present", entity.get("previousPermissionCount"));
        assertTrue("previousPermissionCount should be >= 0",
                (Integer) entity.get("previousPermissionCount") >= 0);

        // Verify folder is now inheriting
        assertTrue("Child folder should now inherit permissions",
                permissionAPI.isInheritingPermissions(childFolder));
    }

    /**
     * Method to test: resetAssetPermissions in the PermissionResource
     * Given Scenario: Non-admin user tries to reset permissions
     * ExpectedResult: DotSecurityException is thrown
     */
    @Test(expected = DotSecurityException.class)
    public void test_resetAssetPermissions_nonAdminUser_throws403() throws DotDataException, DotSecurityException {
        // Create test folder
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

        // Create a non-admin user with backend role
        final String knownPassword = "testPassword456";
        final User nonAdminUser = new UserDataGen()
                .password(knownPassword)
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole())
                .nextPersisted();

        // Call the endpoint - should throw DotSecurityException
        resource.resetAssetPermissions(
                getHttpRequest(nonAdminUser.getEmailAddress(), knownPassword),
                response,
                testFolder.getInode()
        );
    }

    /**
     * Method to test: resetAssetPermissions in the PermissionResource
     * Given Scenario: Asset ID does not exist
     * ExpectedResult: NotFoundInDbException is thrown
     */
    @Test(expected = com.dotcms.contenttype.exception.NotFoundInDbException.class)
    public void test_resetAssetPermissions_assetNotFound_throws404() throws DotDataException, DotSecurityException {
        // Call with non-existent asset ID
        resource.resetAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                "non-existent-asset-id-67890"
        );
    }

    /**
     * Method to test: resetAssetPermissions in the PermissionResource
     * Given Scenario: Asset already inherits permissions
     * ExpectedResult: ConflictException is thrown (409 Conflict)
     */
    @Test(expected = ConflictException.class)
    public void test_resetAssetPermissions_alreadyInheriting_throws409() throws DotDataException, DotSecurityException {
        // Create parent and child folder (child inherits by default)
        final Folder parentFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Folder childFolder = new FolderDataGen().parent(parentFolder).nextPersisted();

        // Verify child is inheriting
        assertTrue("Child folder should initially inherit permissions",
                permissionAPI.isInheritingPermissions(childFolder));

        // Try to reset - should throw ConflictException since already inheriting
        resource.resetAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                childFolder.getInode()
        );
    }

    /**
     * Method to test: resetAssetPermissions in the PermissionResource
     * Given Scenario: Reset with empty/null asset ID
     * ExpectedResult: IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_resetAssetPermissions_emptyAssetId_throws400() throws DotDataException, DotSecurityException {
        // Call with empty asset ID - should throw IllegalArgumentException
        resource.resetAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                ""
        );
    }

    // ========================================================================
    // GET ROLE PERMISSIONS TESTS
    // ========================================================================

    /**
     * Method to test: getRolePermissions in the PermissionResource
     * Given Scenario: Admin user requests permissions for any role
     * ExpectedResult: Role permissions retrieved successfully with roleId, roleName, and assets
     */
    @Test
    public void test_getRolePermissions_adminCanViewAnyRole() throws DotDataException, DotSecurityException {
        // Create a test role with some permissions
        final Role testRole = new RoleDataGen().nextPersisted();

        // Create a folder and add permissions for this role
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ", "WRITE"),
                null
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Set up permissions for the role
        resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testFolder.getInode(),
                false,
                form
        );

        // Call the endpoint as admin
        final ResponseEntityRolePermissionsView responseView = resource.getRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId()
        );

        // Verify response
        assertNotNull("Response should not be null", responseView);
        final Map<String, Object> entity = responseView.getEntity();
        assertNotNull("Entity should not be null", entity);

        // Verify response fields
        assertEquals("roleId should match", testRole.getId(), entity.get("roleId"));
        assertEquals("roleName should match", testRole.getName(), entity.get("roleName"));
        assertNotNull("assets should be present", entity.get("assets"));

        // Verify assets is a list
        final List<Map<String, Object>> assets = (List<Map<String, Object>>) entity.get("assets");
        assertNotNull("assets should be a list", assets);
    }

    /**
     * Method to test: getRolePermissions in the PermissionResource
     * Given Scenario: Non-admin user requests permissions for their own role
     * ExpectedResult: Role permissions retrieved successfully
     */
    @Test
    public void test_getRolePermissions_userCanViewOwnRole() throws DotDataException, DotSecurityException {
        // Create a test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Create a non-admin user and assign them the test role
        final String knownPassword = "testPassword789";
        final User testUser = new UserDataGen()
                .password(knownPassword)
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole(), testRole)
                .nextPersisted();

        // Call the endpoint - user viewing their own role
        final ResponseEntityRolePermissionsView responseView = resource.getRolePermissions(
                getHttpRequest(testUser.getEmailAddress(), knownPassword),
                response,
                testRole.getId()
        );

        // Verify response
        assertNotNull("Response should not be null", responseView);
        final Map<String, Object> entity = responseView.getEntity();
        assertNotNull("Entity should not be null", entity);

        // Verify response fields
        assertEquals("roleId should match", testRole.getId(), entity.get("roleId"));
        assertEquals("roleName should match", testRole.getName(), entity.get("roleName"));
        assertNotNull("assets should be present", entity.get("assets"));
    }

    /**
     * Method to test: getRolePermissions in the PermissionResource
     * Given Scenario: Non-admin user requests permissions for a role they don't have
     * ExpectedResult: ForbiddenException is thrown (403)
     */
    @Test(expected = com.dotcms.rest.exception.ForbiddenException.class)
    public void test_getRolePermissions_userCannotViewOtherRole_throws403() throws DotDataException, DotSecurityException {
        // Create a test role that the user will NOT have
        final Role testRole = new RoleDataGen().nextPersisted();

        // Create a non-admin user WITHOUT the test role
        final String knownPassword = "testPassword101";
        final User testUser = new UserDataGen()
                .password(knownPassword)
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole())
                .nextPersisted();

        // Call the endpoint - should throw ForbiddenException
        resource.getRolePermissions(
                getHttpRequest(testUser.getEmailAddress(), knownPassword),
                response,
                testRole.getId()
        );
    }

    /**
     * Method to test: getRolePermissions in the PermissionResource
     * Given Scenario: Invalid role ID provided
     * ExpectedResult: BadRequestException is thrown (400)
     */
    @Test(expected = com.dotcms.rest.exception.BadRequestException.class)
    public void test_getRolePermissions_invalidRoleId_throws400() throws DotDataException, DotSecurityException {
        // Call with invalid role ID - should throw BadRequestException
        resource.getRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                "invalid-role-id-99999"
        );
    }

    /**
     * Method to test: getRolePermissions in the PermissionResource
     * Given Scenario: Empty role ID provided
     * ExpectedResult: BadRequestException is thrown (400)
     */
    @Test(expected = com.dotcms.rest.exception.BadRequestException.class)
    public void test_getRolePermissions_emptyRoleId_throws400() throws DotDataException, DotSecurityException {
        // Call with empty role ID - should throw BadRequestException
        resource.getRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                ""
        );
    }

    /**
     * Method to test: getRolePermissions in the PermissionResource
     * Given Scenario: Admin user requests permissions and verifies response structure
     * ExpectedResult: Response contains correct structure with assets containing permissions map
     */
    @Test
    public void test_getRolePermissions_responseStructure() throws DotDataException, DotSecurityException {
        // Create a test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Create a folder and add permissions for this role with inheritable permissions
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

        final Map<String, List<String>> inheritable = new HashMap<>();
        inheritable.put("FOLDER", Arrays.asList("READ", "WRITE"));
        inheritable.put("CONTENT", Arrays.asList("READ", "WRITE", "PUBLISH"));

        final RolePermissionForm rolePermissionForm = new RolePermissionForm(
                testRole.getId(),
                Arrays.asList("READ", "WRITE", "PUBLISH"),
                inheritable
        );
        final UpdateAssetPermissionsForm form = new UpdateAssetPermissionsForm(
                Arrays.asList(rolePermissionForm)
        );

        // Set up permissions for the role
        resource.updateAssetPermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testFolder.getInode(),
                false,
                form
        );

        // Call the endpoint
        final ResponseEntityRolePermissionsView responseView = resource.getRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId()
        );

        // Verify response structure
        final Map<String, Object> entity = responseView.getEntity();
        assertEquals("roleId should match", testRole.getId(), entity.get("roleId"));
        assertEquals("roleName should match", testRole.getName(), entity.get("roleName"));

        final List<Map<String, Object>> assets = (List<Map<String, Object>>) entity.get("assets");
        assertNotNull("assets should be present", assets);
        assertFalse("assets should not be empty", assets.isEmpty());

        // Find the folder in the assets
        boolean foundFolder = false;
        for (Map<String, Object> asset : assets) {
            if (testFolder.getInode().equals(asset.get("id"))) {
                foundFolder = true;

                // Verify asset structure
                assertEquals("type should be FOLDER", "FOLDER", asset.get("type"));
                assertNotNull("name should be present", asset.get("name"));
                assertNotNull("path should be present", asset.get("path"));
                assertNotNull("hostId should be present", asset.get("hostId"));
                assertNotNull("canEditPermissions should be present", asset.get("canEditPermissions"));
                assertNotNull("inheritsPermissions should be present", asset.get("inheritsPermissions"));

                // Verify permissions map
                final Map<String, List<String>> permissions = (Map<String, List<String>>) asset.get("permissions");
                assertNotNull("permissions should be present", permissions);
                assertFalse("permissions should not be empty", permissions.isEmpty());

                break;
            }
        }
        assertTrue("Should find the test folder in assets", foundFolder);
    }

    // ========================================================================
    // UPDATE ROLE PERMISSIONS TESTS
    // ========================================================================

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Admin user updates role permissions on a folder
     * ExpectedResult: Permissions updated successfully with roleId, roleName, and asset in response
     */
    @Test
    public void test_updateRolePermissions_success() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build the form with scope-to-permissions map
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ", "WRITE", "PUBLISH"));
        permissions.put("CONTENT", Arrays.asList("READ", "WRITE"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call the endpoint
        final ResponseEntityUpdateRolePermissionsView responseView = resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form
        );

        // Verify response
        assertNotNull("Response should not be null", responseView);
        final Map<String, Object> entity = responseView.getEntity();
        assertNotNull("Entity should not be null", entity);

        // Verify response fields
        assertEquals("roleId should match", testRole.getId(), entity.get("roleId"));
        assertEquals("roleName should match", testRole.getName(), entity.get("roleName"));
        assertNotNull("asset should be present", entity.get("asset"));

        // Verify asset structure
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        assertEquals("asset id should match", testFolder.getInode(), asset.get("id"));
        assertEquals("asset type should be FOLDER", "FOLDER", asset.get("type"));
        assertNotNull("asset permissions should be present", asset.get("permissions"));
        assertFalse("asset should not inherit", (Boolean) asset.get("inheritsPermissions"));
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Update role permissions on a host using host name
     * ExpectedResult: Permissions updated successfully, asset resolved by name
     */
    @Test
    public void test_updateRolePermissions_withHostName() throws DotDataException, DotSecurityException {
        // Create test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build the form
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call the endpoint using host name instead of ID
        final ResponseEntityUpdateRolePermissionsView responseView = resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testSite.getHostname(),  // Using hostname
                false,
                form
        );

        // Verify response
        assertNotNull("Response should not be null", responseView);
        final Map<String, Object> entity = responseView.getEntity();
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        assertEquals("asset type should be HOST", "HOST", asset.get("type"));
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Non-admin user tries to update role permissions
     * ExpectedResult: DotSecurityException is thrown
     */
    @Test(expected = DotSecurityException.class)
    public void test_updateRolePermissions_nonAdminUser_throws403() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Create a non-admin user
        final String knownPassword = "testRolePermPassword";
        final User nonAdminUser = new UserDataGen()
                .password(knownPassword)
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole())
                .nextPersisted();

        // Build the form
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call the endpoint - should throw DotSecurityException
        resource.updateRolePermissions(
                getHttpRequest(nonAdminUser.getEmailAddress(), knownPassword),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form
        );
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Invalid role ID provided
     * ExpectedResult: NotFoundInDbException is thrown
     */
    @Test(expected = com.dotcms.contenttype.exception.NotFoundInDbException.class)
    public void test_updateRolePermissions_invalidRoleId_throws404() throws DotDataException, DotSecurityException {
        // Create test folder
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

        // Build the form
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call with invalid role ID
        resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                "invalid-role-id-999",
                testFolder.getInode(),
                false,
                form
        );
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Invalid asset ID provided
     * ExpectedResult: NotFoundInDbException is thrown
     */
    @Test(expected = com.dotcms.contenttype.exception.NotFoundInDbException.class)
    public void test_updateRolePermissions_invalidAssetId_throws404() throws DotDataException, DotSecurityException {
        // Create test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build the form
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call with invalid asset ID
        resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                "invalid-asset-id-999",
                false,
                form
        );
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Invalid permission scope in request
     * ExpectedResult: IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_updateRolePermissions_invalidScope_throws400() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build form with invalid scope
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INVALID_SCOPE", Arrays.asList("READ"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call the endpoint - should throw IllegalArgumentException
        resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form
        );
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Invalid permission level in request
     * ExpectedResult: IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_updateRolePermissions_invalidPermissionLevel_throws400() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build form with invalid permission level
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ", "INVALID_LEVEL"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call the endpoint - should throw IllegalArgumentException
        resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form
        );
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Update role permissions on an inheriting folder
     * ExpectedResult: Inheritance is automatically broken
     */
    @Test
    public void test_updateRolePermissions_breaksInheritance() throws DotDataException, DotSecurityException {
        // Create parent and child folder (child inherits by default)
        final Folder parentFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Folder childFolder = new FolderDataGen().parent(parentFolder).nextPersisted();

        // Verify child is inheriting
        assertTrue("Child folder should initially inherit permissions",
                permissionAPI.isInheritingPermissions(childFolder));

        // Create test role
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build the form
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ", "WRITE"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Update role permissions on child folder
        final ResponseEntityUpdateRolePermissionsView responseView = resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                childFolder.getInode(),
                false,
                form
        );

        // Verify response
        final Map<String, Object> entity = responseView.getEntity();
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        assertFalse("Asset should not inherit after update", (Boolean) asset.get("inheritsPermissions"));

        // Verify folder is no longer inheriting
        assertFalse("Child folder should no longer inherit permissions",
                permissionAPI.isInheritingPermissions(childFolder));
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Empty array removes permissions for that scope
     * ExpectedResult: Permissions for that scope are removed
     */
    @Test
    public void test_updateRolePermissions_emptyArrayRemovesScope() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // First, set some permissions
        final Map<String, List<String>> initialPerms = new HashMap<>();
        initialPerms.put("INDIVIDUAL", Arrays.asList("READ", "WRITE", "PUBLISH"));
        initialPerms.put("CONTENT", Arrays.asList("READ", "WRITE"));
        final UpdateRolePermissionsForm form1 = new UpdateRolePermissionsForm(initialPerms);

        resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form1
        );

        // Now update with empty array for CONTENT to remove it
        final Map<String, List<String>> updatePerms = new HashMap<>();
        updatePerms.put("INDIVIDUAL", Arrays.asList("READ"));  // Reduce individual
        updatePerms.put("CONTENT", Arrays.asList());  // Empty array - should remove
        final UpdateRolePermissionsForm form2 = new UpdateRolePermissionsForm(updatePerms);

        final ResponseEntityUpdateRolePermissionsView responseView = resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form2
        );

        // Verify response
        final Map<String, Object> entity = responseView.getEntity();
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        final Map<String, List<String>> permMap = (Map<String, List<String>>) asset.get("permissions");

        // CONTENT should be removed (not present or empty)
        assertTrue("CONTENT scope should be removed",
                !permMap.containsKey("CONTENT") || permMap.get("CONTENT").isEmpty());

        // INDIVIDUAL should have only READ
        assertTrue("INDIVIDUAL should contain READ", permMap.get("INDIVIDUAL").contains("READ"));
        assertFalse("INDIVIDUAL should not contain WRITE", permMap.get("INDIVIDUAL").contains("WRITE"));
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Omitted scope preserves existing permissions (hybrid semantics)
     * ExpectedResult: Omitted scopes retain their existing permissions
     */
    @Test
    public void test_updateRolePermissions_omittedScopePreservesPermissions() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // First, set permissions for INDIVIDUAL and CONTENT
        final Map<String, List<String>> initialPerms = new HashMap<>();
        initialPerms.put("INDIVIDUAL", Arrays.asList("READ", "WRITE"));
        initialPerms.put("CONTENT", Arrays.asList("READ", "PUBLISH"));
        final UpdateRolePermissionsForm form1 = new UpdateRolePermissionsForm(initialPerms);

        resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form1
        );

        // Now update ONLY INDIVIDUAL (omit CONTENT)
        final Map<String, List<String>> updatePerms = new HashMap<>();
        updatePerms.put("INDIVIDUAL", Arrays.asList("READ"));  // Change individual
        // CONTENT is omitted - should be preserved
        final UpdateRolePermissionsForm form2 = new UpdateRolePermissionsForm(updatePerms);

        final ResponseEntityUpdateRolePermissionsView responseView = resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form2
        );

        // Verify response
        final Map<String, Object> entity = responseView.getEntity();
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        final Map<String, List<String>> permMap = (Map<String, List<String>>) asset.get("permissions");

        // INDIVIDUAL should be updated to just READ
        assertEquals("INDIVIDUAL should have only READ",
                Arrays.asList("READ"), permMap.get("INDIVIDUAL"));

        // CONTENT should be preserved with original values
        assertNotNull("CONTENT scope should be preserved", permMap.get("CONTENT"));
        assertTrue("CONTENT should still contain READ", permMap.get("CONTENT").contains("READ"));
        assertTrue("CONTENT should still contain PUBLISH", permMap.get("CONTENT").contains("PUBLISH"));
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Empty permissions map provided
     * ExpectedResult: IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_updateRolePermissions_emptyPermissions_throws400() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build form with empty permissions map
        final Map<String, List<String>> permissions = new HashMap<>();
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call the endpoint - should throw IllegalArgumentException
        resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form
        );
    }

    /**
     * Method to test: updateRolePermissions in the PermissionResource
     * Given Scenario: Update with all permission scopes
     * ExpectedResult: All scopes are saved correctly
     */
    @Test
    public void test_updateRolePermissions_allScopes() throws DotDataException, DotSecurityException {
        // Create test data
        final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();
        final Role testRole = new RoleDataGen().nextPersisted();

        // Build form with all scopes
        final Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("INDIVIDUAL", Arrays.asList("READ", "WRITE", "PUBLISH", "EDIT_PERMISSIONS"));
        permissions.put("HOST", Arrays.asList("READ"));
        permissions.put("FOLDER", Arrays.asList("READ", "WRITE", "CAN_ADD_CHILDREN"));
        permissions.put("CONTENT", Arrays.asList("READ", "WRITE", "PUBLISH"));
        permissions.put("PAGE", Arrays.asList("READ", "WRITE"));
        permissions.put("CONTAINER", Arrays.asList("READ"));
        permissions.put("TEMPLATE", Arrays.asList("READ"));
        permissions.put("TEMPLATE_LAYOUT", Arrays.asList("READ"));
        permissions.put("LINK", Arrays.asList("READ"));
        permissions.put("CONTENT_TYPE", Arrays.asList("READ"));
        permissions.put("CATEGORY", Arrays.asList("READ"));
        permissions.put("RULE", Arrays.asList("READ"));
        final UpdateRolePermissionsForm form = new UpdateRolePermissionsForm(permissions);

        // Call the endpoint
        final ResponseEntityUpdateRolePermissionsView responseView = resource.updateRolePermissions(
                getHttpRequest(adminUser.getEmailAddress(), "admin"),
                response,
                testRole.getId(),
                testFolder.getInode(),
                false,
                form
        );

        // Verify response
        assertNotNull("Response should not be null", responseView);
        final Map<String, Object> entity = responseView.getEntity();
        final Map<String, Object> asset = (Map<String, Object>) entity.get("asset");
        final Map<String, List<String>> permMap = (Map<String, List<String>>) asset.get("permissions");

        // Verify INDIVIDUAL permissions
        assertNotNull("INDIVIDUAL should be present", permMap.get("INDIVIDUAL"));
        assertTrue("INDIVIDUAL should contain EDIT_PERMISSIONS",
                permMap.get("INDIVIDUAL").contains("EDIT_PERMISSIONS"));
    }
}
