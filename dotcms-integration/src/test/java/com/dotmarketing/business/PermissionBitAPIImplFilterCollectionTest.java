package com.dotmarketing.business;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link PermissionBitAPIImpl#filterCollection(java.util.Collection, int, User, boolean)}.
 */
public class PermissionBitAPIImplFilterCollectionTest {

    private static PermissionAPI permissionAPI;
    private static User adminUser;
    private static User systemUser;
    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        adminUser     = TestUserUtils.getAdminUser();
        systemUser    = APILocator.systemUser();
        site          = new SiteDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link PermissionBitAPIImpl#filterCollection(java.util.Collection, int, User, boolean)} <br>
     * Given Scenario: null collection is passed. <br>
     * Expected Result: empty list returned without error.
     */
    @Test
    public void test_filterCollection_nullCollection_returnsEmpty()
            throws DotDataException, DotSecurityException {
        final List<Folder> result = permissionAPI.filterCollection(
                null, PermissionAPI.PERMISSION_READ, adminUser, false);
        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link PermissionBitAPIImpl#filterCollection(java.util.Collection, int, User, boolean)} <br>
     * Given Scenario: empty collection is passed. <br>
     * Expected Result: empty list returned without error.
     */
    @Test
    public void test_filterCollection_emptyCollection_returnsEmpty()
            throws DotDataException, DotSecurityException {
        final List<Folder> result = permissionAPI.filterCollection(
                List.of(), PermissionAPI.PERMISSION_READ, adminUser, false);
        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link PermissionBitAPIImpl#filterCollection(java.util.Collection, int, User, boolean)} <br>
     * Given Scenario: CMS admin user is passed. <br>
     * Expected Result: all items are returned — admin bypasses permission checks.
     */
    @Test
    public void test_filterCollection_adminUser_returnsAll()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Folder f1 = new FolderDataGen().site(site).name("admin-all-1-" + ts).nextPersisted();
        final Folder f2 = new FolderDataGen().site(site).name("admin-all-2-" + ts).nextPersisted();

        final List<Folder> result = permissionAPI.filterCollection(
                List.of(f1, f2), PermissionAPI.PERMISSION_READ, adminUser, false);

        assertEquals(2, result.size());
    }

    /**
     * Method to test: {@link PermissionBitAPIImpl#filterCollection(java.util.Collection, int, User, boolean)} <br>
     * Given Scenario: system user is passed. <br>
     * Expected Result: all items are returned — system user bypasses permission checks.
     */
    @Test
    public void test_filterCollection_systemUser_returnsAll()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Folder f1 = new FolderDataGen().site(site).name("sys-all-1-" + ts).nextPersisted();
        final Folder f2 = new FolderDataGen().site(site).name("sys-all-2-" + ts).nextPersisted();

        final List<Folder> result = permissionAPI.filterCollection(
                List.of(f1, f2), PermissionAPI.PERMISSION_READ, systemUser, false);

        assertEquals(2, result.size());
    }

    /**
     * Method to test: {@link PermissionBitAPIImpl#filterCollection(java.util.Collection, int, User, boolean)} <br>
     * Given Scenario: limited user has READ on one of two folders. <br>
     * Expected Result: only the permitted folder is returned.
     */
    @Test
    public void test_filterCollection_limitedUser_returnsOnlyPermittedFolders()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder permitted  = new FolderDataGen().site(freshSite).name("permitted").nextPersisted();
        final Folder restricted = new FolderDataGen().site(freshSite).name("restricted").nextPersisted();

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        permissionAPI.save(
                new Permission(permitted.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                permitted, adminUser, false);

        final List<Folder> result = permissionAPI.filterCollection(
                List.of(permitted, restricted), PermissionAPI.PERMISSION_READ, limitedUser, false);

        assertEquals(1, result.size());
        assertEquals(permitted.getInode(), result.get(0).getInode());
    }

    /**
     * Method to test: {@link PermissionBitAPIImpl#filterCollection(java.util.Collection, int, User, boolean)} <br>
     * Given Scenario: limited user has READ on both folders. <br>
     * Expected Result: both folders are returned.
     */
    @Test
    public void test_filterCollection_limitedUser_permittedOnBoth_returnsBoth()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder f1 = new FolderDataGen().site(freshSite).name("both-1").nextPersisted();
        final Folder f2 = new FolderDataGen().site(freshSite).name("both-2").nextPersisted();

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        permissionAPI.save(
                new Permission(f1.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                f1, adminUser, false);
        permissionAPI.save(
                new Permission(f2.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                f2, adminUser, false);

        final List<Folder> result = permissionAPI.filterCollection(
                List.of(f1, f2), PermissionAPI.PERMISSION_READ, limitedUser, false);

        assertEquals(2, result.size());
    }
}
