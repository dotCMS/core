package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for {@code showLinks} support in the Content Drive search API (issue #36991).
 *
 * <p>Menu Links used to be reachable only through the legacy {@code /api/v1/browser}. They are now
 * a third pagination source in {@link ContentDriveHelper#driveSearch} alongside folders and
 * contentlets, each with its own cursor, count and {@code hasMore} flag.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li><b>Opt-in</b> — links appear only with {@code showLinks: true}; the flag defaults to
 *   {@code false} so existing callers are unaffected.</li>
 *   <li><b>Permissions</b> — links the requesting user cannot READ are filtered out.</li>
 *   <li><b>Version flags</b> — {@code live} / {@code archived} produce exactly the same link set as
 *   the legacy {@code /api/v1/browser} path for the equivalent flags.</li>
 *   <li><b>Pagination</b> — links participate in {@code maxResults} and page to exhaustion via
 *   {@code linkCursor} with no duplicates and no gaps.</li>
 *   <li><b>baseTypes interaction</b> — links are orthogonal to {@code baseTypes}; an empty
 *   {@code baseTypes} array yields a links-only result.</li>
 *   <li><b>Suppression</b> — {@code mimeTypes} and {@code workflow} filters drop links, which can
 *   satisfy neither.</li>
 *   <li><b>Shape and ordering</b> — links carry the Content Drive map shape and honour
 *   {@code sortBy} alongside the other sources.</li>
 * </ul>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveLinksTest extends IntegrationTestBase {

    private static final String LINK_MIME_TYPE = "application/dotlink";

    /** Titles are deliberately alphabetical so ordering assertions are readable. */
    private static final List<String> LINK_TITLES =
            List.of("aaaLink", "bbbLink", "cccLink", "dddLink", "eeeLink");

    private static ContentDriveHelper contentDriveHelper;
    private static BrowserAPI browserAPI;
    private static PermissionAPI permissionAPI;
    private static User systemUser;

    private static Host testSite;
    private static Folder testFolder;
    private static String testAssetPath;

    /** The links under {@link #testFolder}, keyed by title. */
    private static Map<String, Link> links;
    private static Set<String> allLinkIds;

    private static ContentType testType;
    private static int subFolderCount;

    /** A searchable text field on {@link #testType}, so {@code userSearchable} can be exercised. */
    private static final String TEXT_VAR = "topic";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        contentDriveHelper = new ContentDriveHelper();
        browserAPI = APILocator.getBrowserAPI();
        permissionAPI = APILocator.getPermissionAPI();
        systemUser = APILocator.getUserAPI().getSystemUser();

        final String uniqueId = String.valueOf(System.currentTimeMillis());

        testSite = new SiteDataGen().name("drive-links-" + uniqueId + ".local").nextPersisted();
        testFolder = new FolderDataGen().name("driveLinksFolder_" + uniqueId).site(testSite)
                .nextPersisted();
        testAssetPath = "//" + testSite.getHostname() + testFolder.getPath();

        // Menu links — the subject under test.
        links = LINK_TITLES.stream().collect(Collectors.toMap(title -> title,
                title -> new LinkDataGen(testFolder)
                        .hostId(testSite.getIdentifier())
                        .title(title)
                        .nextPersisted()));
        allLinkIds = links.values().stream().map(Link::getIdentifier)
                .collect(Collectors.toSet());

        // Two subfolders and two contentlets, so links are exercised in a mixed folder rather
        // than in isolation.
        new FolderDataGen().name("subA_" + uniqueId).parent(testFolder).nextPersisted();
        new FolderDataGen().name("subB_" + uniqueId).parent(testFolder).nextPersisted();
        subFolderCount = 2;

        testType = new ContentTypeDataGen()
                .name("DriveLinksType_" + uniqueId)
                .velocityVarName("driveLinksType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();

        new FieldDataGen().type(TextField.class).name(TEXT_VAR).velocityVarName(TEXT_VAR)
                .contentTypeId(testType.id()).searchable(true).indexed(true).nextPersisted();

        new ContentletDataGen(testType.id()).setProperty("title", "content one " + uniqueId)
                .setProperty(TEXT_VAR, "angular").folder(testFolder).nextPersisted();
        new ContentletDataGen(testType.id()).setProperty("title", "content two " + uniqueId)
                .setProperty(TEXT_VAR, "angular").folder(testFolder).nextPersisted();

        Logger.info(ContentDriveLinksTest.class,
                "Link test data ready under " + testAssetPath);
    }

    // ---------------------------------------------------------------- helpers

    private DriveRequestForm.Builder baseRequest() {
        return DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .live(false)
                .archived(false)
                .offset(0)
                .maxResults(100);
    }

    private PaginatedContents search(final DriveRequestForm request)
            throws DotDataException, DotSecurityException {
        return search(request, systemUser);
    }

    private PaginatedContents search(final DriveRequestForm request, final User user)
            throws DotDataException, DotSecurityException {
        return contentDriveHelper.driveSearch(request, user);
    }

    /** True when the map is a menu link rather than a folder or contentlet. */
    private static boolean isLink(final Map<String, Object> item) {
        return LINK_MIME_TYPE.equals(item.get("mimeType"));
    }

    private static List<Map<String, Object>> linksIn(final PaginatedContents results) {
        return results.list.stream().filter(ContentDriveLinksTest::isLink)
                .collect(Collectors.toList());
    }

    /**
     * Link identifiers in the response. Content Drive views strip {@code inode}, so identifier is
     * the stable key.
     */
    private static Set<String> linkIdsIn(final PaginatedContents results) {
        return linksIn(results).stream().map(item -> (String) item.get("identifier"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The link set the legacy {@code /api/v1/browser} path returns for the given version flags,
     * built the way {@code BrowserResource} builds its query.
     */
    private Set<String> legacyBrowserLinkIds(final boolean live, final boolean archived)
            throws DotDataException, DotSecurityException {
        final BrowserQuery query = BrowserQuery.builder()
                .withUser(systemUser)
                .respectFrontEndRoles(false)
                .withHostOrFolderId(testFolder.getInode())
                .showLinks(true)
                .showFolders(false)
                .showContent(false)
                .showWorking(!live)
                .showArchived(archived)
                .maxResults(100)
                .build();

        final Map<String, Object> legacy = browserAPI.getFolderContent(query);
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = (List<Map<String, Object>>) legacy.get("list");
        return list.stream().filter(ContentDriveLinksTest::isLink)
                .map(item -> (String) item.get("identifier"))
                .collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------ tests

    /**
     * <b>Given</b> a folder holding menu links, folders and contentlets.
     * <b>When</b> searched with {@code showLinks: true}.
     * <b>Then</b> every link is returned and {@code linkCount} reports how many.
     */
    @Test
    public void testShowLinksTrueReturnsLinks() throws Exception {
        final PaginatedContents results = search(baseRequest().showLinks(true).build());

        assertEquals("Every link under the folder should be returned",
                allLinkIds, new HashSet<>(linkIdsIn(results)));
        assertEquals("linkCount must match the links placed in the page",
                LINK_TITLES.size(), results.linkCount);
        assertFalse("All links fit in one page of 100", results.hasMoreLinks);
        assertEquals("nextLinkCursor should sit past the last returned link",
                LINK_TITLES.size(), results.nextLinkCursor);
    }

    /**
     * <b>Given</b> the same folder.
     * <b>When</b> {@code showLinks} is omitted entirely.
     * <b>Then</b> no links are returned — the regression guard for every existing consumer
     * (Content Drive, AssetPicker) that never sends the flag.
     */
    @Test
    public void testShowLinksDefaultsToFalse() throws Exception {
        final PaginatedContents results = search(baseRequest().build());

        assertTrue("Links must not leak into a request that never asked for them",
                linksIn(results).isEmpty());
        assertEquals(0, results.linkCount);
        assertFalse(results.hasMoreLinks);
        assertEquals("An untouched link cursor must stay at 0", 0, results.nextLinkCursor);
    }

    /**
     * <b>When</b> {@code showLinks} is explicitly {@code false}.
     * <b>Then</b> no links are returned.
     */
    @Test
    public void testShowLinksFalseExcludesLinks() throws Exception {
        final PaginatedContents results = search(baseRequest().showLinks(false).build());

        assertTrue(linksIn(results).isEmpty());
        assertEquals(0, results.linkCount);
    }

    /**
     * <b>Given</b> a limited user granted READ on a site and folder plus READ on two of the
     * folder's three links, with the third left ungranted.
     * <b>When</b> the limited user searches with {@code showLinks: true}.
     * <b>Then</b> only the links they can READ come back.
     *
     * <p>Uses its own site and folder rather than the shared fixtures, because it rewrites
     * permission sets and must not leak that into the other tests.</p>
     */
    @Test
    public void testLinksFilteredByReadPermission() throws Exception {
        // Permission reference updates are async by default, which would make the grants below
        // race the assertions. MenuLinkAPITest pins the same flag for the same reason.
        final boolean asyncOldValue =
                Config.getBooleanProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", true);
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);
        try {
            final String uniqueId = String.valueOf(System.currentTimeMillis());
            final Host site = new SiteDataGen().name("drive-links-perm-" + uniqueId + ".local")
                    .nextPersisted();
            final Folder folder = new FolderDataGen().name("permFolder_" + uniqueId).site(site)
                    .nextPersisted();
            final String assetPath = "//" + site.getHostname() + folder.getPath();

            final Link visibleOne = new LinkDataGen(folder).hostId(site.getIdentifier())
                    .title("permVisibleOne").nextPersisted();
            final Link visibleTwo = new LinkDataGen(folder).hostId(site.getIdentifier())
                    .title("permVisibleTwo").nextPersisted();
            // Never granted to the limited user -- this is the link that must be filtered out.
            final Link hiddenLink = new LinkDataGen(folder).hostId(site.getIdentifier())
                    .title("permHidden").nextPersisted();

            // A fresh user in a fresh empty role: the shared TestUserUtils users are cached
            // across the suite and carry type-level grants that would mask the filtering.
            final Role limitedRole = new RoleDataGen().nextPersisted();
            final User limitedUser = new UserDataGen().roles(limitedRole).nextPersisted();
            final String limitedUserRoleId =
                    APILocator.getRoleAPI().getUserRole(limitedUser).getId();

            // READ on the site so the asset path resolves, and READ on the folder so
            // FolderAPI.getLinks' parent check passes. save() appends, leaving other grants intact.
            permissionAPI.save(new Permission(site.getPermissionId(), limitedUserRoleId,
                    PermissionAPI.PERMISSION_READ), site, systemUser, false);
            permissionAPI.save(new Permission(folder.getPermissionId(), limitedUserRoleId,
                    PermissionAPI.PERMISSION_READ), folder, systemUser, false);

            // Deliberately NO Link-typed inheritable grant on the folder: dotCMS keys inheritable
            // grants on the child's class name, so a folder-only grant leaves every child Link
            // unreadable. READ is therefore opted into per link, and hiddenLink stays unreadable
            // simply by being left alone -- more robust than granting broadly then revoking, since
            // both save() overloads only append (save(Collection) loops over save(Permission)).
            for (final Link readable : List.of(visibleOne, visibleTwo)) {
                permissionAPI.save(new Permission(readable.getPermissionId(), limitedUserRoleId,
                        PermissionAPI.PERMISSION_READ), readable, systemUser, false);
            }

            // Sanity-check both halves of the fixture, so a broken setup points here rather than at
            // the API under test.
            assertTrue("Fixture error: limited user should have READ on an explicitly granted link",
                    permissionAPI.doesUserHavePermission(visibleOne,
                            PermissionAPI.PERMISSION_READ, limitedUser, false));
            assertFalse("Fixture error: limited user should NOT have READ on the ungranted link",
                    permissionAPI.doesUserHavePermission(hiddenLink,
                            PermissionAPI.PERMISSION_READ, limitedUser, false));

            final Set<String> visible = linkIdsIn(search(DriveRequestForm.builder()
                    .assetPath(assetPath)
                    .showLinks(true)
                    .showFolders(false)
                    .baseTypes(List.of())
                    .maxResults(100)
                    .build(), limitedUser));

            assertFalse("The unreadable link must be filtered out",
                    visible.contains(hiddenLink.getIdentifier()));
            assertTrue("The readable links must still be returned",
                    visible.containsAll(List.of(visibleOne.getIdentifier(),
                            visibleTwo.getIdentifier())));
            assertEquals("Exactly the two readable links should come back", 2, visible.size());
        } finally {
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", asyncOldValue);
        }
    }

    /**
     * <b>Given</b> the same fixtures.
     * <b>When</b> searched across every {@code live} / {@code archived} combination.
     * <b>Then</b> the drive returns exactly the link set the legacy {@code /api/v1/browser} path
     * returns for the equivalent flags — the AC is parity with legacy, not a new interpretation.
     */
    @Test
    public void testLinksHonourLiveAndArchivedLikeLegacyBrowser() throws Exception {
        final boolean[] flags = {false, true};
        for (final boolean live : flags) {
            for (final boolean archived : flags) {
                final Set<String> driveIds = new HashSet<>(linkIdsIn(search(baseRequest()
                        .showLinks(true).live(live).archived(archived).build())));
                final Set<String> legacyIds = legacyBrowserLinkIds(live, archived);

                assertEquals(String.format(
                                "Drive and legacy browser must agree on links for live=%s archived=%s",
                                live, archived),
                        legacyIds, driveIds);
            }
        }
    }

    /**
     * <b>Given</b> a folder with more links than {@code maxResults}.
     * <b>When</b> the client pages by feeding {@code nextLinkCursor} back as {@code linkCursor}.
     * <b>Then</b> every link is returned exactly once — no duplicates, no gaps — and paging
     * terminates.
     */
    @Test
    public void testPagingLinksToExhaustion() throws Exception {
        final List<String> collected = new ArrayList<>();
        int linkCursor = 0;
        boolean hasMore = true;
        int guard = 0;

        while (hasMore) {
            assertTrue("Paging failed to terminate", ++guard <= LINK_TITLES.size() + 5);

            // showFolders/showContent off so the whole 2-item budget belongs to links, forcing
            // several pages.
            final PaginatedContents page = search(baseRequest()
                    .showLinks(true)
                    .showFolders(false)
                    .baseTypes(List.of())
                    .maxResults(2)
                    .linkCursor(linkCursor)
                    .build());

            final Set<String> pageIds = linkIdsIn(page);
            assertEquals("linkCount must match the links actually in the page",
                    pageIds.size(), page.linkCount);
            assertTrue("A page must never exceed maxResults", page.linkCount <= 2);

            collected.addAll(pageIds);
            assertTrue("Cursor must advance while more links remain",
                    page.nextLinkCursor > linkCursor || !page.hasMoreLinks);

            linkCursor = page.nextLinkCursor;
            hasMore = page.hasMoreLinks;
        }

        assertEquals("Paging must yield every link exactly once — no duplicates",
                collected.size(), new HashSet<>(collected).size());
        assertEquals("Paging must yield every link — no gaps",
                allLinkIds, new HashSet<>(collected));
    }

    /**
     * <b>Given</b> a page budget entirely consumed by folders.
     * <b>When</b> links are also requested.
     * <b>Then</b> no link is placed in the page, but {@code hasMoreLinks} reports the remainder and
     * the cursor stays put so the next request re-reads from the same position.
     */
    @Test
    public void testFoldersFillingPageStillReportsMoreLinks() throws Exception {
        final PaginatedContents results = search(baseRequest()
                .showLinks(true)
                .showFolders(true)
                .baseTypes(List.of())
                .maxResults(subFolderCount)
                .build());

        assertEquals("Folders should have consumed the whole budget",
                subFolderCount, results.folderCount);
        assertEquals("No link fits once folders filled the page", 0, results.linkCount);
        assertTrue("The client must still learn links remain", results.hasMoreLinks);
        assertEquals("An unconsumed link cursor must not advance", 0, results.nextLinkCursor);
    }

    /**
     * <b>Given</b> {@code showLinks: true} with an empty {@code baseTypes} array and folders off.
     * <b>Then</b> the result is links only — the documented way to express a links-only request,
     * since Links are not a {@code BaseContentType}.
     */
    @Test
    public void testLinksOnlyWhenBaseTypesIsEmpty() throws Exception {
        final PaginatedContents results = search(baseRequest()
                .showLinks(true)
                .showFolders(false)
                .baseTypes(List.of())
                .build());

        assertEquals("Every returned item should be a link",
                results.list.size(), linksIn(results).size());
        assertEquals(allLinkIds, new HashSet<>(linkIdsIn(results)));
        assertEquals(0, results.folderCount);
        assertEquals(0, results.contentCount);
    }

    /**
     * <b>Given</b> {@code showLinks: true} with no {@code baseTypes} at all.
     * <b>Then</b> links are returned <i>alongside</i> content of every base type — the flag is
     * orthogonal to {@code baseTypes} rather than a value within it.
     */
    @Test
    public void testShowLinksWithNoBaseTypesReturnsLinksAndContent() throws Exception {
        final PaginatedContents results = search(baseRequest().showLinks(true).build());

        assertEquals(LINK_TITLES.size(), results.linkCount);
        assertTrue("Content should still be returned when baseTypes is omitted",
                results.contentCount > 0);
    }

    /**
     * <b>When</b> a {@code mimeTypes} filter is present.
     * <b>Then</b> links are suppressed — a Link carries no file MIME type and could never match.
     */
    @Test
    public void testMimeTypesSuppressesLinks() throws Exception {
        final PaginatedContents results = search(baseRequest()
                .showLinks(true)
                .mimeTypes(List.of("image/jpeg"))
                .build());

        assertTrue("Links cannot satisfy a mimeType filter and must be dropped",
                linksIn(results).isEmpty());
        assertEquals(0, results.linkCount);
    }

    /**
     * <b>When</b> a {@code workflow} filter is present.
     * <b>Then</b> links are suppressed, exactly as folders already are — neither carries workflow
     * state.
     */
    @Test
    public void testWorkflowFilterSuppressesLinks() throws Exception {
        final PaginatedContents results = search(baseRequest()
                .showLinks(true)
                .workflow(List.of(WorkflowFilterForm.builder()
                        .scheme(APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId())
                        .build()))
                .build());

        assertTrue("Links carry no workflow state and must be dropped",
                linksIn(results).isEmpty());
        assertEquals(0, results.linkCount);
        assertEquals("Folders are dropped by a workflow filter too", 0, results.folderCount);
    }

    /**
     * <b>When</b> a {@code userSearchable} field filter is present.
     * <b>Then</b> links are suppressed — field filters resolve against a single content type and
     * links have no fields, so they could never satisfy one.
     */
    @Test
    public void testUserSearchableFilterSuppressesLinks() throws Exception {
        final PaginatedContents results = search(baseRequest()
                .showLinks(true)
                .contentTypes(List.of(testType.variable()))
                .userSearchable(Map.of(TEXT_VAR, "angular"))
                .build());

        assertTrue("Links have no fields and must be dropped by a field filter",
                linksIn(results).isEmpty());
        assertEquals(0, results.linkCount);
    }

    /**
     * <b>Then</b> returned links carry the Content Drive map shape: permission <i>names</i> rather
     * than raw ids (matching drive folders), no {@code inode}, and the link decorations clients
     * need to render and follow them.
     */
    @Test
    public void testLinkMapUsesContentDriveShape() throws Exception {
        final PaginatedContents results = search(baseRequest()
                .showLinks(true).showFolders(false).baseTypes(List.of()).build());

        final List<Map<String, Object>> linkMaps = linksIn(results);
        assertFalse("Fixture error: expected links in the response", linkMaps.isEmpty());

        for (final Map<String, Object> linkMap : linkMaps) {
            assertNotNull("identifier is the stable key for drive views", linkMap.get("identifier"));
            assertFalse("Content Drive views strip inode", linkMap.containsKey("inode"));
            assertEquals("links", linkMap.get("type"));
            assertEquals("link", linkMap.get("extension"));
            assertEquals("linkIcon", linkMap.get("__icon__"));
            assertNotNull("title drives display and sorting", linkMap.get("title"));
            assertEquals("name should mirror title, as drive folders expose both",
                    linkMap.get("title"), linkMap.get("name"));
            assertNotNull("url is why a client selects a link", linkMap.get("url"));

            final Object permissions = linkMap.get("permissions");
            assertTrue("permissions must be a list", permissions instanceof List);
            for (final Object permission : (List<?>) permissions) {
                assertTrue("Drive views expose permission names, not raw ids: " + permission,
                        permission instanceof String);
            }
        }
    }

    /**
     * <b>When</b> a {@code sortBy} is supplied.
     * <b>Then</b> links honour it consistently with the rest of the result set, in both
     * directions.
     */
    @Test
    public void testSortByOrdersLinksConsistently() throws Exception {
        final List<String> ascending = linkTitlesSortedBy("title:asc");
        assertEquals("Links must follow sortBy ascending", LINK_TITLES, ascending);

        final List<String> descending = linkTitlesSortedBy("title:desc");
        final List<String> expectedDescending = new ArrayList<>(LINK_TITLES);
        java.util.Collections.reverse(expectedDescending);
        assertEquals("Links must follow sortBy descending", expectedDescending, descending);
    }

    private List<String> linkTitlesSortedBy(final String sortBy) throws Exception {
        final PaginatedContents results = search(baseRequest()
                .showLinks(true)
                .showFolders(false)
                .baseTypes(List.of())
                .sortBy(sortBy)
                .build());
        return linksIn(results).stream().map(item -> (String) item.get("title"))
                .collect(Collectors.toList());
    }
}
