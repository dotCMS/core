package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
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
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the Content Drive <b>Status</b> filter (issue #37066): Archived,
 * Unpublished and Locked on {@code POST /api/v1/drive/search}.
 *
 * <p>Mirrors {@link ContentDriveWorkflowFilterTest}'s isolation approach — a dedicated site and
 * folder, a purpose-built content type, and a unique id per run — so nothing here asserts against
 * shared default content types or content another test may have left behind.</p>
 *
 * <p><b>The semantics under test are OR, not AND.</b> Selecting several statuses <i>widens</i> the
 * result set, matching the content-type and locale filters beside it in the toolbar. Excluding
 * archived content is a separate, pre-existing baseline rather than a fourth status: it applies on
 * every request and only {@code ARCHIVED} lifts it.</p>
 *
 * <p>This class covers User Story 1 (Archived). Later stories extend it.</p>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveStatusFilterTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static User systemUser;

    private static Host testSite;
    private static Folder testFolder;
    private static String testAssetPath;

    private static ContentType type;

    /** Published and live — must never appear under any status filter. */
    private static Contentlet liveItem;
    /** Never published — no live version. */
    private static Contentlet unpublishedItem;
    /** Archived. Archiving removes the live version, so this is also unpublished. */
    private static Contentlet archivedItem;
    /** Locked by the system user, and left live. */
    private static Contentlet lockedItem;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();

        final String uniqueId = System.currentTimeMillis() + "";

        testSite = new SiteDataGen().name("drive-status-" + uniqueId + ".local").nextPersisted();
        testFolder = new FolderDataGen().name("driveStatusFolder_" + uniqueId)
                .site(testSite).nextPersisted();
        testAssetPath = "//" + testSite.getHostname() + testFolder.getPath();

        type = new ContentTypeDataGen()
                .baseContentType(BaseContentType.CONTENT)
                .name("DriveStatusType_" + uniqueId)
                .velocityVarName("driveStatusType" + uniqueId)
                .host(testSite)
                .nextPersisted();

        // publish() mints a new version, so capture what it returns — archiving or locking the
        // pre-publish reference would act on a stale inode.
        liveItem = ContentletDataGen.publish(newContentlet("live"));

        unpublishedItem = newContentlet("unpublished");

        // Archived is seeded from a PUBLISHED item on purpose: archiving removes the live version,
        // so this contentlet ends up both archived AND unpublished. Without that overlap the
        // "UNPUBLISHED alone excludes archived content" assertion would pass vacuously.
        archivedItem = ContentletDataGen.publish(newContentlet("archived"));
        ContentletDataGen.archive(archivedItem);

        lockedItem = ContentletDataGen.publish(newContentlet("locked"));
        APILocator.getContentletAPI().lock(lockedItem, systemUser, false);
    }

    private static Contentlet newContentlet(final String title) {
        return new ContentletDataGen(type.id())
                .host(testSite)
                .folder(testFolder)
                .setProperty("title", title)
                .nextPersisted();
    }

    /**
     * The inodes the drive returned.
     *
     * <p>Asserting on <b>inodes</b>, not identifiers, is deliberate. {@code selectQuery} selects
     * {@code cvi.<working_inode|live_inode> as inode}, so which inode comes back is what proves the
     * query joined the right <i>version</i>. An identifier is stable across versions, so an
     * identifier-based assertion would still pass if the query joined {@code live_inode} where it
     * should have joined {@code working_inode} — which is exactly the silent failure the
     * {@code showWorking} rule guards against. Inodes carry the state; identifiers do not.</p>
     */
    private Set<String> driveInodes(final DriveRequestForm request)
            throws DotDataException, DotSecurityException {
        final PaginatedContents results = contentDriveHelper.driveSearch(request, systemUser);
        return results.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());
    }

    /**
     * Current working inode for a contentlet, re-read from version info.
     *
     * <p>Must be read at assertion time rather than captured at fixture time: publishing, archiving
     * and locking each mint a new version, so an inode held from creation is stale.</p>
     */
    private static String workingInode(final Contentlet contentlet) throws DotDataException {
        return APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId())
                .orElseThrow(() -> new AssertionError(
                        "No version info for " + contentlet.getIdentifier()))
                .getWorkingInode();
    }

    /** Current live inode, or null when there is no live version. Used to prove version choice. */
    private static String liveInode(final Contentlet contentlet) throws DotDataException {
        return APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId())
                .map(ContentletVersionInfo::getLiveInode)
                .orElse(null);
    }

    private DriveRequestForm.Builder baseRequest() {
        return DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .live(false)
                .offset(0)
                .maxResults(100);
    }

    // ---------------------------------------------------------------- FR-002: the default path

    /**
     * With no status sent, the drive behaves exactly as it does today: archived content hidden,
     * everything else returned (FR-002).
     *
     * <p>This is the case that matters most, because it is the path <b>every</b> drive search that
     * exists today takes. The status filter must be skipped entirely rather than translated into a
     * vacuous condition — an empty OR group would be {@code and ( )}, a SQL syntax error.</p>
     */
    @Test
    public void testNoStatusReturnsEverythingButArchived()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest().build());

        assertTrue("Live content must be returned", inodes.contains(workingInode(liveItem)));
        assertTrue("Unpublished content must be returned",
                inodes.contains(workingInode(unpublishedItem)));
        assertTrue("Locked content must be returned", inodes.contains(workingInode(lockedItem)));
        assertFalse("Archived content must stay hidden by default",
                inodes.contains(workingInode(archivedItem)));
    }

    /**
     * An explicitly empty status list behaves identically to omitting it — the same set, not merely
     * a non-empty one (FR-002).
     */
    @Test
    public void testEmptyStatusIsIdenticalToNoStatus()
            throws DotDataException, DotSecurityException {
        assertEquals("An empty status list must be a no-op",
                driveInodes(baseRequest().build()),
                driveInodes(baseRequest().status(List.of()).build()));
    }

    // ---------------------------------------------------------------- FR-003: ARCHIVED

    /**
     * {@code ARCHIVED} returns <b>only</b> archived content — not archived plus everything else,
     * which is what the pre-existing inclusive {@code showArchived} flag does (FR-003).
     */
    @Test
    public void testArchivedReturnsOnlyArchivedContent()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("ARCHIVED")).build());

        assertTrue("The archived item must be returned",
                inodes.contains(workingInode(archivedItem)));
        assertFalse("Live content must not leak into an ARCHIVED filter",
                inodes.contains(workingInode(liveItem)));
        assertFalse("Unpublished content must not leak into an ARCHIVED filter",
                inodes.contains(workingInode(unpublishedItem)));
        assertFalse("Locked content must not leak into an ARCHIVED filter",
                inodes.contains(workingInode(lockedItem)));
    }

    /**
     * Clearing the filter restores the default — archived content hidden again (FR-002, US1 AC2).
     */
    @Test
    public void testClearingArchivedRestoresDefault()
            throws DotDataException, DotSecurityException {
        driveInodes(baseRequest().status(List.of("ARCHIVED")).build());

        final Set<String> inodes = driveInodes(baseRequest().build());
        assertFalse("Archived content must be hidden again once the filter is cleared",
                inodes.contains(workingInode(archivedItem)));
    }

    /**
     * A status combines with a keyword search rather than replacing it: the result is archived
     * items matching the text (FR-009, US1 AC3).
     *
     * <p>Under the default {@code HYBRID_SINGLE_CHUNKED_QUERY_ES} heuristic the SQL supplies the
     * candidate set and the index only narrows by text, so the status clause applies with and
     * without a keyword.</p>
     */
    @Test
    public void testArchivedCombinesWithTextSearch()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .status(List.of("ARCHIVED"))
                .filters(QueryFilters.builder().text("archived").build())
                .build());

        assertTrue("The archived item matching the keyword must be returned",
                inodes.contains(workingInode(archivedItem)));
        assertFalse("A non-archived item must not be returned even if it matches the keyword",
                inodes.contains(workingInode(liveItem)));
    }

    /**
     * The drive returns the <b>working</b> inode for archived content, and that content has no live
     * inode at all.
     *
     * <p>This is the assertion that only an inode-level check can make, and it is the direct guard
     * for the {@code showWorking} rule: {@code selectQuery} picks its joined column as
     * {@code showWorking || showArchived ? "working_inode" : "live_inode"}. If that derivation ever
     * stops covering {@code ARCHIVED}, the query joins {@code live_inode} — which is null here — and
     * returns nothing, silently. Asserting on identifiers could never see the difference, because an
     * identifier is the same whichever version matched.</p>
     */
    @Test
    public void testArchivedReturnsTheWorkingInodeNotTheLiveOne()
            throws DotDataException, DotSecurityException {
        assertNull("Archiving must clear the live version — otherwise this test proves nothing",
                liveInode(archivedItem));

        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("ARCHIVED")).build());

        assertTrue("The archived item's WORKING inode must be the one returned",
                inodes.contains(workingInode(archivedItem)));
    }

    /**
     * Same guard for {@code UNPUBLISHED}: never-published content has no live inode, so the working
     * inode is the only one that can come back.
     */
    @Test
    public void testUnpublishedReturnsTheWorkingInode()
            throws DotDataException, DotSecurityException {
        assertNull("A never-published item must have no live version",
                liveInode(unpublishedItem));

        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("UNPUBLISHED")).build());

        assertTrue(inodes.contains(workingInode(unpublishedItem)));
    }

    // ---------------------------------------------------------------- FR-004: UNPUBLISHED

    /**
     * {@code UNPUBLISHED} returns content with no live version — and <b>excludes archived
     * content</b>, even though every archived item is also unpublished (FR-004, FR-007).
     *
     * <p>The archived fixture was published before being archived, so it genuinely has no live
     * version. Without that overlap this assertion would pass for the wrong reason.</p>
     */
    @Test
    public void testUnpublishedExcludesArchivedContent()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("UNPUBLISHED")).build());

        assertTrue("The never-published item must be returned",
                inodes.contains(workingInode(unpublishedItem)));
        assertFalse("Live content has a live version, so it must not be returned",
                inodes.contains(workingInode(liveItem)));
        assertFalse("Archived content must stay hidden — only ARCHIVED admits it",
                inodes.contains(workingInode(archivedItem)));
    }

    /**
     * Adding {@code ARCHIVED} to {@code UNPUBLISHED} <b>widens</b> the result to everything with no
     * live version, archived included (FR-006, FR-007).
     */
    @Test
    public void testArchivedPlusUnpublishedAdmitsArchivedContent()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("ARCHIVED", "UNPUBLISHED")).build());

        assertTrue("The archived item must now be admitted",
                inodes.contains(workingInode(archivedItem)));
        assertTrue("The unpublished item must still be returned",
                inodes.contains(workingInode(unpublishedItem)));
        assertFalse("Live content still has a live version",
                inodes.contains(workingInode(liveItem)));
    }

    // ---------------------------------------------------------------- FR-005: LOCKED

    /**
     * {@code LOCKED} returns content with a lock held, whoever holds it, and excludes archived
     * content (FR-005).
     *
     * <p>The locked fixture is live, which also proves {@code LOCKED} does not imply the
     * working-version scoping that {@code ARCHIVED} and {@code UNPUBLISHED} force.</p>
     */
    @Test
    public void testLockedReturnsOnlyLockedContent()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("LOCKED")).build());

        assertTrue("The locked item must be returned", inodes.contains(workingInode(lockedItem)));
        assertFalse("Unlocked live content must not be returned",
                inodes.contains(workingInode(liveItem)));
        assertFalse("Unlocked unpublished content must not be returned",
                inodes.contains(workingInode(unpublishedItem)));
        assertFalse("Archived content must stay hidden", inodes.contains(workingInode(archivedItem)));
    }

    // ---------------------------------------------------------------- FR-006: the union

    /**
     * Two statuses return the <b>union</b>, not the intersection — the single most important
     * assertion in this class, because it is what distinguishes OR from the AND the ticket
     * originally specified (FR-006).
     */
    @Test
    public void testUnpublishedOrLockedReturnsTheUnion()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("UNPUBLISHED", "LOCKED")).build());

        assertTrue("The unpublished item must be returned",
                inodes.contains(workingInode(unpublishedItem)));
        assertTrue("The locked item must be returned — this would fail under AND",
                inodes.contains(workingInode(lockedItem)));
        assertFalse("Content that is neither must not be returned",
                inodes.contains(workingInode(liveItem)));
    }

    /**
     * Adding a status never shrinks the result set (SC-007). Asserted as a property over every
     * pair rather than as fixed counts, so it keeps holding as the fixture grows.
     */
    @Test
    public void testAddingAStatusNeverShrinksTheResultSet()
            throws DotDataException, DotSecurityException {
        final List<String> all = List.of("ARCHIVED", "UNPUBLISHED", "LOCKED");

        for (final String first : all) {
            for (final String second : all) {
                if (first.equals(second)) {
                    continue;
                }
                final Set<String> single = driveInodes(
                        baseRequest().status(List.of(first)).build());
                final Set<String> pair = driveInodes(
                        baseRequest().status(List.of(first, second)).build());

                assertTrue(String.format(
                                "[%s] returned %d but [%s, %s] returned %d — adding a status must never "
                                        + "shrink the result set", first, single.size(), first, second,
                                pair.size()),
                        pair.size() >= single.size());
                assertTrue(String.format("[%s, %s] must contain everything [%s] returned",
                                first, second, first),
                        pair.containsAll(single));
            }
        }
    }

    /**
     * All three statuses return everything except content that is cleanly live and unlocked (US4).
     */
    @Test
    public void testAllThreeStatusesReturnEverythingNotCleanlyPublished()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(
                baseRequest().status(List.of("ARCHIVED", "UNPUBLISHED", "LOCKED")).build());

        assertTrue(inodes.contains(workingInode(archivedItem)));
        assertTrue(inodes.contains(workingInode(unpublishedItem)));
        assertTrue(inodes.contains(workingInode(lockedItem)));
        assertFalse("The clean live item is the only one that must not appear",
                inodes.contains(workingInode(liveItem)));
    }

    // ---------------------------------------------------------------- FR-015: folders

    /**
     * A status selection must NOT override an explicit {@code showFolders} (FR-015).
     *
     * <p>Folders carry no status, so the Content Drive UI stops requesting them once a status is
     * selected — but that is the client's decision. The endpoint honours what it is asked for.
     * Silently forcing {@code showFolders} to false here would make the response stop matching the
     * request, and would leave {@code folderCursor}/{@code hasMoreFolders} describing a folder query
     * the caller never received.</p>
     */
    @Test
    public void testStatusDoesNotOverrideExplicitShowFolders()
            throws DotDataException, DotSecurityException {
        new FolderDataGen().name("driveStatusChild_" + System.nanoTime())
                .parent(testFolder).nextPersisted();

        final PaginatedContents unfiltered = contentDriveHelper.driveSearch(
                baseRequest().showFolders(true).build(), systemUser);
        assertTrue("Unfiltered request should list folders", unfiltered.folderCount > 0);

        final PaginatedContents withStatus = contentDriveHelper.driveSearch(
                baseRequest().showFolders(true).status(List.of("ARCHIVED")).build(), systemUser);
        assertTrue("An explicit showFolders:true must still be honoured alongside a status",
                withStatus.folderCount > 0);

        final PaginatedContents withoutFolders = contentDriveHelper.driveSearch(
                baseRequest().showFolders(false).status(List.of("ARCHIVED")).build(), systemUser);
        assertEquals("showFolders:false must still suppress folders",
                0, withoutFolders.folderCount);
    }

    // ---------------------------------------------------------------- FR-010: rejection

    /**
     * An unrecognized status is rejected rather than ignored (FR-010). Ignoring it would return a
     * <b>wider</b> set than the caller asked for.
     */
    @Test(expected = com.dotcms.rest.exception.BadRequestException.class)
    public void testUnknownStatusIsRejected()
            throws DotDataException, DotSecurityException {
        contentDriveHelper.driveSearch(
                baseRequest().status(List.of("DRAFT")).build(), systemUser);
    }
}
