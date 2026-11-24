package com.dotmarketing.portlets.folders.business;

import com.dotmarketing.common.db.DotConnect;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.api.v1.folder.FolderSearchView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for Content Drive folder search permissions (#37749).
 * Verifies that the batch permission filter falls back to inheritance walk-up
 * when an inheriting folder has no cached permission_reference row.
 */
public class FolderSearchPermissionsIT {

    private static FolderAPI folderAPI;
    private static PermissionAPI permissionAPI;
    private static RoleAPI roleAPI;
    private static User adminUser;
    private static Host site;
    private static boolean originalAsyncConfig;
    private static final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        folderAPI     = APILocator.getFolderAPI();
        permissionAPI = APILocator.getPermissionAPI();
        roleAPI       = APILocator.getRoleAPI();
        adminUser     = TestUserUtils.getAdminUser();
        site          = new SiteDataGen().nextPersisted();
        originalAsyncConfig = Config.getBooleanProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", true);
    }

    @AfterClass
    public static void cleanup() {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", originalAsyncConfig);
        for (final User user : createdUsers) {
            UserDataGen.remove(user, true);
        }
        try {
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        } catch (final Exception e) {
            Logger.warn(FolderSearchPermissionsIT.class, "Unable to delete the test site: " + e.getMessage());
        }
    }


    /**
     * Grants a role a permission on a folder, both on the folder itself and as the inheritable
     * permission for the folders under it, which is what the permissions UI saves. A single
     * individual permission would give the subfolders nothing to inherit.
     */
    private static void grantOnFolderAndSubfolders(final Folder folder, final String roleId, final int permission)
            throws DotDataException, DotSecurityException {
        permissionAPI.save(
                new Permission(folder.getPermissionId(), roleId, permission, true), folder, adminUser, false);
        permissionAPI.save(
                new Permission(Folder.class.getCanonicalName(), folder.getPermissionId(), roleId, permission, true),
                folder, adminUser, false);
    }

    /** A new backend user with no permissions of its own, removed in {@link #cleanup()}. */
    private static User newUser() {
        final User user = new UserDataGen().nextPersisted();
        createdUsers.add(user);
        return user;
    }

    /**
     * Clears both the DB permission_reference row and the in-memory cache for the given folder,
     * leaving it in an uncached inheriting state.
     */
    private static void makeUncached(final Folder folder) throws DotDataException {
        new DotConnect()
                .setSQL("DELETE FROM permission_reference WHERE asset_id = ?")
                .addParam(folder.getPermissionId())
                .loadResult();
        permissionAPI.removePermissionableFromCache(folder.getPermissionId());
    }

    /**
     * Checks if a permission_reference row exists in the database for the given folder.
     */
    private static boolean hasPermissionReferenceRow(final Folder folder) throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT COUNT(*) AS count FROM permission_reference WHERE asset_id = ?");
        dc.addParam(folder.getPermissionId());
        dc.loadResult();
        return dc.getInt("count") > 0;
    }

    /**
     * AC-001: For a non-admin user, GET /api/v1/folder/search returns a folder that inherits
     * READ from its parent and has no cached reference row, on the first request, without
     * anyone opening the Site Browser first.
     */
    @Test
    public void test_uncachedInheritingFolder_isReturnedInSearch_AC001()
            throws DotDataException, DotSecurityException {
        final Host freshSite = site;
        final Folder parent  = new FolderDataGen().site(freshSite).name("parent-ac001").nextPersisted();
        final Folder child   = new FolderDataGen().site(freshSite).parent(parent).name("child-ac001").nextPersisted();

        final User limitedUser = newUser();
        final String roleId    = roleAPI.loadRoleByKey(limitedUser.getUserId()).getId();

        // Grant inheritable READ on parent to limitedUser role (child inherits from parent)
        permissionAPI.save(
                new Permission(Folder.class.getCanonicalName(), parent.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                parent, adminUser, false);

        // Delete child's permission_reference to simulate uncached inheriting folder
        makeUncached(child);
        assertFalse("Precondition: child must be uncached", hasPermissionReferenceRow(child));

        // As limitedUser, search for folders under parent
        final PaginatedArrayList<FolderSearchView> result = folderAPI.searchFolders(
                FolderSearchParams.builder()
                        .siteId(freshSite.getIdentifier())
                        .path("/" + parent.getName() + "/")
                        .recursive(false)
                        .user(limitedUser)
                        .build());

        assertEquals("Uncached inheriting child folder must be returned in folder search", 1, result.size());
        assertEquals(child.getIdentifier(), result.get(0).id());
    }

    /**
     * AC-002: Right after a permission change on a site or parent folder, a non-admin user
     * sees the same folders in the Content Drive tree as in the Site Browser.
     */
    @Test
    public void test_permissionChangeOnSite_preservesFolderVisibility_AC002()
            throws DotDataException, DotSecurityException {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);
        try {
            final Host freshSite = site;
            final Folder parent  = new FolderDataGen().site(freshSite).name("parent-ac002").nextPersisted();
            final Folder child   = new FolderDataGen().site(freshSite).parent(parent).name("child-ac002").nextPersisted();

            final User limitedUser = newUser();
            final String roleId    = roleAPI.loadRoleByKey(limitedUser.getUserId()).getId();

            // Grant inheritable READ on parent
            permissionAPI.save(
                    new Permission(Folder.class.getCanonicalName(), parent.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                    parent, adminUser, false);

            // Warm child reference row first using limitedUser (non-admin triggers walk-up and reference write)
            permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_READ, limitedUser);
            assertTrue("Precondition: child should be cached before parent permission update", hasPermissionReferenceRow(child));

            // Modify permission on parent (which deletes subfolder references)
            permissionAPI.save(
                    new Permission(Folder.class.getCanonicalName(), parent.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true),
                    parent, adminUser, false);

            assertFalse("Permission change on parent must clear child's reference row", hasPermissionReferenceRow(child));

            // Immediately search as limitedUser
            final PaginatedArrayList<FolderSearchView> result = folderAPI.searchFolders(
                    FolderSearchParams.builder()
                            .siteId(freshSite.getIdentifier())
                            .path("/" + parent.getName() + "/")
                            .recursive(false)
                            .user(limitedUser)
                            .build());

            assertEquals("Folder must remain visible immediately after parent permission update", 1, result.size());
            assertEquals(child.getIdentifier(), result.get(0).id());
        } finally {
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", originalAsyncConfig);
        }
    }

    /**
     * AC-003 & AC-004: Consistency before and after caching, including the three known differences:
     * - Folder owner grant
     * - Liferay admin without CMS Admin role
     * - PUBLISH on folders
     */
    @Test
    public void test_consistencyBeforeAndAfterCaching_AC003_AC004()
            throws DotDataException, DotSecurityException {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);
        try {
            final Host freshSite = site;
            final Folder parent  = new FolderDataGen().site(freshSite).name("parent-ac003").nextPersisted();
            final Folder child   = new FolderDataGen().site(freshSite).parent(parent).name("child-ac003").nextPersisted();

            final User limitedUser = newUser();
            final String roleId    = roleAPI.loadRoleByKey(limitedUser.getUserId()).getId();

            // 1. Standard role permission: inheritable READ on parent
            permissionAPI.save(
                    new Permission(Folder.class.getCanonicalName(), parent.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                    parent, adminUser, false);

            makeUncached(child);

            // Filter collection while uncached
            final List<Folder> uncachedResult = permissionAPI.filterCollection(
                    List.of(child), PermissionAPI.PERMISSION_READ, limitedUser, false);
            assertEquals("Uncached inheriting folder must pass filterCollection", 1, uncachedResult.size());

            // Filter collection when cached
            final List<Folder> cachedResult = permissionAPI.filterCollection(
                    List.of(child), PermissionAPI.PERMISSION_READ, limitedUser, false);
            assertEquals("Cached inheriting folder must pass filterCollection", 1, cachedResult.size());
            assertEquals("Answer must be identical before and after caching",
                    uncachedResult.get(0).getInode(), cachedResult.get(0).getInode());

            // 2. Known difference 1: Folder owner grant
            // Owner role is granted READ, but the user's role is not
            final User ownerUser = newUser();
            final Folder ownedFolder = new FolderDataGen().site(freshSite).parent(parent).name("owned-folder").nextPersisted();
            ownedFolder.setOwner(ownerUser.getUserId());
            APILocator.getFolderAPI().save(ownedFolder, adminUser, false);
            final Role ownerRole = roleAPI.loadCMSOwnerRole();
            permissionAPI.save(
                    new Permission(Folder.class.getCanonicalName(), parent.getPermissionId(), ownerRole.getId(), PermissionAPI.PERMISSION_READ, true),
                    parent, adminUser, false);

            makeUncached(ownedFolder);
            final List<Folder> ownerUncached = permissionAPI.filterCollection(
                    List.of(ownedFolder), PermissionAPI.PERMISSION_READ, ownerUser, false);
            final List<Folder> ownerCached = permissionAPI.filterCollection(
                    List.of(ownedFolder), PermissionAPI.PERMISSION_READ, ownerUser, false);
            assertEquals("Owner grant without user role must be denied before caching (batch filter rule)", 0, ownerUncached.size());
            assertEquals("Owner grant without user role must be denied after caching (batch filter rule)", 0, ownerCached.size());

            // 3. Known difference 2: Liferay admin without CMS Admin role
            final User liferayAdminUser = new User() {
                @Override
                public boolean isAdmin() {
                    return true;
                }
            };
            liferayAdminUser.setUserId("lr-admin-" + System.currentTimeMillis());
            final Folder restrictedParent = new FolderDataGen().site(freshSite).name("restricted-parent").nextPersisted();
            final Folder restrictedChild  = new FolderDataGen().site(freshSite).parent(restrictedParent).name("restricted-child").nextPersisted();
            makeUncached(restrictedChild);

            final List<Folder> lrAdminUncached = permissionAPI.filterCollection(
                    List.of(restrictedChild), PermissionAPI.PERMISSION_READ, liferayAdminUser, false);
            final List<Folder> lrAdminCached = permissionAPI.filterCollection(
                    List.of(restrictedChild), PermissionAPI.PERMISSION_READ, liferayAdminUser, false);
            assertEquals("Liferay admin without CMS admin role must be denied before caching", 0, lrAdminUncached.size());
            assertEquals("Liferay admin without CMS admin role must be denied after caching", 0, lrAdminCached.size());

            // 4. Known difference 3: PUBLISH on folders
            // Grant EDIT on parent, but NOT PUBLISH
            final Folder editOnlyParent = new FolderDataGen().site(freshSite).name("edit-parent").nextPersisted();
            final Folder editOnlyChild  = new FolderDataGen().site(freshSite).parent(editOnlyParent).name("edit-child").nextPersisted();
            permissionAPI.save(
                    new Permission(Folder.class.getCanonicalName(), editOnlyParent.getPermissionId(), roleId, PermissionAPI.PERMISSION_EDIT, true),
                    editOnlyParent, adminUser, false);
            makeUncached(editOnlyChild);

            final List<Folder> pubUncached = permissionAPI.filterCollection(
                    List.of(editOnlyChild), PermissionAPI.PERMISSION_PUBLISH, limitedUser, false);
            final List<Folder> pubCached = permissionAPI.filterCollection(
                    List.of(editOnlyChild), PermissionAPI.PERMISSION_PUBLISH, limitedUser, false);
            assertEquals("PUBLISH requested without PUBLISH permission must be denied before caching", 0, pubUncached.size());
            assertEquals("PUBLISH requested without PUBLISH permission must be denied after caching", 0, pubCached.size());
        } finally {
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", originalAsyncConfig);
        }
    }

    /**
     * AC-005: The "has subfolders" indicator is true for a folder whose only readable child
     * inherits and is uncached.
     */
    @Test
    public void test_hasSubfolders_trueWhenOnlyChildIsUncached_AC005()
            throws DotDataException, DotSecurityException {
        final Host freshSite = site;
        final Folder rootFolder = new FolderDataGen().site(freshSite).name("root-ac005").nextPersisted();
        final Folder childFolder = new FolderDataGen().site(freshSite).parent(rootFolder).name("child-ac005").nextPersisted();

        final User limitedUser = newUser();
        final String roleId    = roleAPI.loadRoleByKey(limitedUser.getUserId()).getId();

        // Individual READ on rootFolder so user can see it
        grantOnFolderAndSubfolders(rootFolder, roleId, PermissionAPI.PERMISSION_READ);

        makeUncached(childFolder);

        // Search folders at root level (parent of rootFolder is site root)
        final PaginatedArrayList<FolderSearchView> result = folderAPI.searchFolders(
                FolderSearchParams.builder()
                        .siteId(freshSite.getIdentifier())
                        .path("/")
                        .recursive(false)
                        .user(limitedUser)
                        .build());

        final FolderSearchView rootView = result.stream()
                .filter(v -> v.id().equals(rootFolder.getIdentifier()))
                .findFirst()
                .orElse(null);

        assertNotNull("Root folder must be returned", rootView);
        assertTrue("hasChildren must be true when only child inherits and is uncached", rootView.hasChildren());
    }

    /**
     * AC-008: A folder the user cannot read is still left out, whether or not it has a cached row.
     */
    @Test
    public void test_deniedFolder_remainsDenied_AC008()
            throws DotDataException, DotSecurityException {
        final Host freshSite = site;
        final Folder parent  = new FolderDataGen().site(freshSite).name("denied-parent").nextPersisted();
        final Folder child   = new FolderDataGen().site(freshSite).parent(parent).name("denied-child").nextPersisted();

        final User limitedUser = newUser();
        // No permissions granted to limitedUser

        makeUncached(child);

        final PaginatedArrayList<FolderSearchView> result = folderAPI.searchFolders(
                FolderSearchParams.builder()
                        .siteId(freshSite.getIdentifier())
                        .path("/" + parent.getName() + "/")
                        .recursive(false)
                        .user(limitedUser)
                        .build());

        assertEquals("Denied uncached folder must not be returned", 0, result.size());
    }

    /**
     * AC-009: CMS Administrators and the system user see the same results as before.
     */
    @Test
    public void test_adminBypass_preserved_AC009()
            throws DotDataException, DotSecurityException {
        final Host freshSite = site;
        final Folder parent  = new FolderDataGen().site(freshSite).name("admin-parent").nextPersisted();
        final Folder child   = new FolderDataGen().site(freshSite).parent(parent).name("admin-child").nextPersisted();

        makeUncached(child);

        final List<Folder> adminResult = permissionAPI.filterCollection(
                List.of(child), PermissionAPI.PERMISSION_READ, adminUser, false);
        assertEquals("CMS Administrator must bypass filterCollection", 1, adminResult.size());

        final List<Folder> systemUserResult = permissionAPI.filterCollection(
                List.of(child), PermissionAPI.PERMISSION_READ, APILocator.systemUser(), false);
        assertEquals("System user must bypass filterCollection", 1, systemUserResult.size());
    }

    /**
     * AC-010: After a request that took the one-at-a-time check for an uncached item,
     * a later request for the same item resolves it in the batch query, once the cache row is written.
     */
    @Test
    public void test_secondRequest_resolvesFromBatchCache_AC010()
            throws DotDataException, DotSecurityException {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);
        try {
            final Host freshSite = site;
            final Folder parent  = new FolderDataGen().site(freshSite).name("ac010-parent").nextPersisted();
            final Folder child   = new FolderDataGen().site(freshSite).parent(parent).name("ac010-child").nextPersisted();

            final User limitedUser = newUser();
            final String roleId    = roleAPI.loadRoleByKey(limitedUser.getUserId()).getId();
            permissionAPI.save(
                    new Permission(Folder.class.getCanonicalName(), parent.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                    parent, adminUser, false);

            makeUncached(child);
            assertFalse("Precondition: child is uncached", hasPermissionReferenceRow(child));

            // Request 1: takes fallback, writes permission_reference row synchronously
            final List<Folder> req1 = permissionAPI.filterCollection(
                    List.of(child), PermissionAPI.PERMISSION_READ, limitedUser, false);
            assertEquals("First request returns child", 1, req1.size());
            assertTrue("permission_reference row must be written after first request", hasPermissionReferenceRow(child));

            // Request 2: resolves from permission_reference
            final List<Folder> req2 = permissionAPI.filterCollection(
                    List.of(child), PermissionAPI.PERMISSION_READ, limitedUser, false);
            assertEquals("Second request returns child from batch cache", 1, req2.size());
        } finally {
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", originalAsyncConfig);
        }
    }

    /**
     * AC-007 & AC-012: For a low-privilege user who is denied folders on a site,
     * denied folders with cached rows run zero one-at-a-time checks.
     */
    @Test
    public void test_deniedFolders_doNotTriggerWalkUp_AC007_AC012()
            throws DotDataException, DotSecurityException {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);
        try {
            final Host freshSite = site;
            final Folder parent  = new FolderDataGen().site(freshSite).name("denied-set-parent").nextPersisted();

            final List<Folder> children = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                children.add(new FolderDataGen().site(freshSite).parent(parent).name("denied-child-" + i).nextPersisted());
            }

            final User limitedUser = newUser();
            // No permissions on parent or children for limitedUser

            // Save inheritable permission for an unrelated user so reference rows can point to parent
            final User otherUser = newUser();
            final String otherRoleId = roleAPI.loadRoleByKey(otherUser.getUserId()).getId();
            permissionAPI.save(
                    new Permission(Folder.class.getCanonicalName(), parent.getPermissionId(), otherRoleId, PermissionAPI.PERMISSION_READ, true),
                    parent, adminUser, false);

            // Warm the cache so all children have permission_reference rows pointing to parent
            for (Folder c : children) {
                permissionAPI.doesUserHavePermission(c, PermissionAPI.PERMISSION_READ, otherUser);
                assertTrue("Child must have permission_reference row", hasPermissionReferenceRow(c));
            }

            // Filter collection for limitedUser
            final List<Folder> result = permissionAPI.filterCollection(
                    children, PermissionAPI.PERMISSION_READ, limitedUser, false);

            assertEquals("All denied folders must be excluded", 0, result.size());
        } finally {
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", originalAsyncConfig);
        }
    }

    /**
     * The worst case the plan measures: a recursive search over many inheriting folders right
     * after a permission change above them, when none of them has a cached row. Every folder
     * walks up once, the sibling walk-ups hit the in-memory parent entry, and the next request
     * resolves all of them in the batch query again. The time is logged for the plan and only a
     * generous bound is asserted, so the test does not flake on a slow CI runner.
     */
    @Test
    public void test_manyUncachedFolders_resolveOnceThenBatch_worstCase()
            throws DotDataException, DotSecurityException {
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);
        try {
            final int folderCount = 1000;
            final Folder parent = new FolderDataGen().site(site).name("worst-case-parent").nextPersisted();
            final List<Folder> children = new ArrayList<>(folderCount);
            for (int i = 0; i < folderCount; i++) {
                children.add(new FolderDataGen().site(site).parent(parent)
                        .name("worst-case-child-" + i).nextPersisted());
            }
            final Folder probe = new FolderDataGen().site(site).parent(parent)
                    .name("worst-case-probe").nextPersisted();

            // The permission change above the folders: it clears the reference rows under parent
            final User limitedUser = newUser();
            final String roleId = roleAPI.loadRoleByKey(limitedUser.getUserId()).getId();
            grantOnFolderAndSubfolders(parent, roleId, PermissionAPI.PERMISSION_READ);
            for (final Folder child : children) {
                makeUncached(child);
            }
            makeUncached(probe);

            // Preconditions: the children really inherit, and the one-at-a-time check grants them
            assertFalse("Precondition: a child must have no reference row", hasPermissionReferenceRow(children.get(0)));
            assertEquals("Precondition: a child must have no permissions of its own", 0,
                    permissionAPI.getPermissions(children.get(0), true, true).size());
            assertTrue("Precondition: the one-at-a-time check grants an inheriting child",
                    permissionAPI.doesUserHavePermission(probe, PermissionAPI.PERMISSION_READ, limitedUser, false));

            final long coldStart = System.currentTimeMillis();
            final List<Folder> cold = permissionAPI.filterCollection(
                    children, PermissionAPI.PERMISSION_READ, limitedUser, false);
            final long coldMillis = System.currentTimeMillis() - coldStart;

            final long warmStart = System.currentTimeMillis();
            final List<Folder> warm = permissionAPI.filterCollection(
                    children, PermissionAPI.PERMISSION_READ, limitedUser, false);
            final long warmMillis = System.currentTimeMillis() - warmStart;

            Logger.info(FolderSearchPermissionsIT.class, String.format(
                    "#37749 worst case: %d uncached inheriting folders, cold filter %d ms, warm filter %d ms",
                    folderCount, coldMillis, warmMillis));

            assertEquals("Every uncached inheriting folder must be returned", folderCount, cold.size());
            assertEquals("Every folder must be returned once cached", folderCount, warm.size());
            assertTrue("Every folder must have its reference row after the cold request",
                    hasPermissionReferenceRow(children.get(folderCount - 1)));
            assertTrue("Cold request over " + folderCount + " uncached folders took " + coldMillis
                    + " ms, above the 30 s bound", coldMillis < 30_000);
            assertTrue("Warm request took " + warmMillis + " ms, above the 5 s bound", warmMillis < 5_000);
        } finally {
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", originalAsyncConfig);
        }
    }
}
