package com.dotcms.rest.api.v1.asset.bulkdelete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.JobState;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the overlap guard at submission (#37063, FR-029, FR-029a, FR-029b, US5,
 * SC-007, contracts §1).
 * <p>
 * <b>Why an in-flight run needs to be genuinely slow.</b> Every scenario here needs a run that is
 * still discoverable via {@code getActiveJobs} while the test submits a second one — the same
 * "big folder" technique {@code FolderBulkDeleteCancellationIT} uses for the same reason: a folder
 * carrying enough content that its own delete call outlasts the window the test needs.
 */
@EnableWeld
public class FolderBulkDeleteOverlapIT extends Junit5WeldBaseTest {

    private static final int BIG_FOLDER_CONTENT_COUNT = 60;

    @Inject
    FolderBulkDeleteHelper helper;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static User admin;
    private static ContentType contentType;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    private Host site() {
        return new SiteDataGen().nextPersisted();
    }

    private Folder folder(final Host site, final Folder parent) {
        final FolderDataGen dataGen = new FolderDataGen().site(site);
        if (parent != null) {
            dataGen.parent(parent);
        }
        return dataGen.nextPersisted();
    }

    /**
     * The full site-qualified path, not just the leaf name — {@link Folder#getName()} would be
     * wrong for a nested folder ({@code parent/big/descendant}), since it answers only the last
     * segment and would make two genuinely nested folders compare as unrelated paths.
     * {@link Folder#getPath()} already carries the leading and trailing slashes (matches
     * {@code ContainerLoader}'s own {@code "//" + host.getHostname() + folder.getPath()} pattern).
     */
    private String pathOf(final Host site, final Folder folder) {
        return String.format("//%s%s", site.getHostname(), folder.getPath());
    }

    private FolderBulkDeleteSubmitResponse submit(final String... paths) throws Exception {
        return helper.submit(
                FolderBulkDeleteForm.builder().assetPaths(List.of(paths)).build(), admin);
    }

    /**
     * Starts the queue worker if it is not already running.
     * <p>
     * <b>Every test method that waits for a job to leave {@code PENDING} must call this itself</b>
     * rather than relying on another method in the class having already started it — JUnit5 does
     * not guarantee method execution order, and a job the worker never picks up sits
     * {@code PENDING} forever, timing out deterministically rather than flaking. Found the hard
     * way: {@code test_submit_samePathAfterFirstRunTerminal_accepted} was the one method here that
     * skipped this and relied on ambient state from whichever other method happened to run first.
     */
    private void ensureQueueStarted() throws Exception {
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Submits a folder heavy enough that its own delete call outlasts this test's window, and
     * waits for the run to actually be {@code RUNNING} (not merely accepted) before returning —
     * every scenario here depends on the run being discoverable via {@code getActiveJobs} while
     * the test submits a second one.
     */
    private String submitLongRunningJob(final String path) throws Exception {
        ensureQueueStarted();
        final String jobId = submit(path).jobId();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.RUNNING);
        return jobId;
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: A folder is being deleted by an in-flight run; a second submission names
     * that exact path, its ancestor, or its descendant
     * ExpectedResult: All three are refused {@code 409 OVERLAPPING_RUN} before any job is created,
     * and the refusal names the conflicting folder — never the other submitter, since no field in
     * the body could carry one (FR-029, FR-029a, SC-007, contracts §1)
     */
    @Test
    public void test_submit_sameAncestorOrDescendantOfInFlightRun_refused409_noJobCreated()
            throws Exception {

        final Host site = site();
        final Folder parent = folder(site, null);
        final Folder big = folder(site, parent);
        for (int i = 0; i < BIG_FOLDER_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(big).nextPersisted();
        }
        final Folder descendant = folder(site, big);

        final String bigPath = pathOf(site, big);
        final String parentPath = pathOf(site, parent);
        final String descendantPath = pathOf(site, descendant);

        submitLongRunningJob(bigPath);

        final FolderBulkDeleteRefusedException samePath = assertThrows(
                FolderBulkDeleteRefusedException.class, () -> submit(bigPath),
                "the exact path an in-flight run is already covering must be refused");
        assertOverlapRefusal(samePath, bigPath);

        final FolderBulkDeleteRefusedException ancestor = assertThrows(
                FolderBulkDeleteRefusedException.class, () -> submit(parentPath),
                "an ancestor of an in-flight path would remove it as a side effect, so it must be "
                        + "refused too");
        assertOverlapRefusal(ancestor, parentPath);

        final FolderBulkDeleteRefusedException childPath = assertThrows(
                FolderBulkDeleteRefusedException.class, () -> submit(descendantPath),
                "a descendant of an in-flight path would be removed out from under that run, so it "
                        + "must be refused too");
        assertOverlapRefusal(childPath, descendantPath);
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: A folder is being deleted by an in-flight run; a second submission names
     * the same folder with different case, site included — folder and site resolution are
     * case-insensitive, so it is the same folder
     * ExpectedResult: Refused {@code 409 OVERLAPPING_RUN}, same as the exact path (#37685 review)
     */
    @Test
    public void test_submit_samePathDifferentCaseAsInFlightRun_refused409() throws Exception {

        final Host site = site();
        final Folder big = folder(site, null);
        for (int i = 0; i < BIG_FOLDER_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(big).nextPersisted();
        }
        final String bigPath = pathOf(site, big);
        final String upperCasePath = bigPath.toUpperCase();

        submitLongRunningJob(bigPath);

        final FolderBulkDeleteRefusedException refusal = assertThrows(
                FolderBulkDeleteRefusedException.class, () -> submit(upperCasePath),
                "the same folder spelled with different case must be refused like the exact path");
        assertOverlapRefusal(refusal, upperCasePath);
    }

    private void assertOverlapRefusal(final FolderBulkDeleteRefusedException refusal,
            final String conflictingPath) {
        assertEquals("OVERLAPPING_RUN", refusal.errorCode());
        assertEquals(javax.ws.rs.core.Response.Status.CONFLICT, refusal.status());
        assertTrue(refusal.getMessage().contains(conflictingPath),
                "the refusal must name the conflicting folder: " + refusal.getMessage());
        assertFalse(refusal.getMessage().contains(admin.getUserId()),
                "the refusal must never name the other submitter — there is no field in this "
                        + "body where a user id could appear");
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: Two submissions for the very same, not-yet-active path race each other —
     * neither can see the other via {@code getActiveJobs} until one of them actually commits
     * ExpectedResult: Exactly one job is created and the other is refused {@code OVERLAPPING_RUN},
     * every time — proving the per-site advisory lock actually serializes the
     * check-then-act window rather than merely making the race unlikely (FR-029b). Without the
     * lock this is a genuine TOCTOU race: both submissions could read "no active job yet" before
     * either's {@code createJob} commits, and this test would occasionally let both through.
     */
    @Test
    public void test_submit_concurrentOverlappingSubmissions_onlyOneJobCreated() throws Exception {

        final Host site = site();
        final Folder folder = folder(site, null);
        final String path = pathOf(site, folder);

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch go = new CountDownLatch(1);
        final AtomicReference<FolderBulkDeleteSubmitResponse> succeeded = new AtomicReference<>();
        final AtomicReference<FolderBulkDeleteRefusedException> refused = new AtomicReference<>();
        final AtomicReference<Exception> unexpected = new AtomicReference<>();

        final Runnable attempt = () -> {
            try {
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                final FolderBulkDeleteSubmitResponse response = submit(path);
                if (succeeded.getAndSet(response) != null) {
                    unexpected.set(new IllegalStateException(
                            "two concurrent submissions for the same path both succeeded — the "
                                    + "check-then-act race was not closed"));
                }
            } catch (final FolderBulkDeleteRefusedException e) {
                refused.set(e);
            } catch (final Exception e) {
                unexpected.set(e);
            }
        };

        try {
            executor.submit(attempt);
            executor.submit(attempt);
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertTrue(unexpected.get() == null,
                "no unexpected exception, only exactly one success and one OVERLAPPING_RUN "
                        + "refusal: " + unexpected.get());
        assertNotNull(succeeded.get(), "exactly one of the two concurrent submissions must win");
        assertNotNull(refused.get(), "and the other must be refused, not silently dropped or both "
                + "accepted");
        assertEquals("OVERLAPPING_RUN", refused.get().errorCode());
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: A run is in flight for one folder; a second submission names an unrelated
     * folder in the same site
     * ExpectedResult: Accepted — the guard compares paths, not sites or submitters, so unrelated
     * work is never blocked by it (US5 scenario 2)
     */
    @Test
    public void test_submit_unrelatedPathWhileFirstStillInFlight_accepted() throws Exception {

        final Host site = site();
        final Folder big = folder(site, null);
        for (int i = 0; i < BIG_FOLDER_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(big).nextPersisted();
        }
        final Folder unrelated = folder(site, null);

        submitLongRunningJob(pathOf(site, big));

        final FolderBulkDeleteSubmitResponse response = submit(pathOf(site, unrelated));

        assertNotNull(response.jobId(),
                "a path unrelated to any in-flight run must be accepted while the other runs");
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: A run reaches a terminal state; a later submission names the same path
     * ExpectedResult: Accepted — the guard is about in-flight work, not history (US5 scenario 3).
     * A completed run's paths are exactly as submittable again as any other folder's.
     */
    @Test
    public void test_submit_samePathAfterFirstRunTerminal_accepted() throws Exception {

        ensureQueueStarted();

        final Host site = site();
        final Folder folder = folder(site, null);
        final String path = pathOf(site, folder);

        final String firstJobId = submit(path).jobId();
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .until(() -> jobQueueManagerAPI.getJob(firstJobId).state() == JobState.SUCCESS);

        final FolderBulkDeleteSubmitResponse response = submit(path);

        assertNotNull(response.jobId(),
                "once the first run has reached a terminal state, the same path is submittable "
                        + "again — the guard does not remember completed runs");
    }
}
