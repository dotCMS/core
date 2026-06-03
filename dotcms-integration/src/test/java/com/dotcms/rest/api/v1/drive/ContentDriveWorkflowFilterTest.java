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
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
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
 * Integration test for the Content Drive workflow filter (issue #35880 / #35470).
 *
 * <p>Exercises {@link ContentDriveHelper#driveSearch} with the {@code workflow} filter and asserts
 * the DB-first semantics implemented in
 * {@link com.dotcms.browser.BrowserAPIImpl}{@code #appendWorkflowQuery}:</p>
 *
 * <ul>
 *   <li><b>Scheme-only entry</b> — matches by content-type assignment
 *   ({@code workflow_scheme_x_structure}), so content that has <i>no</i> {@code workflow_task}
 *   (imported / push-published / never-actioned) still appears under its scheme.</li>
 *   <li><b>Step-pinned entry</b> — matches the contentlet's <i>current</i> task
 *   ({@code workflow_task.status}); task-less content is therefore excluded.</li>
 *   <li><b>OR semantics</b> — multiple entries union their matches.</li>
 *   <li><b>No filter</b> — result set is unchanged (regression guard).</li>
 *   <li><b>Folder suppression</b> — folders carry no workflow state, so they are dropped when a
 *   workflow filter is active.</li>
 * </ul>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveWorkflowFilterTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static WorkflowAPI workflowAPI;
    private static User systemUser;

    private static Host testSite;
    private static Folder testFolder;
    private static String testAssetPath;

    // Scheme A — filtered by scheme (matches all A-assigned content, task or not).
    private static WorkflowScheme schemeA;
    private static WorkflowStep schemeAStep1;
    private static WorkflowStep schemeAStep2;

    // Scheme C — filtered by step (matches only content whose current task is at the pinned step).
    private static WorkflowScheme schemeC;
    private static WorkflowStep schemeCStep1;

    // Content of type A.
    private static Contentlet aNeverActioned; // no workflow_task at all
    private static Contentlet aAtStep1;       // current task at scheme A step 1
    private static Contentlet aAtStep2;       // current task at scheme A step 2

    // Content of type C.
    private static Contentlet cAtStep1;       // current task at scheme C step 1
    private static Contentlet cNeverActioned; // no workflow_task at all

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        workflowAPI = APILocator.getWorkflowAPI();
        systemUser = APILocator.getUserAPI().getSystemUser();

        final String uniqueId = System.currentTimeMillis() + "";

        testSite = new SiteDataGen().name("drive-wf-" + uniqueId + ".local").nextPersisted();
        testFolder = new FolderDataGen().name("driveWfFolder_" + uniqueId).site(testSite).nextPersisted();
        testAssetPath = "//" + testSite.getHostname() + testFolder.getPath();

        // Two schemes, each with two default steps + actions.
        schemeA = new WorkflowDataGen().name("DriveWfSchemeA_" + uniqueId)
                .nextPersistedWithDefaultStepsAndActions();
        final List<WorkflowStep> aSteps = workflowAPI.findSteps(schemeA);
        schemeAStep1 = aSteps.get(0);
        schemeAStep2 = aSteps.get(1);

        schemeC = new WorkflowDataGen().name("DriveWfSchemeC_" + uniqueId)
                .nextPersistedWithDefaultStepsAndActions();
        schemeCStep1 = workflowAPI.findSteps(schemeC).get(0);

        // Content types governed by each scheme.
        final ContentType typeA = new ContentTypeDataGen()
                .name("DriveWfTypeA_" + uniqueId)
                .velocityVarName("driveWfTypeA_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .workflowId(schemeA.getId())
                .nextPersisted();

        final ContentType typeC = new ContentTypeDataGen()
                .name("DriveWfTypeC_" + uniqueId)
                .velocityVarName("driveWfTypeC_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .workflowId(schemeC.getId())
                .nextPersisted();

        // Type A content.
        aNeverActioned = newContent(typeA.id(), "A never actioned " + uniqueId);
        clearTask(aNeverActioned); // truly task-less — must still match a scheme filter

        aAtStep1 = newContent(typeA.id(), "A at step 1 " + uniqueId);
        moveToStep(aAtStep1, schemeAStep1);

        aAtStep2 = newContent(typeA.id(), "A at step 2 " + uniqueId);
        moveToStep(aAtStep2, schemeAStep2);

        // Type C content.
        cAtStep1 = newContent(typeC.id(), "C at step 1 " + uniqueId);
        moveToStep(cAtStep1, schemeCStep1);

        cNeverActioned = newContent(typeC.id(), "C never actioned " + uniqueId);
        clearTask(cNeverActioned);

        Logger.info(ContentDriveWorkflowFilterTest.class,
                "Workflow filter test data ready under " + testAssetPath);
    }

    private static Contentlet newContent(final String contentTypeId, final String title) {
        return new ContentletDataGen(contentTypeId)
                .setProperty("title", title)
                .folder(testFolder)
                .nextPersisted();
    }

    /** Removes any workflow task so the contentlet has no current step (import-like state). */
    private static void clearTask(final Contentlet contentlet) throws DotDataException {
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
    }

    /** Pins the contentlet's current workflow task to the given step. */
    private static void moveToStep(final Contentlet contentlet, final WorkflowStep step)
            throws DotDataException {
        // Reset the listener-created task first to avoid a duplicate (webasset, language) row.
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
        final WorkflowTask task = workflowAPI.createWorkflowTask(
                contentlet, systemUser, step, "drive-wf-test", "drive-wf-test");
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
     * Scheme-only entry must match every contentlet whose type is governed by that scheme —
     * including a contentlet with no {@code workflow_task} — and must exclude content of other
     * schemes.
     */
    @Test
    public void testSchemeFilterMatchesByTypeAssignmentIncludingTaskLess()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(WorkflowFilterForm.builder().scheme(schemeA.getId()).build()))
                .build());

        assertTrue("Task-less A content must still match the scheme filter",
                inodes.contains(aNeverActioned.getInode()));
        assertTrue("A content at step 1 must match the scheme filter",
                inodes.contains(aAtStep1.getInode()));
        assertTrue("A content at step 2 must match the scheme filter",
                inodes.contains(aAtStep2.getInode()));
        assertFalse("C content must not match the scheme A filter",
                inodes.contains(cAtStep1.getInode()));
        assertFalse("C content must not match the scheme A filter",
                inodes.contains(cNeverActioned.getInode()));
    }

    /**
     * Step-pinned entry must match only the contentlet whose current task is at that step, and must
     * exclude content at other steps as well as task-less content.
     */
    @Test
    public void testStepFilterMatchesCurrentTaskOnly()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(WorkflowFilterForm.builder()
                        .scheme(schemeA.getId()).step(schemeAStep2.getId()).build()))
                .build());

        assertTrue("A content at step 2 must match the step filter",
                inodes.contains(aAtStep2.getInode()));
        assertFalse("A content at step 1 must not match the step-2 filter",
                inodes.contains(aAtStep1.getInode()));
        assertFalse("Task-less A content must not match a step filter",
                inodes.contains(aNeverActioned.getInode()));
    }

    /**
     * Two entries combine with OR: a scheme-only entry (all of A) unioned with a step-pinned entry
     * (only the current-task match of C).
     */
    @Test
    public void testOrSemanticsAcrossEntries()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .workflow(List.of(
                        WorkflowFilterForm.builder().scheme(schemeA.getId()).build(),
                        WorkflowFilterForm.builder()
                                .scheme(schemeC.getId()).step(schemeCStep1.getId()).build()))
                .build());

        // All A content (by scheme assignment).
        assertTrue(inodes.contains(aNeverActioned.getInode()));
        assertTrue(inodes.contains(aAtStep1.getInode()));
        assertTrue(inodes.contains(aAtStep2.getInode()));
        // C content only via the current-task step match.
        assertTrue("C content at the pinned step must match via OR",
                inodes.contains(cAtStep1.getInode()));
        assertFalse("Task-less C content must not match a step-pinned C entry",
                inodes.contains(cNeverActioned.getInode()));
    }

    /**
     * With no workflow filter every contentlet in the folder is returned (regression guard — the
     * workflow clause must be a no-op when absent).
     */
    @Test
    public void testNoWorkflowFilterReturnsEverything()
            throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest().build());

        assertTrue(inodes.contains(aNeverActioned.getInode()));
        assertTrue(inodes.contains(aAtStep1.getInode()));
        assertTrue(inodes.contains(aAtStep2.getInode()));
        assertTrue(inodes.contains(cAtStep1.getInode()));
        assertTrue(inodes.contains(cNeverActioned.getInode()));
    }

    /**
     * Folders carry no workflow state, so a workflow filter must suppress them while an unfiltered
     * request keeps them.
     */
    @Test
    public void testFoldersSuppressedWhenWorkflowFilterActive()
            throws DotDataException, DotSecurityException {
        new FolderDataGen().name("driveWfChild_" + System.nanoTime())
                .parent(testFolder).nextPersisted();

        final PaginatedContents unfiltered = contentDriveHelper.driveSearch(
                baseRequest().showFolders(true).build(), systemUser);
        assertTrue("Unfiltered request should list folders",
                unfiltered.folderCount > 0);

        final PaginatedContents filtered = contentDriveHelper.driveSearch(
                baseRequest().showFolders(true)
                        .workflow(List.of(
                                WorkflowFilterForm.builder().scheme(schemeA.getId()).build()))
                        .build(), systemUser);
        assertEquals("A workflow filter must suppress folders",
                0, filtered.folderCount);
    }
}
