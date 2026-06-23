package com.dotcms.rest.api.v1.folder;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.FolderSearchPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
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
                "name", "ASC", 1, 40);
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
        new FolderDataGen().site(site).name("alpha-beta-" + ts).nextPersisted();
        new FolderDataGen().site(site).name("other-" + ts).nextPersisted();

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
        for (int i = 0; i < 3; i++) {
            new FolderDataGen().site(site).name(String.format("paged-%02d-%d", (Integer) i, (Long) ts)).nextPersisted();
        }

        final var result = resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "paged-" + ts, "/", true, site.getIdentifier(),
                "name", "ASC", 2, 1);

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
                "name", "ASC", 1, 40);
    }

    /**
     * Given Scenario: 'name' is provided but shorter than 3 characters. <br>
     * Expected Result: 400 Bad Request.
     */
    @Test(expected = BadRequestException.class)
    public void test_searchFolders_nameTooShort_returns400() {
        resource.searchFolders(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                "ab", "/", true, "some-site-id",
                "name", "ASC", 1, 40);
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
                    "name", "ASC", 1, 40);
            Assert.fail("Expected security exception");
        } catch (final SecurityException e) {
            // expected
        }
    }
}
