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
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link PermissionBitFactoryImpl#getPermittedIds(java.util.Collection, int, List)}.
 */
public class PermissionBitFactoryImplGetPermittedIdsTest {

    private static PermissionBitFactoryImpl factory;
    private static PermissionAPI permissionAPI;
    private static User adminUser;
    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        factory       = new PermissionBitFactoryImpl(CacheLocator.getPermissionCache());
        permissionAPI = APILocator.getPermissionAPI();
        adminUser     = TestUserUtils.getAdminUser();
        site          = new SiteDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#getPermittedIds} <br>
     * Given Scenario: empty inode list. <br>
     * Expected Result: empty set returned without hitting the DB.
     */
    @Test
    public void test_getPermittedIds_emptyInodeIds_returnsEmpty() throws DotDataException {
        final Set<String> result = factory.getPermittedIds(
                List.of(), PermissionAPI.PERMISSION_READ, List.of("some-role-id"));
        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#getPermittedIds} <br>
     * Given Scenario: empty role list. <br>
     * Expected Result: empty set returned — no roles means no access.
     */
    @Test
    public void test_getPermittedIds_emptyRoleIds_returnsEmpty() throws DotDataException {
        final Folder folder = new FolderDataGen().site(site)
                .name("empty-roles-" + System.currentTimeMillis()).nextPersisted();

        final Set<String> result = factory.getPermittedIds(
                List.of(folder.getInode()), PermissionAPI.PERMISSION_READ, List.of());
        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#getPermittedIds} <br>
     * Given Scenario: role has READ permission on the folder. <br>
     * Expected Result: the folder's inode is in the returned set.
     */
    @Test
    public void test_getPermittedIds_roleHasPermission_returnsInode()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder folder  = new FolderDataGen().site(freshSite).name("has-perm").nextPersisted();

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId    = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        permissionAPI.save(
                new Permission(folder.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                folder, adminUser, false);

        final Set<String> result = factory.getPermittedIds(
                List.of(folder.getInode()), PermissionAPI.PERMISSION_READ, List.of(roleId));

        assertEquals(1, result.size());
        assertTrue(result.contains(folder.getInode()));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#getPermittedIds} <br>
     * Given Scenario: role has no permission on the folder. <br>
     * Expected Result: the folder's inode is NOT in the returned set.
     */
    @Test
    public void test_getPermittedIds_roleHasNoPermission_doesNotReturnInode()
            throws DotDataException {
        final Folder folder = new FolderDataGen().site(site)
                .name("no-perm-" + System.currentTimeMillis()).nextPersisted();

        final User unrelatedUser = new UserDataGen().nextPersisted();
        final String unrelatedRoleId = APILocator.getRoleAPI()
                .loadRoleByKey(unrelatedUser.getUserId()).getId();

        final Set<String> result = factory.getPermittedIds(
                List.of(folder.getInode()), PermissionAPI.PERMISSION_READ, List.of(unrelatedRoleId));

        assertFalse(result.contains(folder.getInode()));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#getPermittedIds} <br>
     * Given Scenario: two folders, role has READ only on the first. <br>
     * Expected Result: only the first folder's inode is returned.
     */
    @Test
    public void test_getPermittedIds_mixedPermissions_returnsOnlyPermitted()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder permitted  = new FolderDataGen().site(freshSite).name("mix-perm").nextPersisted();
        final Folder restricted = new FolderDataGen().site(freshSite).name("mix-restricted").nextPersisted();

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId    = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        permissionAPI.save(
                new Permission(permitted.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                permitted, adminUser, false);

        final Set<String> result = factory.getPermittedIds(
                List.of(permitted.getInode(), restricted.getInode()),
                PermissionAPI.PERMISSION_READ,
                List.of(roleId));

        assertTrue(result.contains(permitted.getInode()));
        assertFalse(result.contains(restricted.getInode()));
    }

    /**
     * Method to test: {@link PermissionBitFactoryImpl#getPermittedIds} <br>
     * Given Scenario: role has READ but not CAN_ADD_CHILDREN on the folder. <br>
     * Expected Result: folder is in READ result but not in CAN_ADD_CHILDREN result.
     */
    @Test
    public void test_getPermittedIds_differentPermissionBits_areCheckedIndependently()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder folder  = new FolderDataGen().site(freshSite).name("bit-check").nextPersisted();

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId    = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        permissionAPI.save(
                new Permission(folder.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                folder, adminUser, false);

        final Set<String> canRead = factory.getPermittedIds(
                List.of(folder.getInode()), PermissionAPI.PERMISSION_READ, List.of(roleId));
        final Set<String> canAddChildren = factory.getPermittedIds(
                List.of(folder.getInode()), PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, List.of(roleId));

        assertTrue("Role should have READ", canRead.contains(folder.getInode()));
        assertFalse("Role should NOT have CAN_ADD_CHILDREN", canAddChildren.contains(folder.getInode()));
    }
}
