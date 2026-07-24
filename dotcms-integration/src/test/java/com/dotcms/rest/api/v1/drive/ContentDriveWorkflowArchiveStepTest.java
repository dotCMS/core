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
import com.dotcms.datagen.WorkflowActionClassDataGen;
import com.dotcms.datagen.WorkflowActionDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.actionlet.ArchiveContentActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for surfacing archived content at archive-target workflow steps (issue #36028).
 *
 * <p>Mirrors {@link ContentDriveWorkflowFilterTest} but seeds a scheme whose <b>archive step</b> is
 * reached by an action carrying {@link ArchiveContentActionlet}. Exercises the per-branch
 * {@code deleted} handling implemented in
 * {@link com.dotcms.browser.BrowserAPIImpl}{@code #appendWorkflowQuery}:</p>
 *
 * <ul>
 *   <li>Filtering by the <b>archive-target step</b> returns content archived-at-that-step.</li>
 *   <li>Scheme-only and normal-step entries stay <b>live-only</b> — a contentlet system-archived
 *   in place at a normal step is NOT returned under that step.</li>
 *   <li>A <b>mixed</b> filter admits archived rows only in the archive-step branch.</li>
 *   <li><b>Hybrid</b> (archive step + free text) still surfaces the archived match.</li>
 *   <li><b>No-filter</b> requests keep the global archived exclusion (regression guard).</li>
 * </ul>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveWorkflowArchiveStepTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static WorkflowAPI workflowAPI;
    private static User systemUser;

    private static Host testSite;
    private static Folder testFolder;
    private static String testAssetPath;
    private static String uniqueId;

    // Scheme with a normal step and an archive-target step (reached by an ArchiveContentActionlet
    // action).
    private static WorkflowScheme scheme;
    private static WorkflowStep normalStep;
    private static WorkflowStep archiveStep;

    private static ContentType type;

    // Content archived (cvi.deleted=true) with its current task parked at the archive step.
    private static Contentlet archivedAtArchiveStep;
    // Live content (cvi.deleted=false) with its current task at the normal step.
    private static Contentlet liveAtNormalStep;
    // Content system-archived in place (cvi.deleted=true) whose task stayed at the normal step.
    private static Contentlet archivedInPlaceAtNormalStep;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        workflowAPI = APILocator.getWorkflowAPI();
        systemUser = APILocator.getUserAPI().getSystemUser();

        uniqueId = System.currentTimeMillis() + "";

        testSite = new SiteDataGen().name("drive-wfarch-" + uniqueId + ".local").nextPersisted();
        testFolder = new FolderDataGen().name("driveWfArchFolder_" + uniqueId).site(testSite)
                .nextPersisted();
        testAssetPath = "//" + testSite.getHostname() + testFolder.getPath();

        // Scheme with two steps; an action available on the normal step archives content and moves
        // it to the archive step (nextStep = archiveStep), making that step archive-target.
        scheme = new WorkflowDataGen().name("DriveWfArchScheme_" + uniqueId).nextPersisted();
        normalStep = new WorkflowStepDataGen(scheme.getId()).name("Normal_" + uniqueId)
                .order(0).nextPersisted();
        archiveStep = new WorkflowStepDataGen(scheme.getId()).name("Archive_" + uniqueId)
                .order(1).nextPersisted();

        final WorkflowAction archiveAction = new WorkflowActionDataGen(scheme.getId(),
                normalStep.getId())
                .name("Archive_" + uniqueId)
                .nextStep(archiveStep.getId())
                .nextPersisted();
        new WorkflowActionClassDataGen(archiveAction.getId())
                .actionClass(ArchiveContentActionlet.class)
                .nextPersisted();

        type = new ContentTypeDataGen()
                .name("DriveWfArchType_" + uniqueId)
                .velocityVarName("driveWfArchType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .workflowId(scheme.getId())
                .nextPersisted();

        liveAtNormalStep = newContent("Live at normal step " + uniqueId);
        moveToStep(liveAtNormalStep, normalStep);

        archivedAtArchiveStep = newContent("Archived at archive step " + uniqueId);
        archive(archivedAtArchiveStep);
        moveToStep(archivedAtArchiveStep, archiveStep);

        archivedInPlaceAtNormalStep = newContent("Archived in place at normal step " + uniqueId);
        archive(archivedInPlaceAtNormalStep);
        moveToStep(archivedInPlaceAtNormalStep, normalStep);

        Logger.info(ContentDriveWorkflowArchiveStepTest.class,
                "Workflow archive-step test data ready under " + testAssetPath);
    }

    private static Contentlet newContent(final String title) {
        return new ContentletDataGen(type.id())
                .setProperty("title", title)
                .folder(testFolder)
                .nextPersisted();
    }

    /** Archives the contentlet (sets {@code cvi.deleted=true}), forcing a synchronous reindex. */
    private static void archive(final Contentlet contentlet)
            throws DotDataException, DotSecurityException {
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        APILocator.getContentletAPI().archive(contentlet, systemUser, false);
    }

    /** Pins the contentlet's current workflow task to the given step. */
    private static void moveToStep(final Contentlet contentlet, final WorkflowStep step)
            throws DotDataException {
        // Reset any listener-created task first to avoid a duplicate (webasset, language) row.
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
        final WorkflowTask task = workflowAPI.createWorkflowTask(
                contentlet, systemUser, step, "drive-wfarch-test", "drive-wfarch-test");
        workflowAPI.saveWorkflowTask(task);
    }

    private Set<String> driveInodes(final DriveRequestForm request)
            throws DotDataException, DotSecurityException {
        final PaginatedContents results = contentDriveHelper.driveSearch(request, systemUser);
        return results.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());
    }

    private DriveRequestForm.Builder baseRequest() {
        return DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .live(false)
                .archived(false)
                .offset(0)
                .maxResults(100);
    }

    /**
     * Filtering by the archive-target step returns content archived-at-that-step — the repro that
     * previously returned 0.
     */
    @Test
    public void testArchiveStepReturnsArchivedContent()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(WorkflowFilterForm.builder()
                        .scheme(scheme.getId()).step(archiveStep.getId()).build()))
                .build());

        assertTrue("Archived content parked at the archive step must be returned",
                inodes.contains(archivedAtArchiveStep.getInode()));
    }

    /**
     * A normal-step entry stays live-only: a contentlet archived in place at that step must NOT be
     * returned.
     */
    @Test
    public void testNormalStepStaysLiveOnly()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(WorkflowFilterForm.builder()
                        .scheme(scheme.getId()).step(normalStep.getId()).build()))
                .build());

        assertTrue("Live content at the normal step must be returned",
                inodes.contains(liveAtNormalStep.getInode()));
        assertFalse("Content archived-in-place at a normal step must NOT be returned",
                inodes.contains(archivedInPlaceAtNormalStep.getInode()));
        assertFalse("Archived-at-archive-step content must not leak into the normal-step branch",
                inodes.contains(archivedAtArchiveStep.getInode()));
    }

    /**
     * A scheme-only entry stays live-only (spec §3.3): browsing by scheme must never dump archived
     * content, even though the scheme owns an archive step.
     */
    @Test
    public void testSchemeOnlyStaysLiveOnly()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(WorkflowFilterForm.builder().scheme(scheme.getId()).build()))
                .build());

        assertTrue("Live content must be returned by the scheme-only filter",
                inodes.contains(liveAtNormalStep.getInode()));
        assertFalse("Archived-at-archive-step content must not appear under a scheme-only filter",
                inodes.contains(archivedAtArchiveStep.getInode()));
        assertFalse("Archived-in-place content must not appear under a scheme-only filter",
                inodes.contains(archivedInPlaceAtNormalStep.getInode()));
    }

    /**
     * Mixed filter (normal step + archive step): archived rows are admitted only in the archive-step
     * branch; the normal-step branch stays live-only.
     */
    @Test
    public void testMixedFilterScopesArchivedToArchiveBranch()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(
                        WorkflowFilterForm.builder()
                                .scheme(scheme.getId()).step(normalStep.getId()).build(),
                        WorkflowFilterForm.builder()
                                .scheme(scheme.getId()).step(archiveStep.getId()).build()))
                .build());

        assertTrue("Live content at the normal step must be returned",
                inodes.contains(liveAtNormalStep.getInode()));
        assertTrue("Archived content at the archive step must be returned",
                inodes.contains(archivedAtArchiveStep.getInode()));
        assertFalse("Content archived-in-place at the normal step must NOT be returned",
                inodes.contains(archivedInPlaceAtNormalStep.getInode()));
    }

    /**
     * Hybrid: an archive-step filter combined with free text still surfaces the archived match (the
     * DB candidate set includes the archived inode and the ES text narrowing does not re-exclude it).
     */
    @Test
    public void testHybridArchiveStepWithFreeText()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(WorkflowFilterForm.builder()
                        .scheme(scheme.getId()).step(archiveStep.getId()).build()))
                .filters(QueryFilters.builder().text("Archived at archive step " + uniqueId)
                        .filterFolders(false).build())
                .build());

        assertTrue("Archived content at the archive step must match the hybrid (workflow + text) "
                        + "filter",
                inodes.contains(archivedAtArchiveStep.getInode()));
    }

    /**
     * With no workflow filter the global archived exclusion still applies (byte-identical path):
     * archived content is not returned, live content is.
     */
    @Test
    public void testNoWorkflowFilterKeepsArchivedExclusion()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest().build());

        assertTrue("Live content must be returned when no workflow filter is present",
                inodes.contains(liveAtNormalStep.getInode()));
        assertFalse("Archived content must stay excluded when no workflow filter is present",
                inodes.contains(archivedAtArchiveStep.getInode()));
        assertFalse("Archived-in-place content must stay excluded when no workflow filter is present",
                inodes.contains(archivedInPlaceAtNormalStep.getInode()));
    }

    /**
     * With {@code showArchived=true} the archive-step logic must NOT run (spec §3.5): everything
     * archived already shows, so the per-branch {@code cvi.deleted='false'} must not be forced on
     * the live branch. Guards the bug where a filter that includes an archive-target step would
     * hide archived content the caller explicitly asked for. Here a mixed filter (normal step +
     * archive step) with {@code archived=true} must return archived content in <b>both</b> branches.
     */
    @Test
    public void testShowArchivedReturnsArchivedInAllBranches()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .archived(true)
                .workflow(List.of(
                        WorkflowFilterForm.builder()
                                .scheme(scheme.getId()).step(normalStep.getId()).build(),
                        WorkflowFilterForm.builder()
                                .scheme(scheme.getId()).step(archiveStep.getId()).build()))
                .build());

        assertTrue("Live content at the normal step must be returned",
                inodes.contains(liveAtNormalStep.getInode()));
        assertTrue("Archived content at the archive step must be returned",
                inodes.contains(archivedAtArchiveStep.getInode()));
        assertTrue("Archived-in-place content at the normal step must NOT be hidden when the "
                        + "caller asked for archived content (showArchived=true)",
                inodes.contains(archivedInPlaceAtNormalStep.getInode()));
    }

    /**
     * When a pinned step cannot be resolved/classified (WorkflowAPI can't help), detection degrades
     * to non-archive: the browse must complete without throwing and stay live-only. Pins a bogus
     * step id alongside the real normal step; the bogus id is skipped, the normal step stays
     * live-only, and archived-in-place content is not surfaced.
     */
    @Test
    public void testUnresolvableStepFallsBackToLiveOnly()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(
                        WorkflowFilterForm.builder()
                                .scheme(scheme.getId()).step(normalStep.getId()).build(),
                        WorkflowFilterForm.builder()
                                .scheme(scheme.getId()).step("bogus-step-" + uniqueId).build()))
                .build());

        assertTrue("Live content at the normal step must still be returned",
                inodes.contains(liveAtNormalStep.getInode()));
        assertFalse("An unresolvable step must not flip the browse into surfacing archived content",
                inodes.contains(archivedInPlaceAtNormalStep.getInode()));
        assertFalse("An unresolvable step must not surface archived-at-archive-step content",
                inodes.contains(archivedAtArchiveStep.getInode()));
    }
}
