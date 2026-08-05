package com.dotmarketing.portlets.folders.business;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.api.v1.folder.FolderSearchView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.folders.business.FolderSearchParams;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link FolderAPIImpl#searchFolders}.
 */
public class FolderAPIImplFilterTest {

    private static FolderAPI folderAPI;
    private static User adminUser;
    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        folderAPI = APILocator.getFolderAPI();
        adminUser = TestUserUtils.getAdminUser();
        site = new SiteDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link FolderAPIImpl#searchFolders} <br>
     * Given Scenario: Admin user searches for folders by partial name across entire site. <br>
     * Expected Result: Only folders whose name contains the filter are returned; non-matching folder is excluded.
     */
    @Test
    public void test_searchFolders_adminUser_returnsMatchingFolders()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(freshSite).name("media").nextPersisted();
        new FolderDataGen().site(freshSite).name("media-archive").nextPersisted();
        new FolderDataGen().site(freshSite).name("other").nextPersisted();

        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("media")
                .siteId(freshSite.getIdentifier())
                .user(adminUser)
                .build());

        assertEquals(2, result.getTotalResults());
        assertEquals(2, result.size());
    }

    /**
     * Method to test: {@link FolderAPI#searchFolders} <br>
     * Given Scenario: No name filter — returns all folders in site. <br>
     * Expected Result: At least the folders created for this test are returned.
     */
    @Test
    public void test_searchFolders_noName_returnsAllFolders()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host freshSite = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(freshSite).name("folder-a-" + ts).nextPersisted();
        new FolderDataGen().site(freshSite).name("folder-b-" + ts).nextPersisted();

        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .siteId(freshSite.getIdentifier())
                .user(adminUser)
                .build());

        assertTrue(result.size() >= 2);
    }

    /**
     * Method to test: {@link FolderAPIImpl#searchFolders} <br>
     * Given Scenario: A limited user has READ permission on one of two name-matching folders. <br>
     * Expected Result: Only the accessible folder is returned; the restricted one is excluded.
     */
    @Test
    public void test_searchFolders_limitedUser_respectsPermissions()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder visible    = new FolderDataGen().site(freshSite).name("folder-visible").nextPersisted();
        final Folder restricted = new FolderDataGen().site(freshSite).name("folder-restricted").nextPersisted();

        final User limitedUser = new UserDataGen().nextPersisted();
        final Permission readPerm = new Permission(
                visible.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(readPerm, visible, adminUser, false);

        // "folder" matches both folder names — permission filtering decides what is returned
        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("folder")
                .siteId(freshSite.getIdentifier())
                .user(limitedUser)
                .build());

        assertEquals(1, result.size());
        assertEquals(visible.getIdentifier(), result.get(0).id());
    }

    /**
     * Method to test: {@link FolderAPI#searchFolders} <br>
     * Given Scenario: Admin user queries a filter with no matching folders. <br>
     * Expected Result: Empty list and totalResults = 0.
     */
    @Test
    public void test_searchFolders_noMatch_returnsEmptyList()
            throws DotDataException, DotSecurityException {
        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("zzz-absolutely-no-match-" + System.currentTimeMillis())
                .siteId(site.getIdentifier())
                .user(adminUser)
                .build());

        assertEquals(0, result.getTotalResults());
        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link FolderAPIImpl#searchFolders} <br>
     * Given Scenario: Admin searches within a path scope (recursive). <br>
     * Expected Result: Only the folder within the path subtree is returned; the one at root is excluded.
     */
    @Test
    public void test_searchFolders_withPathScope_returnsOnlyDescendants()
            throws DotDataException, DotSecurityException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder assets = new FolderDataGen().site(freshSite).name("assets").nextPersisted();
        new FolderDataGen().site(freshSite).parent(assets).name("images").nextPersisted(); // inside scope
        new FolderDataGen().site(freshSite).name("images-root").nextPersisted();           // outside scope

        // "images" matches both folders by name — the path scope filters out the root one
        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("images")
                .path("/" + assets.getName() + "/")
                .recursive(true)
                .siteId(freshSite.getIdentifier())
                .user(adminUser)
                .build());

        assertEquals(1, result.getTotalResults());
        assertEquals(1, result.size());
    }

    /**
     * Method to test: {@link FolderAPIImpl#searchFolders} <br>
     * Given Scenario: Admin user searches for an existing folder. <br>
     * Expected Result: Exactly one result is returned with non-null id, inode, name, and path.
     */
    @Test
    public void test_searchFolders_resultContainsExpectedViewFields()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        new FolderDataGen().site(site).name("viewcheck-" + ts).nextPersisted();

        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("viewcheck-" + ts)
                .siteId(site.getIdentifier())
                .user(adminUser)
                .build());

        assertEquals(1, result.size());
        assertEquals(1, result.getTotalResults());
        final FolderSearchView view = result.get(0);
        assertNotNull(view.id());
        assertNotNull(view.inode());
        assertNotNull(view.name());
        assertNotNull(view.path());
        assertFalse("leaf folder has no children", view.hasChildren());
    }

    /**
     * Method to test: {@link FolderAPI#searchFolders} <br>
     * Given Scenario: 5 matching folders, paginated with limit=2 and offset=2. <br>
     * Expected Result: totalResults = 5, only 2 items returned.
     */
    @Test
    public void test_searchFolders_pagination_returnsCorrectSlice()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            new FolderDataGen().site(site).name(String.format("page-folder-%02d-%d", i, ts)).nextPersisted();
        }

        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("page-folder-")
                .siteId(site.getIdentifier())
                .user(adminUser)
                .limit(2)
                .offset(2)
                .build());

        assertEquals(5, result.getTotalResults());
        assertEquals(2, result.size());
    }

    // ── hasChildren field ─────────────────────────────────────────────────────

    /**
     * Method to test: {@link FolderAPIImpl#searchFolders} <br>
     * Given Scenario: A parent folder has one direct child; the admin searches for the parent. <br>
     * Expected Result: The returned view has {@code hasChildren = true}.
     */
    @Test
    public void test_searchFolders_hasChildren_trueForParentWithChild()
            throws DotDataException, DotSecurityException {
        final long ts      = System.currentTimeMillis();
        final Host fresh   = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(fresh).name("hc-parent-" + ts).nextPersisted();
        new FolderDataGen().site(fresh).parent(parent).name("hc-child-" + ts).nextPersisted();

        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("hc-parent-" + ts)
                .siteId(fresh.getIdentifier())
                .user(adminUser)
                .build());

        assertEquals(1, result.size());
        assertTrue("parent folder with a child should have hasChildren=true",
                result.get(0).hasChildren());
    }

    /**
     * Method to test: {@link FolderAPIImpl#searchFolders} <br>
     * Given Scenario: A folder has no children; the admin searches for it. <br>
     * Expected Result: The returned view has {@code hasChildren = false}.
     */
    @Test
    public void test_searchFolders_hasChildren_falseForLeafFolder()
            throws DotDataException, DotSecurityException {
        final long ts    = System.currentTimeMillis();
        final Host fresh = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(fresh).name("hc-leaf-" + ts).nextPersisted();

        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("hc-leaf-" + ts)
                .siteId(fresh.getIdentifier())
                .user(adminUser)
                .build());

        assertEquals(1, result.size());
        assertFalse("leaf folder should have hasChildren=false", result.get(0).hasChildren());
    }

    /**
     * Method to test: {@link FolderAPIImpl#searchFolders} <br>
     * Given Scenario: A child folder exists but the requesting user has no READ permission on it. <br>
     * Expected Result: The parent's {@code hasChildren} is {@code false} because the child is not
     * visible to this user.
     */
    @Test
    public void test_searchFolders_hasChildren_falseWhenChildNotReadable()
            throws DotDataException, DotSecurityException {
        final long ts    = System.currentTimeMillis();
        final Host fresh = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(fresh).name("hc-perm-parent-" + ts).nextPersisted();
        final Folder child  = new FolderDataGen().site(fresh).parent(parent).name("hc-perm-child-" + ts).nextPersisted();

        final User limitedUser  = new UserDataGen().nextPersisted();
        final String limitedRoleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        final String adminRoleId   = APILocator.getRoleAPI().loadRoleByKey(adminUser.getUserId()).getId();

        // Give limited user individual READ on parent only
        APILocator.getPermissionAPI().save(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        parent.getPermissionId(), limitedRoleId, PermissionAPI.PERMISSION_READ, true),
                parent, adminUser, false);

        // Set individual READ on child for admin only — limited user cannot see it
        APILocator.getPermissionAPI().save(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        child.getPermissionId(), adminRoleId, PermissionAPI.PERMISSION_READ, true),
                child, adminUser, false);

        final var result = folderAPI.searchFolders(FolderSearchParams.builder()
                .name("hc-perm-parent-" + ts)
                .siteId(fresh.getIdentifier())
                .user(limitedUser)
                .build());

        assertEquals(1, result.size());
        assertFalse("hasChildren should be false when user cannot read any child",
                result.get(0).hasChildren());
    }
}
