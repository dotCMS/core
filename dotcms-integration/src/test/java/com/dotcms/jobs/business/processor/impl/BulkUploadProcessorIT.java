package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.junit5.EnableWeld;
import com.dotmarketing.util.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the bulk-upload run itself — what actually reaches the folder
 * (spec FR-005, FR-006, SC-001).
 * <p>
 * Where {@code BulkUploadResourceIT} covers the submission, these cover the work. The two are
 * separate because the whole point of the design is that they happen at different times: the
 * submission is answered before any file exists, so "accepted" and "created" are different claims
 * and have to be verified separately.
 * <p>
 * Later phases add to this class rather than replacing it — partial failure (US2), progress and
 * cancellation (US4). Only the US1 cases are here.
 */
@EnableWeld
public class BulkUploadProcessorIT extends Junit5WeldBaseTest {

    private final List<FieldVariable> restrictions = new ArrayList<>();

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /** Resolved per test rather than in a static initialiser, which ran before the API was up. */
    private User admin() throws Exception {
        return APILocator.systemUser();
    }

    private Host site() {
        return new SiteDataGen().nextPersisted();
    }

    /**
     * Renders the per-item reasons into the assertion message.
     * <p>
     * Without this a failure reads "expected 10 but was 0" and says nothing about <b>why</b> —
     * which cost a full diagnostic cycle once already. The reasons are recorded per file precisely
     * so they can be read; a test that hides them wastes what the feature went to trouble to
     * produce.
     */
    @SuppressWarnings("unchecked")
    private String describe(final Map<String, Object> outcome) {
        final Object results = outcome.get("results");
        if (!(results instanceof List)) {
            return "none recorded";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Object item : (List<Object>) results) {
            sb.append("\n  ").append(item);
        }
        return sb.toString();
    }

    /**
     * A request carrying the author, because staging needs one: {@code createTempFile} builds its
     * allow-list from {@code PortalUtil.getUser(request)}, which reads {@code WebKeys.USER} first.
     * The <b>run</b> has no request at all — that is the point of the fingerprint — but the
     * <b>submission</b> does, and this stands in for it.
     */
    private HttpServletRequest request() throws Exception {
        final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
        request.setAttribute(com.liferay.portal.util.WebKeys.USER, admin());
        return request;
    }

    /**
     * Stages {@code count} files the way the endpoint would, and builds the job the queue would
     * hand the processor. Staging for real rather than faking it, because the whole reason this
     * class exists is that the run has no HTTP request: the content has to be retrievable by the
     * captured fingerprint, and a fake would hide it if it were not.
     */
    private Job jobFor(final Folder folder, final int count) throws Exception {
        return jobFor(folder, count, 0);
    }

    /**
     * As above, but every file is padded to at least {@code minBytes}. Only the throughput
     * measurement needs realistically sized content; everywhere else a few bytes say the same
     * thing faster.
     */
    private Job jobFor(final Folder folder, final int count, final int minBytes) throws Exception {
        final HttpServletRequest request = request();
        final List<Map<String, Object>> stagedFiles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String fileName = "bulk-" + UUID.randomUUID() + ".txt";
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFile(
                    fileName, request, new ByteArrayInputStream(bodyOf(i, minBytes)));

            final Map<String, Object> file = new HashMap<>();
            file.put("tempFileId", tempFile.id);
            file.put("fileName", fileName);
            file.put("sizeBytes", tempFile.length());
            file.put("mimeType", tempFile.mimeType);
            stagedFiles.add(file);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("baseType", "DOTASSET");
        parameters.put("folderId", folder.getIdentifier());
        parameters.put("targetId", folder.getIdentifier());
        parameters.put("userId", admin().getUserId());
        parameters.put("stagedFiles", stagedFiles);
        parameters.put("requestFingerprint",
                APILocator.getTempFileAPI().getRequestFingerprint(request));

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName("assetBulkUpload")
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(new DefaultProgressTracker())
                .build();
    }

    /** File content: distinct per file, padded to {@code minBytes} where a size is asked for. */
    private static byte[] bodyOf(final int index, final int minBytes) {
        final byte[] body = new byte[Math.max(minBytes, 16)];
        Arrays.fill(body, (byte) ('a' + (index % 26)));
        return body;
    }

    /**
     * Method to test: the bulk-upload processor, end to end
     * <p>
     * Given scenario: A batch of 10 valid files submitted against a folder the author may add
     * children to.
     * <p>
     * Expected result: All 10 exist in that folder when the run reports itself finished, and the
     * outcome's counts say 10 — <b>zero silently discarded</b> (SC-001). This is the criterion the
     * whole ticket exists for: today Content Drive accepts the selection, warns, and uploads the
     * first file only.
     */
    @Test
    public void test_run_createsEveryFileInTheTarget() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 10);

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);

        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(10, ((Number) outcome.get("successCount")).intValue(),
                "every file the author chose must be created — this is the criterion the whole "
                        + "ticket exists for, since today the selection is accepted and only the "
                        + "first file lands.\nRecorded per-item results: " + describe(outcome));
        assertEquals(0, ((Number) outcome.get("failedCount")).intValue());
        assertEquals(10, ((Number) outcome.get("total")).intValue(),
                "and the counts are authoritative, not the number the client believes it sent");

        final List<Contentlet> inFolder =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);
        assertEquals(10, inFolder.size(), "zero silently discarded");

        // Settled: the run fires NEW, so these are drafts. See
        // test_run_leavesEveryFileAsADraftRatherThanPublishingIt for the assertion that pins it —
        // this one would pass either way, because publishing leaves a working version too.
    }

    /**
     * Method to test: the bulk-upload processor
     * <p>
     * Given scenario: The same file uploaded through this endpoint and through the existing
     * single-file path.
     * <p>
     * Expected result: Observably equivalent — same resolved content type, same permissions, same
     * workflow behaviour (FR-006). The requirement is the <b>equivalence</b>, not the sharing of a
     * particular call site: the single-file path is entered over REST and a background run cannot
     * re-enter it, so what must not vary is what an author can observe afterwards.
     */
    @Test
    public void test_run_createsFilesEquivalentlyToTheSingleFileUpload() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 1);

        new BulkUploadProcessor().process(job);

        final List<Contentlet> created =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);
        assertEquals(1, created.size());

        final Contentlet asset = created.get(0);
        assertEquals("DotAsset", asset.getContentType().variable(),
                "the content type resolves the same way the single-file upload resolves it");
        assertEquals(folder.getInode(), asset.getFolder(),
                "and it lands where the author asked, not at the site root");
        assertTrue(APILocator.getPermissionAPI().doesUserHavePermission(
                        asset, com.dotmarketing.business.PermissionAPI.PERMISSION_READ,
                        admin(), false),
                "permissions are enforced by the same creation path, so they are inherited "
                        + "rather than reinvented");
    }

    /**
     * Method to test: the bulk-upload processor's index policy
     * <p>
     * Given scenario: A batch completes.
     * <p>
     * Expected result: No file was created with a per-item {@code WAIT_FOR} (FR-008). That policy
     * does not merely block on an index refresh — it also flushes the system-wide query cache on
     * every file, so a full batch would charge every other user one flush per file. The
     * batch-level resolution that replaces it is asserted in {@code BulkUploadIndexingIT}, together
     * with its ordering against the completion signal.
     */
    @Test
    public void test_run_doesNotSerializeBehindAPerFileIndexWait() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 5);

        final long startedAt = System.currentTimeMillis();
        new BulkUploadProcessor().process(job);
        final long elapsed = System.currentTimeMillis() - startedAt;

        assertEquals(5, APILocator.getFolderAPI().getWorkingContent(folder, admin(), false).size());

        // A per-file WAIT_FOR blocks on the next index refresh for each file — the default refresh
        // interval is 1s and this repository does not override it — so five files would floor at
        // roughly five seconds plus five system-wide query cache flushes. Generous headroom on
        // purpose: this guards an order-of-magnitude regression, not jitter, because a hard
        // threshold in CI is a flaky test rather than a useful one.
        assertTrue(elapsed < 5_000L, String.format(
                "a 5-file batch took %dms, which is the shape of a per-file index wait rather "
                        + "than one batch-level resolution", elapsed));
    }

    /**
     * Builds a job whose staged files carry the sizes and media types given, so a test can set up
     * a mixed batch without needing real content of those sizes on disk.
     * <p>
     * The measured values are what the run classifies from (research R4), so overriding them here
     * exercises exactly the path production takes.
     */
    private Job jobWith(final Folder folder, final List<Map<String, Object>> overrides)
            throws Exception {
        final Job job = jobFor(folder, overrides.size());

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> staged =
                (List<Map<String, Object>>) job.parameters().get("stagedFiles");

        for (int i = 0; i < overrides.size(); i++) {
            staged.get(i).putAll(overrides.get(i));
        }
        return job;
    }

    private static Map<String, Object> file(final String name, final long size, final String mime) {
        final Map<String, Object> f = new HashMap<>();
        f.put("fileName", name);
        f.put("sizeBytes", size);
        f.put("mimeType", mime);
        return f;
    }

    /** The recorded reason for one file, by name. */
    private BatchFailureReason reasonFor(final Map<String, Object> outcome, final String key) {
        @SuppressWarnings("unchecked")
        final List<BatchItemResult> results = (List<BatchItemResult>) outcome.get("results");
        return results.stream()
                .filter(r -> r.key().equals(key))
                .findFirst()
                .flatMap(BatchItemResult::reason)
                .orElse(null);
    }

    /**
     * Method to test: the run's per-file outcome on a mixed batch
     * <p>
     * Given scenario: Five files of which two are invalid — one over the size ceiling, one of a
     * disallowed media type.
     * <p>
     * Expected result: The three valid files are created, and the two failures are named
     * individually with <b>different</b> reasons (FR-007, SC-002). This is the case the ticket
     * calls normal rather than exceptional: a batch of thirty routinely contains one that is too
     * large or whose name is taken, and a single "upload failed" toast over a thirty-file drop is
     * the outcome to avoid.
     */
    @Test
    public void test_run_reportsEachFailureByNameAndReasonWithoutAbortingTheBatch() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        restrictTypesTo("text/*");
        final long ceiling = Config.getLongProperty(
                "CONTENT_BULK_UPLOAD_FALLBACK_MAX_FILE_BYTES", 209715200L);

        final Job job = jobWith(folder, List.of(
                file("ok-1.txt", 10L, "text/plain"),
                file("huge.mov", ceiling + 1, "video/quicktime"),
                file("ok-2.txt", 10L, "text/plain"),
                file("payload.exe", 10L, "application/x-msdownload"),
                file("ok-3.txt", 10L, "text/plain")));

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);
        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(3, ((Number) outcome.get("successCount")).intValue(),
                "the valid files must still land — one bad file does not take the batch down.\n"
                        + describe(outcome));
        assertEquals(2, ((Number) outcome.get("failedCount")).intValue());

        assertEquals(BatchFailureReason.OVER_SIZE_LIMIT, reasonFor(outcome, "huge.mov"),
                "the size failure names the size rule");
        assertNotEquals(reasonFor(outcome, "huge.mov"), reasonFor(outcome, "payload.exe"),
                "and the two failures are not collapsed into one reason");
    }

    /**
     * Declares an {@code accept} allow list on the batch's binary field, so the type rule is a
     * real rule for the duration of the test.
     * <p>
     * <b>Needed because the default content type restricts nothing.</b> Out of the box a dotAsset
     * declares no allow list, and an empty list means "every type is allowed" — FR-012b and
     * FR-006's equivalence rule both require that, so a test asserting a refusal has to configure
     * the restriction it is asserting about. An earlier version of these two tests did not, and
     * failed against correct behaviour.
     */
    private void restrictTypesTo(final String acceptList) throws Exception {
        final ContentType dotAsset = APILocator.getContentTypeAPI(admin()).find("dotAsset");
        final Field binary = dotAsset.fields().stream()
                .filter(f -> f instanceof BinaryField)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("dotAsset has no binary field"));

        restrictions.add(new FieldVariableDataGen()
                .field(binary)
                .key(BinaryField.ALLOWED_FILE_TYPES)
                .value(acceptList)
                .nextPersisted());
    }

    /**
     * Undoes the restriction after every test.
     * <p>
     * <b>Not optional bookkeeping.</b> The field variable is persisted on the <i>shared</i>
     * {@code dotAsset} content type, so without this the restriction outlives the test that set it
     * and every later case in the JVM inherits it — which is how
     * {@code test_run_createsFilesEquivalentlyToTheSingleFileUpload} started failing after these
     * two were added: its {@code text/plain} files were being refused by an {@code image/*} rule
     * left behind by a neighbour. A failure like that reads as a bug in the code under test and is
     * not.
     */
    @AfterEach
    public void liftRestrictions() {
        restrictions.forEach(variable -> {
            try {
                APILocator.getContentTypeFieldAPI().delete(variable);
            } catch (final Exception e) {
                Logger.warn(this, "Could not lift a test type restriction: " + e.getMessage());
            }
        });
        restrictions.clear();
    }

    /**
     * Method to test: the type rule
     * <p>
     * Given scenario: A content type restricted to images, and a file whose resolved media type is
     * an executable, renamed to an extension that would be permitted.
     * <p>
     * Expected result: Still refused, as {@code DISALLOWED_FILE_TYPE} (FR-012a). The product
     * resolves the type by content detection rather than by trusting the name, so this is a
     * media-type rule — and the client's copy must say so, because telling an author it is an
     * extension rule invites them to rename the file and try again.
     */
    @Test
    public void test_run_refusesADisallowedMediaTypeEvenWhenRenamed() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        restrictTypesTo("image/*");

        final Job job = jobWith(folder, List.of(
                file("looks-harmless.png", 10L, "application/x-msdownload")));

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);
        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(0, ((Number) outcome.get("successCount")).intValue(),
                "renaming must not get a disallowed type past the rule.\n" + describe(outcome));
        assertEquals(BatchFailureReason.DISALLOWED_FILE_TYPE,
                reasonFor(outcome, "looks-harmless.png"));
    }

    /**
     * Method to test: the FR-011a divergence
     * <p>
     * Given scenario: A content type declaring no {@code maxFileLength}, and a file over the
     * configured fallback ceiling.
     * <p>
     * Expected result: Failed as {@code OVER_SIZE_LIMIT} in the batch — <b>and this is the one
     * place a batch is deliberately stricter than a single upload</b>. FR-006 otherwise requires
     * equivalence; FR-011a records this as a knowing exception, because without a fallback the
     * batch would have no per-file bound at all (the content-type rule is unset by default). The
     * test exists so the divergence is deliberate and visible rather than discovered by an operator
     * as a defect.
     */
    @Test
    public void test_run_appliesTheFallbackCeilingWhereTheContentTypeDeclaresNone() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final long fallback = Config.getLongProperty(
                "CONTENT_BULK_UPLOAD_FALLBACK_MAX_FILE_BYTES", 209715200L);

        final Job job = jobWith(folder, List.of(
                file("over-the-fallback.bin", fallback + 1, "application/octet-stream")));

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);
        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(BatchFailureReason.OVER_SIZE_LIMIT,
                reasonFor(outcome, "over-the-fallback.bin"));
    }

    /**
     * Method to test: {@link BulkUploadProcessor#process} — reclaim at the terminal state
     * <p>
     * Given scenario: A batch completes successfully.
     * <p>
     * Expected result: The staged content is gone (FR-033). <b>This is the happy path, which is
     * exactly why it went unchecked:</b> the reclaim was written for the two failure routes — a
     * refusal and a read that dies — and both were tested carefully. A run that succeeds reaches a
     * terminal state too, and nothing purges staged content on a schedule, so every completed batch
     * was leaking its own bytes permanently: invisible to the author, uncollected by any run, and
     * growing with every upload.
     */
    @Test
    public void test_run_reclaimsStagedContentWhenItReachesATerminalState() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 3);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> staged =
                (List<Map<String, Object>>) job.parameters().get("stagedFiles");
        final List<String> tempFileIds = new ArrayList<>();
        staged.forEach(file -> tempFileIds.add(String.valueOf(file.get("tempFileId"))));

        new BulkUploadProcessor().process(job);

        assertEquals(3, APILocator.getFolderAPI().getWorkingContent(folder, admin(), false).size(),
                "the run succeeded, so the assets exist");

        for (final String tempFileId : tempFileIds) {
            final Optional<DotTempFile> leftBehind = APILocator.getTempFileAPI()
                    .getTempFile(List.of(admin().getUserId()), tempFileId);

            assertTrue(leftBehind.isEmpty() || !leftBehind.get().file.exists(), String.format(
                    "staged content '%s' survived a completed run; nothing purges it on a "
                            + "schedule, so this leaks permanently and grows with every batch",
                    tempFileId));
        }
    }

    /**
     * Method to test: {@link BulkUploadProcessor#process} — reclaim after cancellation
     * <p>
     * Given scenario: A run is cancelled, so some of its files were never reached.
     * <p>
     * Expected result: Their content is reclaimed too (FR-033, explicitly "including for files the
     * run never reached"). Those are the files most likely to be forgotten, because no per-item
     * outcome was ever written for them — there is no row pointing at what to clean up.
     */
    @Test
    public void test_run_reclaimsContentOfFilesItNeverReached() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 5);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> staged =
                (List<Map<String, Object>>) job.parameters().get("stagedFiles");
        final String lastTempFileId = String.valueOf(staged.get(4).get("tempFileId"));

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.cancel(job);
        processor.process(job);

        final Optional<DotTempFile> leftBehind = APILocator.getTempFileAPI()
                .getTempFile(List.of(admin().getUserId()), lastTempFileId);

        assertTrue(leftBehind.isEmpty() || !leftBehind.get().file.exists(),
                "a file the run never reached still had content staged for it, and a cancelled "
                        + "run is a terminal state like any other");
    }

    /**
     * Method to test: {@link BulkUploadProcessor#getResultMetadata} — the duplicate flag
     * <p>
     * Given scenario: A run the submission recognised as a repeat of one that already succeeded.
     * <p>
     * Expected result: The outcome carries {@code duplicateSubmission: true} (contract §3,
     * FR-040a). <b>The counts alone cannot express this</b>: a duplicate collides on every file, so
     * it is numerically identical to a batch whose files genuinely all collided. Only the flag
     * separates "you already uploaded these" from "none of these could be created", and those need
     * opposite reactions from the author.
     */
    @Test
    public void test_outcome_flagsARunThatRepeatsOneAlreadyDone() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job built = jobFor(folder, 1);
        final Map<String, Object> parameters = new HashMap<>(built.parameters());
        parameters.put("duplicateOfJobId", "an-earlier-run");
        final Job job = Job.builder().from(built).parameters(parameters).build();

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);

        assertEquals(Boolean.TRUE, processor.getResultMetadata(job).get("duplicateSubmission"),
                "a repeat must be reportable as one, or the client shows 'everything failed' for "
                        + "files the author already has");
    }

    /**
     * Method to test: {@link BulkUploadProcessor#getResultMetadata} — not over-flagging
     * <p>
     * Given scenario: An ordinary first-time run.
     * <p>
     * Expected result: {@code duplicateSubmission: false}. A batch wrongly reported as a repeat
     * would send the author looking for files that were never created.
     */
    @Test
    public void test_outcome_doesNotFlagAnOrdinaryRun() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 1);

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);

        assertEquals(Boolean.FALSE, processor.getResultMetadata(job).get("duplicateSubmission"));
    }

    /**
     * Method to test: {@link BulkUploadProcessor#process} — progress while the run is in flight
     * <p>
     * Given scenario: A batch large enough that progress must move more than once.
     * <p>
     * Expected result: The tracker reports a rising fraction and finishes at 1.0 (FR-024). Progress
     * is the difference between a batch an author can leave and one they have to guess about —
     * without it the interface can only say "working", which for fifty files is indistinguishable
     * from being stuck.
     */
    @Test
    public void test_run_reportsProgressAsFilesComplete() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 5);

        final DefaultProgressTracker tracker =
                (DefaultProgressTracker) job.progressTracker().orElseThrow();

        new BulkUploadProcessor().process(job);

        assertEquals(1.0f, tracker.progress(), 0.001f,
                "a finished run reports itself finished; a tracker stuck below 1.0 leaves the "
                        + "interface showing an in-flight batch forever");
    }

    /**
     * Method to test: {@link BulkUploadProcessor#cancel} and the outcome it leaves
     * <p>
     * Given scenario: A run cancelled before it reached every file.
     * <p>
     * Expected result: Files already created stay (FR-026), and the outcome records the untouched
     * ones as <b>SKIPPED rather than FAILED</b> (FR-028). That distinction is the whole point:
     * skipped files were never tried, and reporting them as failures tells the author their files
     * were rejected when they simply stopped the run themselves. Cancellation also takes effect
     * between files, never mid-file, so nothing is left half-created (FR-027).
     */
    @Test
    public void test_cancel_keepsWhatWasCreatedAndMarksTheRestSkippedNotFailed() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 4);

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.cancel(job);
        processor.process(job);

        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(0, ((Number) outcome.get("failedCount")).intValue(),
                "a cancelled run's untouched files are not failures — they were never attempted.\n"
                        + describe(outcome));
        assertTrue(((Number) outcome.get("skippedCount")).intValue() > 0,
                "and they are recorded as skipped, which is a distinct outcome");
    }

    /**
     * Method to test: the bulk-upload processor, at the configured maximum
     * <p>
     * Given scenario: A full batch — 100 files of 1 MB, {@code CONTENT_BULK_UPLOAD_MAX_FILES} —
     * run on a single node.
     * <p>
     * Expected result: It finishes inside 120 seconds with all 100 created (SC-003).
     * <p>
     * <b>Read the ceiling as an order of magnitude, not as a performance target.</b> 120s for 100
     * files is roughly 1.2s per file, which is several times slower than this run is on any
     * machine that would execute it — the headroom is deliberate, so that CI jitter, a cold cache
     * or a loaded box never turn this red. What it is written to catch is the regression that
     * matters: **a per-file search-index wait creeping back in**. That is what FR-008 removed and
     * what SC-003 names explicitly, and it does not fail anything else in the suite — every other
     * test would still pass, just slowly. It changes throughput by an order of magnitude, so it
     * lands well outside this bound while ordinary variance stays well inside it.
     * <p>
     * The criterion is deliberately absolute rather than measured against today's single-file
     * upload: that path waits for each file to become searchable, which is the very pathology
     * being removed, so comparing against it would make SC-003 true by construction.
     */
    @Test
    public void test_run_sustainsThroughputAtTheConfiguredMaximum() throws Exception {
        final int fileCount = 100;
        final long ceilingMillis = 120_000L;

        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        // Staged outside the measurement: the submission is a separate leg with its own bound
        // (FR-013b), and SC-003 is about the run.
        final Job job = jobFor(folder, fileCount, 1024 * 1024);

        final BulkUploadProcessor processor = new BulkUploadProcessor();

        final long startedAt = System.currentTimeMillis();
        processor.process(job);
        final long elapsed = System.currentTimeMillis() - startedAt;

        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(fileCount, ((Number) outcome.get("successCount")).intValue(),
                "a timing measurement over a run that did not do the work says nothing.\n"
                        + "Recorded per-item results: " + describe(outcome));

        assertTrue(elapsed < ceilingMillis, String.format(
                "a full batch of %d files took %dms, past the %dms ceiling. The headroom here is "
                        + "generous enough that jitter does not explain this: look first for a "
                        + "per-file search-index wait, which is what SC-003 guards against and "
                        + "what an IndexPolicy other than DEFER would reintroduce",
                fileCount, elapsed, ceilingMillis));
    }


    /**
     * Method to test: the bulk-upload processor
     * <p>
     * Given scenario: A batch of valid files is run.
     * <p>
     * Expected result: Every file is left as a <b>draft</b> — working, not live.
     * <p>
     * <b>This needs its own test because the obvious assertion does not catch it.</b> Publishing a
     * contentlet leaves a working version behind as well, so
     * {@code getWorkingContent(...).size() == n} passes whether the run published or not. Only
     * asking each contentlet whether it is live tells the two apart, which is why an earlier
     * version of this feature fired PUBLISH for weeks with a green suite.
     * <p>
     * <b>Why draft is the right answer</b> (FR-006): the single-file endpoint checks an asset in as
     * working unless the caller explicitly asks for live. A batch that published would mean the
     * same file put an author's unreviewed content on the live site when uploaded with others and
     * not when uploaded alone — an equivalence failure whose consequence is publishing something
     * nobody approved.
     */
    @Test
    public void test_run_leavesEveryFileAsADraftRatherThanPublishingIt() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 3);

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);

        final Map<String, Object> outcome = processor.getResultMetadata(job);
        assertEquals(3, ((Number) outcome.get("successCount")).intValue(),
                "the run has to have created something for this to mean anything.\n"
                        + "Recorded per-item results: " + describe(outcome));

        final List<Contentlet> created =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);
        assertEquals(3, created.size());

        for (final Contentlet contentlet : created) {
            assertFalse(contentlet.isLive(), String.format(
                    "a bulk upload must leave files as drafts, exactly as uploading the same file "
                            + "on its own does; '%s' was published instead",
                    contentlet.getTitle()));
        }
    }

}
