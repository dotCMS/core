package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link FolderBulkDeleteProcessor} — the happy path (#37063, spec US1,
 * SC-001).
 * <p>
 * Drives the processor directly with a hand-built {@link Job}, the same pattern
 * {@code BulkUploadProcessorIT} uses — no queue polling needed to prove the delete itself works;
 * that machinery is exercised by the endpoint (T015) and later stories (cancellation, notification).
 */
@EnableWeld
public class FolderBulkDeleteProcessorIT extends Junit5WeldBaseTest {

    @Inject
    FolderBulkDeleteHelper helper;

    private static User admin;
    private static Host site;
    private static FolderAPI folderAPI;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        folderAPI = APILocator.getFolderAPI();
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return String.format("//%s/%s/", site.getHostname(), folder.getName());
    }

    private Job jobFor(final List<Folder> folders) throws Exception {
        final List<String> paths = new ArrayList<>();
        for (final Folder folder : folders) {
            paths.add(pathOf(folder));
        }
        return jobForPaths(paths, admin);
    }

    /**
     * Builds a job for arbitrary paths (not necessarily backed by a {@link Folder} this test
     * created) submitted by an arbitrary user — needed for US2, where several tests submit paths
     * that never resolve, or resolve under a limited-permission user.
     */
    private Job jobForPaths(final List<String> paths, final User submittingUser) {
        return jobForPaths(paths, submittingUser, new DefaultProgressTracker());
    }

    /** Same, but with a caller-supplied tracker — needed to observe what the processor reports. */
    private Job jobForPaths(final List<String> paths, final User submittingUser,
            final ProgressTracker tracker) {
        final List<Map<String, Object>> pathParams = new ArrayList<>();
        for (final String path : paths) {
            final Map<String, Object> p = new HashMap<>();
            p.put("path", path);
            pathParams.add(p);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", submittingUser.getUserId());
        parameters.put("paths", pathParams);

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName(FolderBulkDeleteHelper.QUEUE_NAME)
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(tracker)
                .build();
    }

    /**
     * Records every {@code updateProgress} call verbatim — the same pattern
     * {@code BulkUploadProcessorIT}'s own {@code RecordingProgressTracker} already established,
     * needed here because a direct {@code process(job)} call (no real queue) has no watcher to
     * observe progress changes through.
     */
    private static class RecordingProgressTracker implements ProgressTracker {

        private final List<Float> reported = new ArrayList<>();
        private volatile float current;

        @Override
        public void updateProgress(final float progress) {
            this.current = progress;
            this.reported.add(progress);
        }

        @Override
        public float progress() {
            return current;
        }

        List<Float> reported() {
            return reported;
        }
    }

    /** Grants a role a combined permission bitmask on a permissionable, admin-issued. */
    private void grantPermission(final Permissionable permissionable, final String roleId,
            final int permissionBitmask) throws Exception {
        final Permission permission = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                permissionable.getPermissionId(), roleId, permissionBitmask, true);
        APILocator.getPermissionAPI().save(permission, permissionable, admin, false);
    }

    @SuppressWarnings("unchecked")
    private List<BatchItemResult> resultsOf(final Map<String, Object> metadata) {
        return (List<BatchItemResult>) metadata.get("results");
    }

    private BatchItemResult resultFor(final Map<String, Object> metadata, final String key) {
        return resultsOf(metadata).stream()
                .filter(r -> r.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no result recorded for " + key));
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: Five folders the admin may delete, one containing a piece of content
     * ExpectedResult: All five folders — and the content inside the one that had any — no longer
     * exist once the run finishes (SC-001, US1 scenarios 1-2)
     */
    @Test
    public void test_process_fiveFolders_allDeleted_includingContents() throws Exception {

        final List<Folder> folders = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            folders.add(folder());
        }

        final Folder folderWithContent = folder();
        folders.add(folderWithContent);

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .host(site)
                .folder(folderWithContent)
                .nextPersisted();

        final Job job = jobFor(folders);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        for (final Folder folder : folders) {
            assertThrowsDoesNotExist(folder.getInode());
        }

        assertNull(APILocator.getContentletAPI().find(
                contentlet.getInode(), admin, false),
                "content inside a deleted folder must be gone too");
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A folder deleted through the run
     * ExpectedResult: Behaves observably the same as the shipped single-folder delete — gone from
     * the folder API by inode, same as {@code WebAssetHelper.deleteFolder} already guarantees,
     * since the processor calls the identical unchanged {@code FolderAPI.delete} (US1 scenario 3,
     * FR-006)
     */
    @Test
    public void test_process_oneFolder_behavesLikeTheShippedSingleDelete() throws Exception {

        final Folder folder = folder();
        final String inode = folder.getInode();

        // The shipped single delete's own equivalence check: it resolves, deletes, and the folder
        // no longer resolves by inode afterward. The bulk run must reach the identical end state.
        final Job job = jobFor(List.of(folder));
        new FolderBulkDeleteProcessor().process(job);

        assertThrowsDoesNotExist(inode);
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A job whose parameters carry exactly one path
     * ExpectedResult: Runs as a batch of one — no special-casing back to the synchronous endpoint
     * (US1 scenario 5, D-009)
     */
    @Test
    public void test_process_singlePath_runsAsABatchOfOne() throws Exception {

        final Folder folder = folder();
        final Job job = jobFor(List.of(folder));

        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        assertEquals(1, ((Number) metadata.get("total")).intValue());
        assertEquals(1, ((Number) metadata.get("successCount")).intValue());
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: Four submitted paths covering every outcome kind this run can produce — one
     * succeeds, one is read-only (fails), one is a covering ancestor (succeeds), and its own child
     * (skipped, {@code COVERED_BY_PARENT}) — so success, failure, and the ancestor-dedup skip are
     * all exercised, not only the happy path
     * ExpectedResult: {@code updateProgress} is called exactly four times, once per completed
     * path regardless of its outcome, strictly increasing each time and reaching {@code 1.0} on
     * the last — never at any finer granularity, since nothing inside {@code FolderAPI.delete} is
     * observable (FR-024, FR-025, C-006). This is also the control for FR-024a's heartbeat (proven
     * separately in {@code FolderBulkDeleteHeartbeatIT}): the two signals are deliberately kept
     * apart, so a heartbeat firing mid-delete must never show up here as a fifth, fractional
     * entry.
     */
    @Test
    public void test_process_progressReportedOncePerCompletedFolder_neverFinerGrained()
            throws Exception {

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        grantPermission(site, roleId, PermissionAPI.PERMISSION_READ);

        final Folder deletable = folder();
        grantPermission(deletable, roleId, PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);

        final Folder readOnlyFolder = folder();
        grantPermission(readOnlyFolder, roleId, PermissionAPI.PERMISSION_READ);

        final Folder coveringParent = folder();
        grantPermission(coveringParent, roleId, PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        final Folder coveredChild = new FolderDataGen().parent(coveringParent).name("child")
                .nextPersisted();
        grantPermission(coveredChild, roleId, PermissionAPI.PERMISSION_READ);

        final List<String> paths = List.of(pathOf(deletable), pathOf(readOnlyFolder),
                pathOf(coveringParent), pathOf(coveringParent) + coveredChild.getName() + "/");

        final RecordingProgressTracker tracker = new RecordingProgressTracker();
        final Job job = jobForPaths(paths, limitedUser, tracker);

        new FolderBulkDeleteProcessor().process(job);

        final List<Float> reported = tracker.reported();
        assertEquals(4, reported.size(),
                "one updateProgress call per completed path (success, failure, and skip alike), "
                        + "never more — got: " + reported);

        float previous = 0.0f;
        for (final Float value : reported) {
            assertTrue(value > previous,
                    "each report must strictly increase over the last — got: " + reported);
            previous = value;
        }
        assertEquals(1.0f, reported.get(reported.size() - 1), 0.001f,
                "the last completed path must bring progress to exactly 1.0");
    }

    // ------------------------------------------------------------------------------------------
    // User Story 2 - a folder that cannot be deleted does not take the run down with it (T027-T031)
    // ------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: Five folders submitted by the same limited user — three the user may fully
     * delete, one they may only read (not edit), and one path that never resolves
     * ExpectedResult: The three deletable folders are gone; the read-only one is untouched and
     * recorded FAILED/PERMISSION_DENIED; the unresolvable path is recorded FAILED/PATH_NOT_FOUND —
     * distinguishable from each other, and neither aborts the other successes (US2 scenarios 1-4,
     * SC-002, FR-018 — reasons derived from facts, not exception message text)
     */
    @Test
    public void test_process_mixedFailure_permissionDeniedAndUnresolvablePath_threeSucceed()
            throws Exception {

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        grantPermission(site, roleId, PermissionAPI.PERMISSION_READ);

        final List<Folder> deletable = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final Folder f = folder();
            grantPermission(f, roleId, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                    | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
            deletable.add(f);
        }

        final Folder readOnlyFolder = folder();
        grantPermission(readOnlyFolder, roleId, PermissionAPI.PERMISSION_READ);

        final String unresolvablePath = String.format("//%s/does-not-exist-%s/",
                site.getHostname(), UUID.randomUUID());

        final List<String> paths = new ArrayList<>();
        for (final Folder f : deletable) {
            paths.add(pathOf(f));
        }
        paths.add(pathOf(readOnlyFolder));
        paths.add(unresolvablePath);

        final Job job = jobForPaths(paths, limitedUser);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        for (final Folder f : deletable) {
            assertThrowsDoesNotExist(f.getInode());
        }

        final Folder stillThere = folderAPI.find(readOnlyFolder.getInode(), admin, false);
        assertTrue(stillThere != null && UtilMethods.isSet(stillThere.getInode()),
                "the read-only folder must not have been deleted");

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        assertEquals(5, ((Number) metadata.get("total")).intValue());
        assertEquals(3, ((Number) metadata.get("successCount")).intValue());
        assertEquals(2, ((Number) metadata.get("failedCount")).intValue());

        final BatchItemResult permissionResult = resultFor(metadata, pathOf(readOnlyFolder));
        assertEquals(BatchItemStatus.FAILED, permissionResult.status());
        assertEquals(BatchFailureReason.PERMISSION_DENIED, permissionResult.reason().orElseThrow());

        final BatchItemResult notFoundResult = resultFor(metadata, unresolvablePath);
        assertEquals(BatchItemStatus.FAILED, notFoundResult.status());
        assertEquals(BatchFailureReason.PATH_NOT_FOUND, notFoundResult.reason().orElseThrow());
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A folder the submitting user cannot even read — distinct from
     * {@code test_process_mixedFailure_...} above, where the folder resolves fine and only to
     * delete itself (inside {@code announceAndDelete}) fails on missing EDIT. Here resolution
     * itself ({@code deleteOne}'s own {@code DotSecurityException} catch) is what fails, since
     * {@code FolderAPIImpl.findFolderByPath} refuses a folder the user cannot even READ before
     * the run ever reaches {@code FolderAPI.delete}
     * ExpectedResult: Recorded FAILED/PERMISSION_DENIED, not PATH_NOT_FOUND — a rights refusal
     * during resolution must not be misnamed as though the folder simply does not exist (#37685
     * review)
     */
    @Test
    public void test_process_unreadablePath_recordedAsPermissionDenied() throws Exception {

        final Folder unreadableFolder = folder();
        // Deliberately no permission grants at all for limitedUser on this folder or its site —
        // the run must never resolve it.
        final User limitedUser = new UserDataGen().nextPersisted();

        final Job job = jobForPaths(List.of(pathOf(unreadableFolder)), limitedUser);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        final BatchItemResult result = resultFor(metadata, pathOf(unreadableFolder));
        assertEquals(BatchItemStatus.FAILED, result.status());
        assertEquals(BatchFailureReason.PERMISSION_DENIED, result.reason().orElseThrow());

        final Folder stillThere = folderAPI.find(unreadableFolder.getInode(), admin, false);
        assertTrue(stillThere != null && UtilMethods.isSet(stillThere.getInode()),
                "a folder the submitter never had rights to must be untouched");
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A deletable top-level folder containing one subfolder the user may only
     * read, never edit
     * ExpectedResult: Recorded FAILED/PERMISSION_DENIED, not UNCLASSIFIED — {@code
     * FolderAPIImpl.delete}'s recursive call into the subfolder throws a raw
     * {@code DotSecurityException} (the subfolder's own {@code PERMISSION_EDIT} check, which runs
     * before that call's own {@code try} block) that the *parent* call's catch-all then rewraps
     * into a plain {@code DotDataException}; only walking the cause chain
     * ({@code classifyWrappedDeleteFailure}) recovers that this was a rights'
     * refusal. Neither folder is touched, since the parent's own delete never reaches
     * {@code folderFactory.delete} once the child's delete throws.
     */
    @Test
    public void test_process_subfolderWithoutEditRights_recordedAsPermissionDenied()
            throws Exception {

        final User limitedUser = new UserDataGen().nextPersisted();
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId();
        grantPermission(site, roleId, PermissionAPI.PERMISSION_READ);

        final Folder parent = folder();
        grantPermission(parent, roleId, PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);

        final Folder child = new FolderDataGen().parent(parent).name("read-only-child")
                .nextPersisted();
        grantPermission(child, roleId, PermissionAPI.PERMISSION_READ);

        final Job job = jobForPaths(List.of(pathOf(parent)), limitedUser);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        final BatchItemResult result = resultFor(metadata, pathOf(parent));
        assertEquals(BatchItemStatus.FAILED, result.status());
        assertEquals(BatchFailureReason.PERMISSION_DENIED, result.reason().orElseThrow());

        final Folder parentStillThere = folderAPI.find(parent.getInode(), admin, false);
        assertTrue(parentStillThere != null && UtilMethods.isSet(parentStillThere.getInode()),
                "the parent must be untouched — its own recursive delete failed on the child");
        final Folder childStillThere = folderAPI.find(child.getInode(), admin, false);
        assertTrue(childStillThere != null && UtilMethods.isSet(childStillThere.getInode()),
                "the subfolder itself must survive too — its own permission check is what failed");
    }

    /** One folder holding one piece of content, and the non-admin user who will delete it. */
    private record ContentScenario(Folder folder, Contentlet content, User deleter) { }

    /**
     * Builds one folder holding one piece of content, a deleter and a locker, set up the way the
     * manual run of this scenario was (which reported {@code IN_USE} on a real instance):
     * <ul>
     *   <li>Both users are Back-end Users, like anyone who can call the endpoint, and neither is
     *   a CMS admin: {@code canLock} skips the lock check for admins, and a non-backend user only
     *   finds live content in the index, so {@code FolderAPIImpl} would see nothing to destroy
     *   in this never-published content.</li>
     *   <li>Deleter, on the folder: READ, EDIT and EDIT_PERMISSIONS ({@code FolderAPIImpl.delete}
     *   checks the last two). On content inherited from the folder: READ, EDIT and PUBLISH
     *   ({@code canLock} checks EDIT, {@code internalDestroy} checks PUBLISH).</li>
     *   <li>Locker, on content inherited from the folder: READ and EDIT, enough to lock it.</li>
     * </ul>
     * The content keeps the System Workflow {@code ContentTypeDataGen} attaches, like real content.
     */
    private ContentScenario contentScenario(final boolean locked) throws Exception {

        final Role backEndUserRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final User deleter = new UserDataGen().roles(backEndUserRole).nextPersisted();
        final String deleterRoleId = APILocator.getRoleAPI()
                .loadRoleByKey(deleter.getUserId()).getId();
        final User locker = new UserDataGen().roles(backEndUserRole).nextPersisted();
        final String lockerRoleId = APILocator.getRoleAPI()
                .loadRoleByKey(locker.getUserId()).getId();
        grantPermission(site, deleterRoleId, PermissionAPI.PERMISSION_READ);
        grantPermission(site, lockerRoleId, PermissionAPI.PERMISSION_READ);

        final Folder folder = folder();
        final String contentPermissionType = Contentlet.class.getCanonicalName();
        APILocator.getPermissionAPI().save(List.of(
                new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, folder.getPermissionId(),
                        deleterRoleId, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true),
                new Permission(contentPermissionType, folder.getPermissionId(), deleterRoleId,
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                                | PermissionAPI.PERMISSION_PUBLISH, true),
                new Permission(contentPermissionType, folder.getPermissionId(), lockerRoleId,
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true)
        ), folder, admin, false);

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet content = new ContentletDataGen(contentType.id())
                .host(site)
                .folder(folder)
                .nextPersisted();

        if (locked) {
            APILocator.getContentletAPI().lock(content, locker, false);
        }

        return new ContentScenario(folder, content, deleter);
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: The same folder, content and deleter as the {@code IN_USE} test below, but
     * nobody holds a lock on the content
     * ExpectedResult: SUCCESS, and both the folder and the content are gone. The control for the
     * {@code IN_USE} test: the deleter's rights are enough, so a failure there comes from the lock
     */
    @Test
    public void test_process_unlockedContent_nonAdminDeleter_folderAndContentDeleted()
            throws Exception {

        final ContentScenario scenario = contentScenario(false);

        final Job job = jobForPaths(List.of(pathOf(scenario.folder())), scenario.deleter());
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final BatchItemResult result = resultFor(processor.getResultMetadata(job),
                pathOf(scenario.folder()));
        assertEquals(BatchItemStatus.SUCCESS, result.status(),
                "reason: " + result.reason() + ", message: " + result.message());
        assertThrowsDoesNotExist(scenario.folder().getInode());
        assertNull(APILocator.getContentletAPI().find(scenario.content().getInode(), admin, false),
                "the content must have been destroyed with its folder");
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A folder whose only content is locked by another user, deleted by a
     * non-admin who otherwise has every right needed (see the control test above)
     * ExpectedResult: Recorded FAILED/IN_USE, and the folder, the content and the lock survive.
     * {@code canLock} throws {@code DotLockException}, which {@code FolderAPIImpl} rewraps (also
     * through the System Workflow's {@code Destroy} action), so only walking the cause chain
     * recovers it (#37685 review)
     */
    @Test
    public void test_process_contentLockedByAnotherUser_recordedAsInUse() throws Exception {

        final ContentScenario scenario = contentScenario(true);

        final Job job = jobForPaths(List.of(pathOf(scenario.folder())), scenario.deleter());
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final BatchItemResult result = resultFor(processor.getResultMetadata(job),
                pathOf(scenario.folder()));
        assertEquals(BatchItemStatus.FAILED, result.status(),
                "reason: " + result.reason() + ", message: " + result.message());
        assertEquals(BatchFailureReason.IN_USE, result.reason().orElseThrow(),
                "message: " + result.message());

        final Folder folderStillThere = folderAPI.find(scenario.folder().getInode(), admin, false);
        assertTrue(folderStillThere != null && UtilMethods.isSet(folderStillThere.getInode()),
                "the folder must be untouched — its delete failed on the locked content");
        final Contentlet contentAfter = APILocator.getContentletAPI()
                .find(scenario.content().getInode(), admin, false);
        assertNotNull(contentAfter, "the locked content must not have been destroyed");
        assertTrue(APILocator.getVersionableAPI().getLockedBy(contentAfter).isPresent(),
                "the other user's lock must still be in place");
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A path that resolves to a file, not a folder
     * ExpectedResult: Recorded FAILED/PATH_NOT_FOUND, distinguishable from a permission refusal
     * (FR-010)
     */
    @Test
    public void test_process_pathResolvesToFile_recordedAsPathNotFound() throws Exception {

        final Folder parent = folder();
        final Contentlet fileAsset = FileAssetDataGen.createFileAsset(parent, "myfile", ".txt");
        final String filePath = pathOf(parent)
                + fileAsset.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);

        final Job job = jobForPaths(List.of(filePath), admin);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        assertEquals(1, ((Number) metadata.get("failedCount")).intValue());

        final BatchItemResult result = resultFor(metadata, filePath);
        assertEquals(BatchItemStatus.FAILED, result.status());
        assertEquals(BatchFailureReason.PATH_NOT_FOUND, result.reason().orElseThrow());
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A selection naming the site root alongside a deletable folder
     * ExpectedResult: The site root (the system folder in disguise) is recorded
     * FAILED/PROTECTED_FOLDER and never deleted; the rest of the selection still runs (FR-011, US2
     * scenario 6)
     */
    @Test
    public void test_process_siteRootPath_recordedAsProtectedFolder_restOfSelectionStillRuns()
            throws Exception {

        final Folder deletable = folder();
        final String siteRootPath = String.format("//%s/", site.getHostname());

        final Job job = jobForPaths(List.of(siteRootPath, pathOf(deletable)), admin);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        assertThrowsDoesNotExist(deletable.getInode());

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        assertEquals(1, ((Number) metadata.get("successCount")).intValue());
        assertEquals(1, ((Number) metadata.get("failedCount")).intValue());

        final BatchItemResult siteRootResult = resultFor(metadata, siteRootPath);
        assertEquals(BatchItemStatus.FAILED, siteRootResult.status());
        assertEquals(BatchFailureReason.PROTECTED_FOLDER, siteRootResult.reason().orElseThrow());
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A selection naming a folder and one of its own subfolders — submitted with
     * the descendant listed FIRST, so a pass mistaking processing order for the check would get
     * this wrong
     * ExpectedResult: The ancestor is deleted; the descendant is never attempted at all — recorded
     * SKIPPED/COVERED_BY_PARENT, never FAILED, regardless of submission order (FR-013)
     */
    @Test
    public void test_process_ancestorAndDescendant_descendantSkippedNeverAttempted()
            throws Exception {

        final Folder parent = folder();
        final Folder child = new FolderDataGen().parent(parent).name("child").nextPersisted();

        final String parentPath = pathOf(parent);
        final String childPath = parentPath + child.getName() + "/";

        final Job job = jobForPaths(List.of(childPath, parentPath), admin);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        assertThrowsDoesNotExist(parent.getInode());

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        assertEquals(1, ((Number) metadata.get("successCount")).intValue());
        assertEquals(1, ((Number) metadata.get("skippedCount")).intValue());
        assertEquals(0, ((Number) metadata.get("failedCount")).intValue());

        final BatchItemResult childResult = resultFor(metadata, childPath);
        assertEquals(BatchItemStatus.SKIPPED, childResult.status());
        assertEquals(BatchFailureReason.COVERED_BY_PARENT, childResult.reason().orElseThrow());

        final BatchItemResult parentResult = resultFor(metadata, parentPath);
        assertEquals(BatchItemStatus.SUCCESS, parentResult.status());
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: A selection where every path fails to resolve
     * ExpectedResult: The outcome shows zero successes and names every failure — never recorded as
     * a run-level success (US2 scenario 7)
     */
    @Test
    public void test_process_everyPathFails_zeroSuccesses_notRunLevelSuccess() throws Exception {

        final List<String> paths = List.of(
                String.format("//%s/does-not-exist-1-%s/", site.getHostname(), UUID.randomUUID()),
                String.format("//%s/does-not-exist-2-%s/", site.getHostname(), UUID.randomUUID())
        );

        final Job job = jobForPaths(paths, admin);
        final FolderBulkDeleteProcessor processor = new FolderBulkDeleteProcessor();
        processor.process(job);

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        assertEquals(2, ((Number) metadata.get("total")).intValue());
        assertEquals(0, ((Number) metadata.get("successCount")).intValue());
        assertEquals(2, ((Number) metadata.get("failedCount")).intValue());

        for (final String path : paths) {
            assertEquals(BatchFailureReason.PATH_NOT_FOUND, resultFor(metadata, path).reason().orElseThrow());
        }
    }

    private void assertThrowsDoesNotExist(final String inode) throws Exception {
        try {
            final Folder found = folderAPI.find(inode, admin, false);
            assertTrue(found == null || !com.dotmarketing.util.UtilMethods.isSet(found.getInode()),
                    "folder " + inode + " should no longer exist");
        } catch (final DoesNotExistException expected) {
            // also an acceptable "gone" signal, depending on the API's own contract
        }
    }
}
