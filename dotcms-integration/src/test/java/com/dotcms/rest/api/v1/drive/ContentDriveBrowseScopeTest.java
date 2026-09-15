package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.BadRequestException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the Content Drive <b>browse scopes</b> (issue #37426) on
 * {@code POST /api/v1/drive/search}.
 *
 * <p>Three scopes replace the one the drive could express: {@code ALL} is the whole site at any
 * depth, {@code ROOT} is what sits at the site root, and {@code SYSTEM_HOST} is the shared content
 * that belongs to no site. A request that names no scope must keep meaning what it means today,
 * because the Asset Picker and six other callers reach this same listing and none of them will
 * ever send one.</p>
 *
 * <p>Isolation follows {@link ContentDriveStatusFilterTest}: a dedicated site, a purpose-built
 * content type, and a unique id per run, so nothing here depends on shared demo data or on what
 * another test left behind. The site is removed afterwards.</p>
 *
 * <p><b>Why a System Host fixture needs care.</b> Content published to System Host outlives this
 * test's site and is visible to every other test in the suite. The assertions below therefore
 * check that System Host content this test created is present or absent, never that System Host
 * contains <i>only</i> that content — another suite's fixture may legitimately be sitting there.</p>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveBrowseScopeTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static User systemUser;

    private static Host testSite;
    private static Host systemHost;
    private static Folder childFolder;

    private static String siteRootPath;
    private static String childFolderPath;

    private static ContentType type;

    /** Sits at the site root, with no folder above it. */
    private static Contentlet rootItem;
    /** Sits inside {@link #childFolder}, one level below the root. */
    private static Contentlet nestedItem;
    /** Belongs to System Host, so to no site at all. */
    private static Contentlet systemHostItem;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        systemHost = APILocator.getHostAPI().findSystemHost();

        final String uniqueId = System.currentTimeMillis() + "";

        testSite = new SiteDataGen().name("drive-scope-" + uniqueId + ".local").nextPersisted();
        childFolder = new FolderDataGen().name("driveScopeFolder_" + uniqueId)
                .site(testSite).nextPersisted();

        siteRootPath = "//" + testSite.getHostname() + "/";
        childFolderPath = "//" + testSite.getHostname() + childFolder.getPath();

        // Built on System Host rather than on the test site, so content of this type is allowed to
        // live on either. A type scoped to the test site could not hold the System Host fixture.
        type = new ContentTypeDataGen()
                .baseContentType(BaseContentType.CONTENT)
                .name("DriveScopeType_" + uniqueId)
                .velocityVarName("driveScopeType" + uniqueId)
                .host(systemHost)
                .nextPersisted();

        rootItem = new ContentletDataGen(type.id())
                .host(testSite)
                .setProperty("title", "scope-root-" + uniqueId)
                .nextPersisted();

        nestedItem = new ContentletDataGen(type.id())
                .host(testSite)
                .folder(childFolder)
                .setProperty("title", "scope-nested-" + uniqueId)
                .nextPersisted();

        systemHostItem = new ContentletDataGen(type.id())
                .host(systemHost)
                .setProperty("title", "scope-shared-" + uniqueId)
                .nextPersisted();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        // The site takes its folder and both site-bound contentlets with it. The System Host item
        // has no site to be removed with, so it is deleted on its own -- left behind it would show
        // up in every later test that lists shared content.
        if (null != systemHostItem) {
            APILocator.getContentletAPI().destroy(systemHostItem, systemUser, false);
        }
        if (null != testSite) {
            APILocator.getHostAPI().archive(testSite, systemUser, false);
            APILocator.getHostAPI().delete(testSite, systemUser, false);
        }
    }

    /**
     * The inodes the drive returned.
     *
     * <p>Inodes rather than identifiers, for the reason spelled out in
     * {@link ContentDriveStatusFilterTest}: the query selects an inode, so the inode is what proves
     * it joined the right version. Read at assertion time, never captured at fixture time.</p>
     */
    private Set<String> driveInodes(final DriveRequestForm request)
            throws DotDataException, DotSecurityException {
        return inodesFrom(contentDriveHelper.driveSearch(request, systemUser));
    }

    private static Set<String> inodesFrom(final PaginatedContents results) {
        return results.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());
    }

    private static String workingInode(final Contentlet contentlet) throws DotDataException {
        return APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId())
                .orElseThrow(() -> new AssertionError(
                        "No version info for " + contentlet.getIdentifier()))
                .getWorkingInode();
    }

    private DriveRequestForm.Builder requestAt(final String assetPath) {
        return DriveRequestForm.builder()
                .assetPath(assetPath)
                .live(false)
                .offset(0)
                .maxResults(100);
    }

    // ------------------------------------------------------------- FR-007: the site root

    /**
     * Selecting the site row lists what sits at the root and nothing from inside a folder (FR-007).
     *
     * <p>This is the view that does not exist today: before the scopes, asking for the site and
     * asking for the whole site were the same request.</p>
     */
    @Test
    public void testSiteRootListsRootItemsAndNotFolderContents()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                requestAt(siteRootPath).browseScope(BrowseScope.ROOT).build());

        assertTrue("Content at the site root must be listed",
                inodes.contains(workingInode(rootItem)));
        assertFalse("Content inside a folder must not be listed at the site root",
                inodes.contains(workingInode(nestedItem)));
    }

    /**
     * The site root never admits System Host content, whatever the toggle says (FR-008).
     *
     * <p>Asserted with {@code includeSystemHost(true)} deliberately — the value the toggle sends
     * when it is on. The scope has to win over it, or "the site root" would quietly mean "the site
     * root plus everything shared".</p>
     */
    @Test
    public void testSiteRootExcludesSystemHostEvenWithTheToggleOn()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(requestAt(siteRootPath)
                .browseScope(BrowseScope.ROOT)
                .includeSystemHost(true)
                .build());

        assertFalse("System Host content must never appear in the site root scope",
                inodes.contains(workingInode(systemHostItem)));
    }

    // ------------------------------------------------------------- FR-006: all site content

    /**
     * All Site Content spans every depth of the site (FR-006), which is what the site row used to
     * do and what this scope now carries.
     */
    @Test
    public void testAllSiteContentListsEveryDepth()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                requestAt(siteRootPath).browseScope(BrowseScope.ALL).build());

        assertTrue("Content at the site root must be listed",
                inodes.contains(workingInode(rootItem)));
        assertTrue("Content inside a folder must also be listed",
                inodes.contains(workingInode(nestedItem)));
    }

    /**
     * The toggle decides whether All Site Content carries shared content alongside the site's
     * (FR-020), and it is the only scope where the question means anything.
     */
    @Test
    public void testAllSiteContentHonoursTheSystemHostToggle()
            throws DotDataException, DotSecurityException {
        final String sharedInode = workingInode(systemHostItem);

        assertTrue("With the toggle on, shared content joins the site's",
                driveInodes(requestAt(siteRootPath)
                        .browseScope(BrowseScope.ALL)
                        .includeSystemHost(true)
                        .build()).contains(sharedInode));

        assertFalse("With the toggle off, shared content is excluded",
                driveInodes(requestAt(siteRootPath)
                        .browseScope(BrowseScope.ALL)
                        .includeSystemHost(false)
                        .build()).contains(sharedInode));
    }

    // ------------------------------------------------------------- FR-010: System Host

    /**
     * System Host lists shared content and admits nothing belonging to a site (FR-010).
     *
     * <p>Both halves matter. Listing the shared item proves the scope reaches the clause that was
     * unreachable before this feature; excluding the site's items proves it did not simply widen.</p>
     */
    @Test
    public void testSystemHostListsSharedContentAndNoSiteContent()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                requestAt(siteRootPath).browseScope(BrowseScope.SYSTEM_HOST).build());

        assertTrue("Shared content must be listed",
                inodes.contains(workingInode(systemHostItem)));
        assertFalse("Content at the site root must not appear under System Host",
                inodes.contains(workingInode(rootItem)));
        assertFalse("Content inside a site folder must not appear under System Host",
                inodes.contains(workingInode(nestedItem)));
    }

    // ------------------------------------------------------------- folders, per scope

    /**
     * The site root reports the site's top-level folders alongside its root content (FR-007).
     *
     * <p>They sit at the root, so they are part of what is "at" the root. This is the half that
     * distinguishes the site-root scope from all-site-content by more than depth: the two differ in
     * their content <i>and</i> in their folders.</p>
     */
    @Test
    public void testSiteRootReportsTheSitesTopLevelFolders()
            throws DotDataException, DotSecurityException {
        final PaginatedContents results = contentDriveHelper.driveSearch(
                requestAt(siteRootPath).browseScope(BrowseScope.ROOT).build(), systemUser);

        assertTrue("The site's top-level folders must be listed at the site root",
                results.folderCount > 0);
        // `title` rather than a folder-shaped guess: DotFolderTransformerImpl sets both "name" and
        // "title" to the folder's name, and "title" is the key content rows carry too.
        assertTrue("The test's own folder must be among them",
                results.list.stream()
                        .anyMatch(item -> childFolder.getName().equals(item.get("title"))));
    }

    /**
     * System Host reports no folders because it has none (FR-010) — asserted with folders
     * <b>explicitly requested</b>, so this is about the place and not about the request.
     *
     * <p>Distinct from the all-site-content case below. There, a caller could ask for folders and
     * get them, and the drive simply does not ask. Here there is nothing to return however the
     * request is phrased, which is why System Host can never grow a folder column by accident.</p>
     */
    @Test
    public void testSystemHostHasNoFoldersEvenWhenAskedFor()
            throws DotDataException, DotSecurityException {
        final PaginatedContents results = contentDriveHelper.driveSearch(
                requestAt(siteRootPath)
                        .browseScope(BrowseScope.SYSTEM_HOST)
                        .showFolders(true)
                        .build(),
                systemUser);

        assertEquals("System Host holds no folders, so none can be listed",
                0, results.folderCount);
    }

    /**
     * All Site Content carries no folders (FR-006) — and the decision is the <b>caller's</b>.
     *
     * <p>Folder policy deliberately lives with the caller so the response always matches the
     * request, which is why this asserts the two halves separately: asking for no folders returns
     * none, and asking for them still returns them. A scope that silently suppressed folders would
     * make the response stop matching the request, and would take the Asset Picker with it.</p>
     */
    @Test
    public void testAllSiteContentReturnsNoFoldersWhenItDoesNotAskForThem()
            throws DotDataException, DotSecurityException {
        assertEquals("Asking for no folders must return none",
                0,
                contentDriveHelper.driveSearch(requestAt(siteRootPath)
                        .browseScope(BrowseScope.ALL)
                        .showFolders(false)
                        .build(), systemUser).folderCount);

        assertTrue("The scope must not decide this on the caller's behalf",
                contentDriveHelper.driveSearch(requestAt(siteRootPath)
                        .browseScope(BrowseScope.ALL)
                        .showFolders(true)
                        .build(), systemUser).folderCount > 0);
    }

    // ------------------------------------------------------------- FR-026: the scope-less request

    /**
     * A request naming no scope behaves as it does today (FR-026). <b>The most important test in
     * this file.</b>
     *
     * <p>Seven callers reach this listing without ever sending a scope. Asserted at a folder path
     * rather than at the root because that is the Asset Picker's shape, and because it is the one
     * that would break if {@code ALL} had been made the default: the folder constraint would be
     * discarded and the picker would start listing every descendant.</p>
     */
    @Test
    public void testNoScopeAtAFolderPathStillListsThatFolderOnly()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(requestAt(childFolderPath).build());

        assertTrue("The folder's own content must be listed",
                inodes.contains(workingInode(nestedItem)));
        assertFalse("Content outside the folder must not be listed",
                inodes.contains(workingInode(rootItem)));
    }

    /**
     * At the site root, no scope and {@code ALL} are the same request (FR-026) — the whole-site
     * view the drive produced before this feature, now reachable by name.
     */
    @Test
    public void testNoScopeAtTheSiteRootMatchesAllSiteContent()
            throws DotDataException, DotSecurityException {
        assertEquals("Omitting the scope at the site root must equal asking for ALL",
                driveInodes(requestAt(siteRootPath).build()),
                driveInodes(requestAt(siteRootPath).browseScope(BrowseScope.ALL).build()));
    }

    /**
     * A scope paired with a folder path is refused rather than resolved (FR-022's invariant).
     *
     * <p>Refusing is the point. Picking one of two contradictory statements would list content from
     * somewhere the caller did not ask for, and the caller would have no way to tell.</p>
     */
    @Test
    public void testAScopeWithAFolderPathIsRefused() {
        assertThrows("A scope is only meaningful at the site root",
                BadRequestException.class,
                () -> requestAt(childFolderPath).browseScope(BrowseScope.ROOT).build());
    }

    // ------------------------------------------------------------- FR-012 / SC-004: both paths

    /** The two internal search paths, only the first of which runs unless configured otherwise. */
    private static final String[] SEARCH_HEURISTICS =
            {"HYBRID_SINGLE_CHUNKED_QUERY_ES", "PURE_ES"};

    /**
     * Runs one request under both search paths and hands each result set to {@code assertions}.
     *
     * <p>The heuristic is memoised per {@link BrowserAPIImpl} instance, so setting the config is
     * not enough on its own — each run gets a fresh instance through the helper's injectable
     * constructor, whose lazy read then picks the new value up.</p>
     *
     * <p>A text filter is what routes a request through the index at all. Without one neither
     * heuristic is consulted and a test using this would quietly assert nothing, so it is applied
     * here rather than left to each caller to remember.</p>
     */
    private void underBothSearchPaths(final BrowseScope scope,
            final BiConsumer<String, Set<String>> assertions)
            throws DotDataException, DotSecurityException {
        final String original = Config.getStringProperty("BROWSE_API_HEURISTIC_TYPE",
                SEARCH_HEURISTICS[0]);
        try {
            for (final String heuristic : SEARCH_HEURISTICS) {
                Config.setProperty("BROWSE_API_HEURISTIC_TYPE", heuristic);

                assertions.accept(heuristic, inodesFrom(
                        new ContentDriveHelper(new BrowserAPIImpl()).driveSearch(
                                requestAt(siteRootPath)
                                        .browseScope(scope)
                                        .filters(QueryFilters.builder().text("scope").build())
                                        .build(),
                                systemUser)));
            }
        } finally {
            // Read with the default rather than null: Config hands the value straight to the
            // properties store, so restoring a null would be worse than the state it replaced.
            Config.setProperty("BROWSE_API_HEURISTIC_TYPE", original);
        }
    }

    /**
     * System Host admits nothing belonging to a site, whichever path serves the request (FR-012).
     *
     * <p><b>What is asserted, and what deliberately is not.</b> The index-only path gives up
     * read-your-writes by design, so content written moments ago may legitimately not be indexed
     * yet. Asserting the fixture is present would produce a flake that reads exactly like a scope
     * bug. What must hold under both paths is what is <i>excluded</i> — and an unindexed fixture
     * cannot make an exclusion true by accident.</p>
     */
    @Test
    public void testSystemHostScopeAdmitsNoSiteContentUnderEitherSearchPath()
            throws DotDataException, DotSecurityException {
        final String rootInode = workingInode(rootItem);
        final String nestedInode = workingInode(nestedItem);

        underBothSearchPaths(BrowseScope.SYSTEM_HOST, (heuristic, inodes) -> {
            assertFalse("Site root content leaked into System Host under " + heuristic,
                    inodes.contains(rootInode));
            assertFalse("Folder content leaked into System Host under " + heuristic,
                    inodes.contains(nestedInode));
        });
    }

    /**
     * The same guarantee for the site root: searching within it never starts returning content
     * from inside a folder, or shared content (FR-012, and the spec's search edge case).
     */
    @Test
    public void testSiteRootAdmitsNoFolderOrSharedContentUnderEitherSearchPath()
            throws DotDataException, DotSecurityException {
        final String nestedInode = workingInode(nestedItem);
        final String sharedInode = workingInode(systemHostItem);

        underBothSearchPaths(BrowseScope.ROOT, (heuristic, inodes) -> {
            assertFalse("Folder content leaked into the site root under " + heuristic,
                    inodes.contains(nestedInode));
            assertFalse("Shared content leaked into the site root under " + heuristic,
                    inodes.contains(sharedInode));
        });
    }
}
