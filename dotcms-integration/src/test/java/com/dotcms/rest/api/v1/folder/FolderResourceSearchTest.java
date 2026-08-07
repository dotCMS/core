package com.dotcms.rest.api.v1.folder;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.FolderSearchPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Integration tests for {@code GET /api/v1/folder/search}.
 */
public class FolderResourceSearchTest {

    static HttpServletResponse response;
    static FolderResource resource;
    static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        response = new MockHttpResponse();
        resource = new FolderResource();
        adminUser = TestUserUtils.getAdminUser();
    }

    private HttpServletRequest getHttpRequest(final String userEmail, final String password) {
        final var userEmailAndPassword = userEmail + ":" + password;
        final var request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode(userEmailAndPassword.getBytes())));
        return request;
    }

    /** Convenience wrapper with sensible defaults for most tests. */
    private ResponseEntityPaginatedDataView search(final String name, final String path,
            final boolean recursive, final String siteId) {
        return resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                name, path != null ? path : "/", recursive, siteId,
                "name", "ASC", 1, 40, false);
    }

    // ── Name-filter tests ────────────────────────────────────────────────────

    /**
     * Given Scenario: Admin searches by name filter only (default path + recursive). <br>
     * Expected Result: 200 with matching folders from the entire site.
     */
    @Test
    public void test_searchFolders_nameOnly_returnsMatchingFolders()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).name("alpha-" + ts).nextPersisted();
        new FolderDataGen().site(site).name("alpha-" + ts + "-beta").nextPersisted();
        new FolderDataGen().site(site).name("other-" + ts).nextPersisted();

        // Search term must be a real substring of the folders it should match: both
        // "alpha-<ts>" and "alpha-<ts>-beta" contain "alpha-<ts>", "other-<ts>" does not.
        final var result = search("alpha-" + ts, null, true, site.getIdentifier());

        Assert.assertNotNull(result);
        Assert.assertEquals(2, ((List<?>) result.getEntity()).size());
    }

    /**
     * Given Scenario: No name provided, default path + recursive. <br>
     * Expected Result: Returns all folders in the site.
     */
    @Test
    public void test_searchFolders_noName_returnsAllSiteFolders()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).name("folder-a-" + ts).nextPersisted();
        new FolderDataGen().site(site).name("folder-b-" + ts).nextPersisted();

        final var result = search(null, null, true, site.getIdentifier());

        Assert.assertTrue(((List<?>) result.getEntity()).size() >= 2);
    }

    // ── Path-scope tests ─────────────────────────────────────────────────────

    /**
     * Given Scenario: path=/parent/ + recursive=true, no name filter. <br>
     * Expected Result: Returns all descendants of /parent/.
     */
    @Test
    public void test_searchFolders_pathRecursive_returnsAllDescendants()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent-" + ts).nextPersisted();
        new FolderDataGen().site(site).parent(parent).name("child-a-" + ts).nextPersisted();
        new FolderDataGen().site(site).parent(parent).name("child-b-" + ts).nextPersisted();

        final var result = search(null, "/" + parent.getName() + "/", true, site.getIdentifier());

        Assert.assertEquals(2, ((List<?>) result.getEntity()).size());
    }

    /**
     * Given Scenario: path=/parent/ + recursive=false. <br>
     * Expected Result: Only direct children of /parent/ are returned.
     */
    @Test
    public void test_searchFolders_pathNotRecursive_returnsDirectChildrenOnly()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("par-" + ts).nextPersisted();
        final Folder child = new FolderDataGen().site(site).parent(parent).name("child-" + ts).nextPersisted();
        new FolderDataGen().site(site).parent(child).name("grandchild-" + ts).nextPersisted();

        final var result = search(null, "/" + parent.getName() + "/", false, site.getIdentifier());

        Assert.assertEquals(1, ((List<?>) result.getEntity()).size());
    }

    // ── Combined name + path tests ────────────────────────────────────────────

    /**
     * Given Scenario: name + path combined, recursive. <br>
     * Expected Result: Only folders matching the name AND within the path scope are returned.
     */
    @Test
    public void test_searchFolders_nameAndPath_returnsIntersection()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder assets = new FolderDataGen().site(site).name("assets-" + ts).nextPersisted();
        new FolderDataGen().site(site).parent(assets).name("images-" + ts).nextPersisted(); // matches
        new FolderDataGen().site(site).name("images-root-" + ts).nextPersisted();            // outside path

        final var result = search("images-" + ts, "/" + assets.getName() + "/", true, site.getIdentifier());

        Assert.assertEquals(1, ((List<?>) result.getEntity()).size());
    }

    // ── Pagination test ───────────────────────────────────────────────────────

    /**
     * Given Scenario: 3 matching folders, page=2, per_page=1. <br>
     * Expected Result: Exactly 1 folder returned.
     */
    @Test
    public void test_searchFolders_pagination_page2_returnsCorrectSlice()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        // The ts goes right after the "paged-" prefix so every folder contains the
        // "paged-<ts>" search term (a "%paged-<ts>%" LIKE match).
        for (int i = 0; i < 3; i++) {
            new FolderDataGen().site(site).name(String.format("paged-%d-%02d", ts, i)).nextPersisted();
        }

        final var result = resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "paged-" + ts, "/", true, site.getIdentifier(),
                "name", "ASC", 2, 1, false);

        Assert.assertEquals(1, ((List<?>) result.getEntity()).size());
    }

    // ── Validation tests ──────────────────────────────────────────────────────

    /**
     * Given Scenario: 'siteId' is missing. <br>
     * Expected Result: 400 Bad Request.
     */
    @Test(expected = BadRequestException.class)
    public void test_searchFolders_missingSiteId_returns400() {
        resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "images", "/", true, null,
                "name", "ASC", 1, 40, false);
    }

    /**
     * Given Scenario: 'name' is provided but shorter than 2 characters. <br>
     * Expected Result: 400 Bad Request.
     */
    @Test(expected = BadRequestException.class)
    public void test_searchFolders_nameTooShort_returns400() {
        resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "a", "/", true, "some-site-id",
                "name", "ASC", 1, 40, false);
    }

    // ── hasChildren field tests ───────────────────────────────────────────────

    /**
     * Given Scenario: Parent folder has a child folder; search for the parent as admin. <br>
     * Expected Result: The parent folder's {@code hasChildren} is {@code true}.
     */
    @Test
    public void test_searchFolders_hasChildren_trueWhenSubfolderExists()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("hc-parent-" + ts).nextPersisted();
        new FolderDataGen().site(site).parent(parent).name("hc-child-" + ts).nextPersisted();

        final var result = search("hc-parent-" + ts, null, true, site.getIdentifier());

        Assert.assertNotNull(result);
        @SuppressWarnings("unchecked")
        final List<FolderSearchView> views = (List<FolderSearchView>) result.getEntity();
        Assert.assertEquals(1, views.size());
        Assert.assertTrue("hasChildren should be true when a child folder exists",
                views.get(0).hasChildren());
    }

    /**
     * Given Scenario: Folder has no child folders; search for it as admin. <br>
     * Expected Result: The folder's {@code hasChildren} is {@code false}.
     */
    @Test
    public void test_searchFolders_hasChildren_falseForLeafFolder()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).name("hc-leaf-" + ts).nextPersisted();

        final var result = search("hc-leaf-" + ts, null, true, site.getIdentifier());

        Assert.assertNotNull(result);
        @SuppressWarnings("unchecked")
        final List<FolderSearchView> views = (List<FolderSearchView>) result.getEntity();
        Assert.assertEquals(1, views.size());
        Assert.assertFalse("hasChildren should be false for a leaf folder",
                views.get(0).hasChildren());
    }

    /**
     * Given Scenario: Parent folder has a child folder, but the current user lacks READ on the
     * child. <br>
     * Expected Result: The parent folder's {@code hasChildren} is {@code false} because the child
     * is not visible to this user.
     */
    @Test
    public void test_searchFolders_hasChildren_falseWhenChildExistsButNoReadPermission()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("hc-perm-parent-" + ts).nextPersisted();
        final Folder child  = new FolderDataGen().site(site).parent(parent).name("hc-perm-child-" + ts).nextPersisted();

        // Create a limited user with no additional role assignments
        final User limitedUser = new UserDataGen()
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole())
                .nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser, APILocator.systemUser(), false);

        final String limitedRoleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        final String adminRoleId   = APILocator.getRoleAPI().loadRoleByKey(adminUser.getUserId()).getId();

        // Grant READ on the site to the limited user — searchFolders validates site READ
        // access before searching, so without this the endpoint returns 403 (not the folders).
        APILocator.getPermissionAPI().save(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        site.getPermissionId(), limitedRoleId, PermissionAPI.PERMISSION_READ, true),
                site, APILocator.systemUser(), false);

        // Grant READ on parent to limited user (individual permission breaks inheritance)
        APILocator.getPermissionAPI().save(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        parent.getPermissionId(), limitedRoleId, PermissionAPI.PERMISSION_READ, true),
                parent, APILocator.systemUser(), false);

        // Set individual permissions on child for admin only — limited user has no READ
        APILocator.getPermissionAPI().save(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        child.getPermissionId(), adminRoleId, PermissionAPI.PERMISSION_READ, true),
                child, APILocator.systemUser(), false);

        final var result = resource.searchFolders(
                getHttpRequest(limitedUser.getEmailAddress(), password), response,
                "hc-perm-parent-" + ts, "/", true, site.getIdentifier(),
                "name", "ASC", 1, 40, false);

        Assert.assertNotNull(result);
        @SuppressWarnings("unchecked")
        final List<FolderSearchView> views = (List<FolderSearchView>) result.getEntity();
        Assert.assertEquals(1, views.size());
        Assert.assertFalse("hasChildren should be false when user lacks READ on children",
                views.get(0).hasChildren());
    }

    /**
     * Given Scenario: 'name' is exactly 2 characters, matching a real folder. <br>
     * Expected Result: 200 with the matching folder (the minimum length is accepted, not rejected).
     */
    @Test
    public void test_searchFolders_nameExactlyTwoChars_returnsMatchingFolders()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final String twoCharName = "f" + (ts % 10);
        new FolderDataGen().site(site).name(twoCharName).nextPersisted();
        new FolderDataGen().site(site).name("other-" + ts).nextPersisted();

        final var result = search(twoCharName, null, true, site.getIdentifier());

        Assert.assertNotNull(result);
        Assert.assertEquals(1, ((List<?>) result.getEntity()).size());
    }

    /**
     * Given Scenario: Unauthenticated request. <br>
     * Expected Result: Security exception is thrown.
     */
    @Test
    public void test_searchFolders_unauthenticated_throws() {
        final var request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        try {
            resource.searchFolders(request, response, null, "/", true, "some-site-id",
                    "name", "ASC", 1, 40, false);
            Assert.fail("Expected security exception");
        } catch (final SecurityException e) {
            // expected
        }
    }

    // ── defaultBaseType exposure ─────────────────────────────────────────────

    /**
     * Given Scenario: A folder has its Content Drive upload preference set to {@code DOTASSET};
     * search for it. <br>
     * Expected Result: The {@link FolderSearchView} exposes {@code defaultBaseType = DOTASSET}, so
     * the Content Drive sidebar can read the preference from the search response.
     */
    @Test
    public void test_searchFolders_exposesDefaultBaseType()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site).name("dbt-search-" + ts).nextPersisted();
        folder.setDefaultBaseType("DOTASSET");
        APILocator.getFolderAPI().save(folder, adminUser, false);

        final var result = search("dbt-search-" + ts, null, true, site.getIdentifier());

        Assert.assertNotNull(result);
        @SuppressWarnings("unchecked")
        final List<FolderSearchView> views = (List<FolderSearchView>) result.getEntity();
        Assert.assertEquals(1, views.size());
        Assert.assertEquals("DOTASSET", views.get(0).defaultBaseType());
    }

    /**
     * Given Scenario: A folder with no upload preference; search for it. <br>
     * Expected Result: The {@link FolderSearchView} exposes {@code defaultBaseType = null}.
     */
    @Test
    public void test_searchFolders_defaultBaseTypeNullWhenNoPreference()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).name("dbt-none-" + ts).nextPersisted();

        final var result = search("dbt-none-" + ts, null, true, site.getIdentifier());

        Assert.assertNotNull(result);
        @SuppressWarnings("unchecked")
        final List<FolderSearchView> views = (List<FolderSearchView>) result.getEntity();
        Assert.assertEquals(1, views.size());
        Assert.assertNull(views.get(0).defaultBaseType());
    }

    // ── includePermissions ────────────────────────────────────────────────────

    /**
     * Given Scenario: A limited (non-admin) user holds READ + EDIT on a folder and requests
     * {@code includePermissions=true}. <br>
     * Expected Result: The view carries exactly {@code [READ, EDIT]}. The user must be non-admin:
     * {@code filterCollection} short-circuits for CMS Admin, so an admin-run assertion would pass
     * against an implementation that computes nothing.
     */
    @Test
    public void test_searchFolders_includePermissions_returnsGrantedTypesForLimitedUser()
            throws Exception {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site).name("res-perm-" + ts).nextPersisted();

        final User limitedUser = newLimitedUserWithSiteRead(site);
        grantOnFolder(folder, limitedUser,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT);

        final var result = resource.searchFolders(
                getHttpRequest(limitedUser.getEmailAddress(), LIMITED_USER_PASSWORD), response,
                "res-perm-" + ts, "/", true, site.getIdentifier(),
                "name", "ASC", 1, 40, true);

        @SuppressWarnings("unchecked")
        final List<FolderSearchView> views = (List<FolderSearchView>) result.getEntity();
        Assert.assertEquals(1, views.size());
        Assert.assertEquals(List.of("READ", "EDIT"), views.get(0).permissions());
    }

    /**
     * Given Scenario: The same request without {@code includePermissions}. <br>
     * Expected Result: {@code permissions} is {@code null} — "not requested", distinct from an empty
     * array — while the five folder-detail fields are populated regardless of the flag.
     */
    @Test
    public void test_searchFolders_withoutIncludePermissions_permissionsNullAndDetailFieldsPresent()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site)
                .name("res-nodetail-" + ts)
                .title("Res Detail " + ts)
                .sortOrder(3)
                .fileMasks("*.pdf")
                .showOnMenu(true)
                .nextPersisted();

        final var result = search("res-nodetail-" + ts, null, true, site.getIdentifier());

        @SuppressWarnings("unchecked")
        final List<FolderSearchView> views = (List<FolderSearchView>) result.getEntity();
        Assert.assertEquals(1, views.size());
        final FolderSearchView view = views.get(0);
        Assert.assertNull("permissions must be null when not requested", view.permissions());
        Assert.assertEquals(folder.getTitle(), view.title());
        Assert.assertEquals(folder.getSortOrder(), view.sortOrder());
        Assert.assertEquals(folder.getFilesMasks(), view.filesMasks());
        Assert.assertEquals(folder.getDefaultFileType(), view.defaultFileType());
        Assert.assertEquals(folder.isShowOnMenu(), view.showOnMenu());
    }

    // ── perPage cap when includePermissions=true ──────────────────────────────

    /**
     * Given Scenario: {@code includePermissions=true} with a {@code perPage} above the configured
     * maximum. <br>
     * Expected Result: 400, and the message names the cap so the caller can page correctly rather
     * than silently receiving no permissions.
     */
    @Test
    public void test_searchFolders_includePermissions_perPageOverCap_returns400()
            throws DotDataException, DotSecurityException {
        final int cap = Config.getIntProperty(FolderResource.PERMISSIONS_MAX_PER_PAGE_KEY,
                FolderResource.PERMISSIONS_MAX_PER_PAGE_DEFAULT);
        try {
            resource.searchFolders(
                    getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                    null, "/", true, site().getIdentifier(),
                    "name", "ASC", 1, cap + 1, true);
            Assert.fail("Expected a BadRequestException for perPage above the cap");
        } catch (final BadRequestException e) {
            final String clientMessage = errorMessageOf(e);
            Assert.assertTrue("the 400 must name the cap, got: " + clientMessage,
                    clientMessage.contains(String.valueOf(cap)));
            Assert.assertTrue("the 400 must name the flag that triggered it, got: " + clientMessage,
                    clientMessage.contains(FolderResource.INCLUDE_PERMISSIONS_PARAM));
        }
    }

    /**
     * Given Scenario: The same over-cap {@code perPage} with {@code includePermissions} off. <br>
     * Expected Result: 200 — the cap constrains only the flag, never existing callers.
     */
    @Test
    public void test_searchFolders_perPageOverCap_withoutFlag_succeeds()
            throws DotDataException, DotSecurityException {
        final int cap = Config.getIntProperty(FolderResource.PERMISSIONS_MAX_PER_PAGE_KEY,
                FolderResource.PERMISSIONS_MAX_PER_PAGE_DEFAULT);
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).name("res-cap-off-" + ts).nextPersisted();

        final var result = resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "res-cap-off-" + ts, "/", true, site.getIdentifier(),
                "name", "ASC", 1, cap + 1, false);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, ((List<?>) result.getEntity()).size());
    }

    /**
     * Given Scenario: {@code includePermissions=true} with {@code perPage} exactly at the cap. <br>
     * Expected Result: 200 — the bound is inclusive.
     */
    @Test
    public void test_searchFolders_includePermissions_perPageAtCap_succeeds()
            throws DotDataException, DotSecurityException {
        final int cap = Config.getIntProperty(FolderResource.PERMISSIONS_MAX_PER_PAGE_KEY,
                FolderResource.PERMISSIONS_MAX_PER_PAGE_DEFAULT);
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).name("res-cap-at-" + ts).nextPersisted();

        final var result = resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "res-cap-at-" + ts, "/", true, site.getIdentifier(),
                "name", "ASC", 1, cap, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, ((List<?>) result.getEntity()).size());
    }

    /**
     * Given Scenario: {@code includePermissions=true} with {@code per_page=0} while
     * {@code dotcms.paginator.rows} is raised above the cap. <br>
     * Expected Result: 400. {@link com.dotcms.util.PaginationUtil} turns any {@code perPage <= 0}
     * into {@code dotcms.paginator.rows}, so validating the raw parameter would let this through and
     * then page above the cap. The guard must run against the effective page size.
     */
    @Test
    public void test_searchFolders_includePermissions_perPageZero_validatesEffectivePageSize()
            throws DotDataException, DotSecurityException {
        final int cap = Config.getIntProperty(FolderResource.PERMISSIONS_MAX_PER_PAGE_KEY,
                FolderResource.PERMISSIONS_MAX_PER_PAGE_DEFAULT);
        final int originalRows = Config.getIntProperty(WebKeys.DOTCMS_PAGINATION_ROWS,
                FolderResource.DEFAULT_PAGINATION_ROWS);
        Config.setProperty(WebKeys.DOTCMS_PAGINATION_ROWS, cap + 1);
        try {
            resource.searchFolders(
                    getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                    null, "/", true, site().getIdentifier(),
                    "name", "ASC", 1, 0, true);
            Assert.fail("Expected a BadRequestException: per_page=0 resolves to a page above the cap");
        } catch (final BadRequestException e) {
            final String clientMessage = errorMessageOf(e);
            Assert.assertTrue("the 400 must report the effective page size, got: " + clientMessage,
                    clientMessage.contains(String.valueOf(cap + 1)));
        } finally {
            Config.setProperty(WebKeys.DOTCMS_PAGINATION_ROWS, originalRows);
        }
    }

    /**
     * Given Scenario: {@code includePermissions=true} with {@code per_page=0} under the default
     * {@code dotcms.paginator.rows}. <br>
     * Expected Result: 200 — the effective page size (10 by default) is well under the cap, so the
     * stricter guard must not reject ordinary callers that omit the parameter.
     */
    @Test
    public void test_searchFolders_includePermissions_perPageZero_underDefaultRows_succeeds()
            throws DotDataException, DotSecurityException {
        final long ts = System.currentTimeMillis();
        final Host site = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(site).name("res-zero-" + ts).nextPersisted();

        final var result = resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "res-zero-" + ts, "/", true, site.getIdentifier(),
                "name", "ASC", 1, 0, true);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, ((List<?>) result.getEntity()).size());
    }

    /**
     * Given Scenario: The cap property is lowered at runtime, then a request is made just above the
     * new value. <br>
     * Expected Result: 400 — the cap is read from configuration, not hardcoded.
     */
    @Test
    public void test_searchFolders_includePermissions_capIsConfigDriven()
            throws DotDataException, DotSecurityException {
        final int original = Config.getIntProperty(FolderResource.PERMISSIONS_MAX_PER_PAGE_KEY,
                FolderResource.PERMISSIONS_MAX_PER_PAGE_DEFAULT);
        Config.setProperty(FolderResource.PERMISSIONS_MAX_PER_PAGE_KEY, 5);
        try {
            resource.searchFolders(
                    getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                    null, "/", true, site().getIdentifier(),
                    "name", "ASC", 1, 6, true);
            Assert.fail("Expected a BadRequestException once the cap was lowered to 5");
        } catch (final BadRequestException e) {
            final String clientMessage = errorMessageOf(e);
            Assert.assertTrue("the 400 must name the configured cap, got: " + clientMessage,
                    clientMessage.contains("cannot exceed 5 "));
        } finally {
            Config.setProperty(FolderResource.PERMISSIONS_MAX_PER_PAGE_KEY, original);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static final String LIMITED_USER_PASSWORD = "admin";

    /**
     * Returns the message the client actually receives for a {@link BadRequestException}.
     *
     * <p>Not {@code getMessage()}: {@code HttpStatusCodeException} extends
     * {@code WebApplicationException}, whose {@code getMessage()} is the generic
     * "HTTP 400 Bad Request". The caller-facing text travels in the {@code error-message} response
     * header (and in the JSON entity), so that is what these assertions check.
     */
    private static String errorMessageOf(final BadRequestException e) {
        final String message = e.getResponse().getHeaderString("error-message");
        Assert.assertNotNull("the 400 must carry an error-message header", message);
        return message;
    }

    /** Lazily-created site for tests that only need some valid site id. */
    private Host site() throws DotDataException, DotSecurityException {
        return new SiteDataGen().nextPersisted();
    }

    /**
     * Creates a backend, non-admin user with READ on {@code site}. The site grant is required
     * because {@code searchFolders} validates site READ access before searching.
     */
    private static User newLimitedUserWithSiteRead(final Host site) throws Exception {
        final User limitedUser = new UserDataGen()
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole())
                .nextPersisted();
        limitedUser.setPassword(LIMITED_USER_PASSWORD);
        APILocator.getUserAPI().save(limitedUser, APILocator.systemUser(), false);

        final String roleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        APILocator.getPermissionAPI().save(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        site.getPermissionId(), roleId, PermissionAPI.PERMISSION_READ, true),
                site, APILocator.systemUser(), false);
        return limitedUser;
    }

    /**
     * Replaces the individual permissions the user's role holds on {@code folder} with {@code bits};
     * setting individual permissions breaks inheritance, so the grants end up exact.
     */
    private static void grantOnFolder(final Folder folder, final User user, final int bits)
            throws DotDataException, DotSecurityException {
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId();
        APILocator.getPermissionAPI().save(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        folder.getPermissionId(), roleId, bits, true),
                folder, APILocator.systemUser(), false);
    }
}
