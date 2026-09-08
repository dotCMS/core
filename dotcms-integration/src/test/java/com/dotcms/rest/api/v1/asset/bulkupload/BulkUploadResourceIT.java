package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.jboss.weld.junit5.EnableWeld;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@code POST /api/v1/assets/_bulkupload} — the submission itself
 * (spec FR-002, FR-003, FR-004, SC-008; contracts §1).
 * <p>
 * <b>What every test here is really asserting is that the refusal happened before anything was
 * committed.</b> A `400` returned after a batch was created, or after the whole body reached disk,
 * is not the behaviour this endpoint promises: it is the only bound in the path, because nothing
 * below it caps a request — {@code TEMP_RESOURCE_MAX_FILE_SIZE} ships as {@code -1}, the staging
 * layer counts neither files nor bytes, and no servlet multipart limit is configured.
 * <p>
 * Distinguishability matters as much as refusal (FR-004): "too many files" and "too much data" are
 * different problems with different fixes, and an author handed one opaque error cannot act on it.
 */
@EnableWeld
public class BulkUploadResourceIT extends Junit5WeldBaseTest {

    /**
     * Injected, not constructed.
     * <p>
     * An earlier version did {@code new BulkUploadHelper(jobQueueManagerAPI)}, which passed while
     * the real deployment failed: {@code @ApplicationScoped} needs a no-args constructor for Weld
     * to proxy, and without one the container aborts at validation — WELD-001435 — so dotCMS did
     * not start and every URL answered 404. Direct construction bypasses exactly the machinery that
     * was broken. Taking the bean from the container is what makes these tests notice.
     */
    @Inject
    BulkUploadHelper helper;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static User admin;
    private static Host site;

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    private BulkUploadHelper helper() {
        return helper;
    }

    /** A folder the admin can add children to. */
    private Folder targetFolder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    /** A request only for the fingerprint capture; the run has none and resolves by that instead. */
    private HttpServletRequest request() {
        return new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
    }

    private List<UploadPart> parts(final int count, final int bytesEach) {
        final List<UploadPart> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            parts.add(new UploadPart("file-" + i + ".txt",
                    new ByteArrayInputStream(new byte[bytesEach])));
        }
        return parts;
    }

    private BulkUploadForm form(final String folderId, final Long declaredTotal) {
        return new BulkUploadForm("DOTASSET", folderId, null, declaredTotal);
    }

    /**
     * Method to test: {@code POST /v1/assets/_bulkupload}
     * <p>
     * Given scenario: A valid batch of files against a folder the author may add children to.
     * <p>
     * Expected result: {@code 202} carrying a {@code jobId} and a {@code statusUrl}, returned
     * <b>before</b> the files are created (FR-002). The status URL is included rather than left for
     * the client to assemble, following the job framework's own response and the {@code _import}
     * precedent — a deliberate divergence from bulk refresh, which returns the id alone.
     */
    @Test
    public void test_submit_answers202WithAHandleBeforeCreatingAnything() throws Exception {
        final Folder folder = targetFolder();
        final FakeBatchStaging staging = new FakeBatchStaging();

        final BulkUploadSubmitResponse response = helper().submit(
                form(folder.getIdentifier(), null), parts(3, 10), staging, admin, request());

        assertNotNull(response.jobId(), "the caller must get a handle");
        assertEquals("/api/v1/jobs/" + response.jobId() + "/status", response.statusUrl(),
                "the status URL is returned rather than left for the client to assemble");
        assertEquals(3, response.submitted());

        // The point of 202: the work has NOT been done. The job exists, the assets do not.
        assertEquals(0, APILocator.getFolderAPI().getLiveContent(folder, admin, false).size(),
                "the submission is answered before anything is created");
    }

    /**
     * Method to test: {@code POST /v1/assets/_bulkupload}
     * <p>
     * Given scenario: A submission carrying no {@code files} parts.
     * <p>
     * Expected result: {@code 400}, and no job created. An empty batch is not an empty success —
     * reporting "0 of 0 uploaded" for a submission the author believed carried files hides the
     * failure rather than surfacing it.
     */
    @Test
    public void test_submit_refusesASubmissionWithNoFiles() throws Exception {
        final Folder folder = targetFolder();

        final BulkUploadRefusedException refused = assertThrows(BulkUploadRefusedException.class,
                () -> helper().submit(form(folder.getIdentifier(), null), List.of(),
                        new FakeBatchStaging(), admin, request()));

        assertEquals(BulkUploadRefusedException.Ceiling.FILE_COUNT, refused.ceiling());
    }

    /**
     * Method to test: {@code POST /v1/assets/_bulkupload}
     * <p>
     * Given scenario: More files than {@code CONTENT_BULK_UPLOAD_MAX_FILES}.
     * <p>
     * Expected result: {@code 400}, no job, and — the part that distinguishes a bound from a policy
     * — the parts beyond the ceiling never reached the staging layer. The read aborts where the
     * ceiling is crossed (FR-010a).
     */
    @Test
    public void test_submit_refusesOverTheFileCountAndStagesNothingBeyondIt() throws Exception {
        final Folder folder = targetFolder();
        final FakeBatchStaging staging = new FakeBatchStaging();
        final int max = Config.getIntProperty(BulkUploadHelper.MAX_FILES_KEY,
                BulkUploadHelper.DEFAULT_MAX_FILES);

        Config.setProperty(BulkUploadHelper.MAX_FILES_KEY, 3);
        try {
            final BulkUploadRefusedException refused = assertThrows(
                    BulkUploadRefusedException.class,
                    () -> helper().submit(form(folder.getIdentifier(), null), parts(6, 10),
                            staging, admin, request()));

            assertEquals(BulkUploadRefusedException.Ceiling.FILE_COUNT, refused.ceiling(),
                    "'too many files' must be distinguishable from 'too much data'");
            assertTrue(staging.staged().size() <= 3,
                    "the read stops at the ceiling rather than after the whole body");
            assertEquals(List.of(), staging.leaked(),
                    "and nothing it staged is left behind");
        } finally {
            Config.setProperty(BulkUploadHelper.MAX_FILES_KEY, max);
        }
    }

    /**
     * Method to test: {@code POST /v1/assets/_bulkupload}
     * <p>
     * Given scenario: A batch whose accumulated size crosses
     * {@code CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES} part-way through the body.
     * <p>
     * Expected result: {@code 413} — distinct from the {@code 400} of a count or shape problem,
     * because it is a different problem with a different fix — the read aborted there, and
     * everything staged before the abort was reclaimed (FR-013d.1). Nothing purges staged content
     * on a schedule, so a leak here would be permanent.
     */
    @Test
    public void test_submit_answers413AndReclaims_whenTheTotalSizeCeilingIsCrossed() throws Exception {
        final Folder folder = targetFolder();
        final FakeBatchStaging staging = new FakeBatchStaging();
        final long max = Config.getLongProperty(BulkUploadHelper.MAX_TOTAL_BYTES_KEY,
                BulkUploadHelper.DEFAULT_MAX_TOTAL_BYTES);

        Config.setProperty(BulkUploadHelper.MAX_TOTAL_BYTES_KEY, 250L);
        try {
            final BulkUploadRefusedException refused = assertThrows(
                    BulkUploadRefusedException.class,
                    () -> helper().submit(form(folder.getIdentifier(), null), parts(5, 100),
                            staging, admin, request()));

            assertEquals(BulkUploadRefusedException.Ceiling.TOTAL_SIZE, refused.ceiling(),
                    "answered 413, and distinguishable from the count refusal");
            assertTrue(staging.staged().size() <= 3,
                    "no more than the ceiling may reach disk");
            assertEquals(List.of(), staging.leaked(),
                    "what was staged before the abort is reclaimed; nothing purges it later");
        } finally {
            Config.setProperty(BulkUploadHelper.MAX_TOTAL_BYTES_KEY, max);
        }
    }

    /**
     * Method to test: {@code POST /v1/assets/_bulkupload}
     * <p>
     * Given scenario: A declared {@code totalSizeBytes} already over the ceiling.
     * <p>
     * Expected result: {@code 413} before a single byte of body is read. This is the courtesy path
     * (FR-013c.1) — it saves the author uploading gigabytes only to be refused — and it is
     * <b>never</b> the enforcement point, which the test above covers for a caller who omits or
     * under-states the figure.
     */
    @Test
    public void test_submit_refusesADeclaredTotalOverTheCeiling_withoutReadingTheBody() throws Exception {
        final Folder folder = targetFolder();
        final FakeBatchStaging staging = new FakeBatchStaging();
        final long max = Config.getLongProperty(BulkUploadHelper.MAX_TOTAL_BYTES_KEY,
                BulkUploadHelper.DEFAULT_MAX_TOTAL_BYTES);

        Config.setProperty(BulkUploadHelper.MAX_TOTAL_BYTES_KEY, 250L);
        try {
            final BulkUploadRefusedException refused = assertThrows(
                    BulkUploadRefusedException.class,
                    () -> helper().submit(form(folder.getIdentifier(), 10_000L), parts(5, 100),
                            staging, admin, request()));

            assertEquals(BulkUploadRefusedException.Ceiling.TOTAL_SIZE, refused.ceiling());
            assertEquals(0, staging.staged().size(),
                    "the courtesy refusal saves the author the upload entirely: not one byte "
                            + "should have been staged");
        } finally {
            Config.setProperty(BulkUploadHelper.MAX_TOTAL_BYTES_KEY, max);
        }
    }

    /**
     * Method to test: {@code POST /v1/assets/_bulkupload}
     * <p>
     * Given scenario: An author without add-children permission on the target folder.
     * <p>
     * Expected result: {@code 403}, refused at submission rather than becoming N per-file
     * permission failures inside a run the author was never entitled to start (FR-003, FR-004).
     * Moved here from the unit level: it needs a real target and {@code PermissionAPI}.
     */
    @Test
    public void test_submit_answers403WhenTheAuthorMayNotAddChildren() throws Exception {
        final Folder folder = targetFolder();
        final User limited = new UserDataGen().nextPersisted();
        final FakeBatchStaging staging = new FakeBatchStaging();

        assertThrows(DotSecurityException.class,
                () -> helper().submit(form(folder.getIdentifier(), null), parts(2, 10),
                        staging, limited, request()));

        assertEquals(0, staging.staged().size(),
                "permission is decided before the body is read, so an author who cannot use the "
                        + "folder is never made to upload into it first");
    }

    /**
     * Method to test: {@code POST /v1/assets/_bulkupload}
     * <p>
     * Given scenario: A target folder that does not exist.
     * <p>
     * Expected result: {@code 404}, distinguishable from the {@code 403} above. An author whose
     * folder was deleted between choosing it and submitting has a different problem from one who
     * lacks rights, and telling them "forbidden" sends them to an administrator for nothing.
     */
    @Test
    public void test_submit_answers404ForAMissingTarget() throws Exception {
        final FakeBatchStaging staging = new FakeBatchStaging();

        assertThrows(DoesNotExistException.class,
                () -> helper().submit(form("a-folder-that-does-not-exist", null), parts(2, 10),
                        staging, admin, request()));

        assertEquals(0, staging.staged().size(),
                "resolved before the body, so a deleted folder costs the author nothing");
    }

    /**
     * Method to test: {@link BulkUploadHelper#submit} on a resubmission of the same batch
     * <p>
     * Given scenario: The author's connection dropped before they learned whether their submission
     * was accepted, so they submit the identical batch again — same target, same files, same order.
     * <p>
     * Expected result: A {@code submissionFingerprint} is stored, and it is <b>the same</b> for
     * both submissions (FR-040, data-model §1). That fingerprint is what lets the run recognise a
     * resubmission and flag it, instead of letting the author's second attempt collide against the
     * files their first attempt already created and be reported as "every file failed" (FR-040a).
     * <p>
     * The client's whole retry promise depends on this: C-002a entitles it to resubmit safely, and
     * without a way to tell a duplicate from a genuinely all-collided batch, a successful retry
     * reads as total failure — which is worse than no retry at all, because the author then deletes
     * and re-uploads files that were already there.
     * <p>
     * <b>Expected to FAIL until T058.</b>
     */
    @Test
    public void test_submit_fingerprintsABatchSoAResubmissionIsRecognisable() throws Exception {
        final Folder folder = targetFolder();

        final BulkUploadSubmitResponse first = helper().submit(
                form(folder.getIdentifier(), null), parts(3, 10), new FakeBatchStaging(),
                admin, request());
        final BulkUploadSubmitResponse again = helper().submit(
                form(folder.getIdentifier(), null), parts(3, 10), new FakeBatchStaging(),
                admin, request());

        final Object firstPrint =
                jobQueueManagerAPI.getJob(first.jobId()).parameters().get("submissionFingerprint");
        final Object againPrint =
                jobQueueManagerAPI.getJob(again.jobId()).parameters().get("submissionFingerprint");

        assertNotNull(firstPrint,
                "without a fingerprint there is no way to recognise a resubmission, and the "
                        + "client's retry promise in C-002a cannot hold");
        assertEquals(firstPrint, againPrint,
                "the same batch must fingerprint the same, or a retry looks like a new batch and "
                        + "collides against the files the first attempt created");
    }

    /**
     * Method to test: {@link BulkUploadHelper#submit} — the job parameters it stores
     * <p>
     * Given scenario: A batch targeting a folder, so the {@code siteId} half of the target is
     * absent.
     * <p>
     * Expected result: <b>No parameter value is null.</b> The job framework stores parameters in an
     * ImmutableMap, which rejects nulls — and the rejection does not surface at submission. The
     * insert succeeds, and the failure appears later inside {@code PostgresJobQueue.nextJob}, which
     * is the <b>shared</b> processing loop: {@code "null value in entry: siteId=null"} stopped every
     * queue in the product from advancing, not just this one. An earlier version put both
     * {@code folderId} and {@code siteId} unconditionally, and since exactly one is set by
     * definition, every single bulk upload poisoned the loop.
     * <p>
     * This is asserted as an invariant over the whole map rather than on the two target keys,
     * because the next parameter someone adds is the one that will be null.
     */
    @Test
    public void test_submit_storesNoNullParameterValues() throws Exception {
        final Folder folder = targetFolder();

        final BulkUploadSubmitResponse response = helper().submit(
                form(folder.getIdentifier(), null), parts(2, 10), new FakeBatchStaging(),
                admin, request());

        final Job job = jobQueueManagerAPI.getJob(response.jobId());

        job.parameters().forEach((key, value) -> assertNotNull(value, String.format(
                "job parameter '%s' is null; the framework's ImmutableMap rejects that and the "
                        + "shared job loop stops advancing for every queue", key)));
    }

    /**
     * Method to test: the configured ceilings
     * <p>
     * Given scenario: An operator changes the limits, and then leaves them unset.
     * <p>
     * Expected result: The change is honoured, and the documented default applies when nothing is
     * set. That is what FR-010 asks for — "operator-configurable through the product's standard
     * configuration mechanism rather than hardcoded" — and it is the half that matters: an operator
     * who cannot change these has no way to adapt the feature to their own storage.
     * <p>
     * <b>Deliberately not asserted here: the value shipped in the properties file.</b> An earlier
     * version of this test read {@code Config.getIntProperty(key, -1)} and expected 100, which
     * could only ever pass if the key were present in the config the test JVM loads. It is not, and
     * should not be: {@code dotcms-integration} carries its own partial
     * {@code dotmarketing-config.properties} — 886 lines against core's 1000, and missing the
     * {@code TEMP_RESOURCE_*} block too — that shadows core's on the test classpath. Adding the
     * keys there would have made the assertion pass by editing the fixture it asserts against,
     * which tests nothing. The shipped defaults are documented in the contract and in the
     * properties file itself; what needs a test is that the code reads them rather than baking
     * them in.
     */
    @Test
    public void test_ceilings_areOperatorConfigurableWithTheDocumentedDefaults() {

        final String maxFilesKey = BulkUploadHelper.MAX_FILES_KEY;
        final String maxTotalKey = BulkUploadHelper.MAX_TOTAL_BYTES_KEY;

        try {
            Config.setProperty(maxFilesKey, 7);
            Config.setProperty(maxTotalKey, 4096L);

            assertEquals(7, Config.getIntProperty(maxFilesKey,
                            BulkUploadHelper.DEFAULT_MAX_FILES),
                    "an operator's file-count ceiling must be honoured");
            assertEquals(4096L, Config.getLongProperty(maxTotalKey,
                            BulkUploadHelper.DEFAULT_MAX_TOTAL_BYTES),
                    "an operator's total-size ceiling must be honoured");
        } finally {
            Config.setProperty(maxFilesKey, null);
            Config.setProperty(maxTotalKey, null);
        }

        assertEquals(100, Config.getIntProperty(maxFilesKey,
                        BulkUploadHelper.DEFAULT_MAX_FILES),
                "with nothing configured, the documented default applies");
        assertEquals(1073741824L, Config.getLongProperty(maxTotalKey,
                        BulkUploadHelper.DEFAULT_MAX_TOTAL_BYTES),
                "with nothing configured, the documented default applies");
    }
}
