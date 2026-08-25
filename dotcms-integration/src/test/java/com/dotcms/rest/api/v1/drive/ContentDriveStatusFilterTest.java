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

    /** Identifiers rather than inodes: publishing, archiving and locking each mint a new version. */
    private Set<String> driveIdentifiers(final DriveRequestForm request)
            throws DotDataException, DotSecurityException {
        final PaginatedContents results = contentDriveHelper.driveSearch(request, systemUser);
        return results.list.stream()
                .map(item -> (String) item.get("identifier"))
                .collect(Collectors.toSet());
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
        final Set<String> ids = driveIdentifiers(baseRequest().build());

        assertTrue("Live content must be returned", ids.contains(liveItem.getIdentifier()));
        assertTrue("Unpublished content must be returned",
                ids.contains(unpublishedItem.getIdentifier()));
        assertTrue("Locked content must be returned", ids.contains(lockedItem.getIdentifier()));
        assertFalse("Archived content must stay hidden by default",
                ids.contains(archivedItem.getIdentifier()));
    }

    /**
     * An explicitly empty status list behaves identically to omitting it — the same set, not merely
     * a non-empty one (FR-002).
     */
    @Test
    public void testEmptyStatusIsIdenticalToNoStatus()
            throws DotDataException, DotSecurityException {
        assertEquals("An empty status list must be a no-op",
                driveIdentifiers(baseRequest().build()),
                driveIdentifiers(baseRequest().status(List.of()).build()));
    }

    // ---------------------------------------------------------------- FR-003: ARCHIVED

    /**
     * {@code ARCHIVED} returns <b>only</b> archived content — not archived plus everything else,
     * which is what the pre-existing inclusive {@code showArchived} flag does (FR-003).
     */
    @Test
    public void testArchivedReturnsOnlyArchivedContent()
            throws DotDataException, DotSecurityException {
        final Set<String> ids = driveIdentifiers(
                baseRequest().status(List.of("ARCHIVED")).build());

        assertTrue("The archived item must be returned",
                ids.contains(archivedItem.getIdentifier()));
        assertFalse("Live content must not leak into an ARCHIVED filter",
                ids.contains(liveItem.getIdentifier()));
        assertFalse("Unpublished content must not leak into an ARCHIVED filter",
                ids.contains(unpublishedItem.getIdentifier()));
        assertFalse("Locked content must not leak into an ARCHIVED filter",
                ids.contains(lockedItem.getIdentifier()));
    }

    /**
     * Clearing the filter restores the default — archived content hidden again (FR-002, US1 AC2).
     */
    @Test
    public void testClearingArchivedRestoresDefault()
            throws DotDataException, DotSecurityException {
        driveIdentifiers(baseRequest().status(List.of("ARCHIVED")).build());

        final Set<String> ids = driveIdentifiers(baseRequest().build());
        assertFalse("Archived content must be hidden again once the filter is cleared",
                ids.contains(archivedItem.getIdentifier()));
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
        final Set<String> ids = driveIdentifiers(baseRequest()
                .status(List.of("ARCHIVED"))
                .filters(QueryFilters.builder().text("archived").build())
                .build());

        assertTrue("The archived item matching the keyword must be returned",
                ids.contains(archivedItem.getIdentifier()));
        assertFalse("A non-archived item must not be returned even if it matches the keyword",
                ids.contains(liveItem.getIdentifier()));
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
        final Set<String> ids = driveIdentifiers(
                baseRequest().status(List.of("UNPUBLISHED")).build());

        assertTrue("The never-published item must be returned",
                ids.contains(unpublishedItem.getIdentifier()));
        assertFalse("Live content has a live version, so it must not be returned",
                ids.contains(liveItem.getIdentifier()));
        assertFalse("Archived content must stay hidden — only ARCHIVED admits it",
                ids.contains(archivedItem.getIdentifier()));
    }

    /**
     * Adding {@code ARCHIVED} to {@code UNPUBLISHED} <b>widens</b> the result to everything with no
     * live version, archived included (FR-006, FR-007).
     */
    @Test
    public void testArchivedPlusUnpublishedAdmitsArchivedContent()
            throws DotDataException, DotSecurityException {
        final Set<String> ids = driveIdentifiers(
                baseRequest().status(List.of("ARCHIVED", "UNPUBLISHED")).build());

        assertTrue("The archived item must now be admitted",
                ids.contains(archivedItem.getIdentifier()));
        assertTrue("The unpublished item must still be returned",
                ids.contains(unpublishedItem.getIdentifier()));
        assertFalse("Live content still has a live version",
                ids.contains(liveItem.getIdentifier()));
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
        final Set<String> ids = driveIdentifiers(
                baseRequest().status(List.of("LOCKED")).build());

        assertTrue("The locked item must be returned", ids.contains(lockedItem.getIdentifier()));
        assertFalse("Unlocked live content must not be returned",
                ids.contains(liveItem.getIdentifier()));
        assertFalse("Unlocked unpublished content must not be returned",
                ids.contains(unpublishedItem.getIdentifier()));
        assertFalse("Archived content must stay hidden", ids.contains(archivedItem.getIdentifier()));
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
        final Set<String> ids = driveIdentifiers(
                baseRequest().status(List.of("UNPUBLISHED", "LOCKED")).build());

        assertTrue("The unpublished item must be returned",
                ids.contains(unpublishedItem.getIdentifier()));
        assertTrue("The locked item must be returned — this would fail under AND",
                ids.contains(lockedItem.getIdentifier()));
        assertFalse("Content that is neither must not be returned",
                ids.contains(liveItem.getIdentifier()));
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
                final Set<String> single = driveIdentifiers(
                        baseRequest().status(List.of(first)).build());
                final Set<String> pair = driveIdentifiers(
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
        final Set<String> ids = driveIdentifiers(
                baseRequest().status(List.of("ARCHIVED", "UNPUBLISHED", "LOCKED")).build());

        assertTrue(ids.contains(archivedItem.getIdentifier()));
        assertTrue(ids.contains(unpublishedItem.getIdentifier()));
        assertTrue(ids.contains(lockedItem.getIdentifier()));
        assertFalse("The clean live item is the only one that must not appear",
                ids.contains(liveItem.getIdentifier()));
    }

    // ---------------------------------------------------------------- FR-015: folders

    /**
     * Folders carry no status, so any status selection must drop them while an unfiltered request
     * keeps them (FR-015).
     */
    @Test
    public void testFoldersSuppressedWhenStatusFilterActive()
            throws DotDataException, DotSecurityException {
        new FolderDataGen().name("driveStatusChild_" + System.nanoTime())
                .parent(testFolder).nextPersisted();

        final PaginatedContents unfiltered = contentDriveHelper.driveSearch(
                baseRequest().showFolders(true).build(), systemUser);
        assertTrue("Unfiltered request should list folders", unfiltered.folderCount > 0);

        final PaginatedContents filtered = contentDriveHelper.driveSearch(
                baseRequest().showFolders(true).status(List.of("ARCHIVED")).build(), systemUser);
        assertEquals("A status filter must suppress folders", 0, filtered.folderCount);
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
