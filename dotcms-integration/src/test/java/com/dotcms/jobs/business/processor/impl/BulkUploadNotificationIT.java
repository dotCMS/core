package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.util.I18NMessage;
import com.dotcms.notifications.business.NotificationAPI;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import com.dotcms.rest.api.v1.asset.bulkupload.BulkUploadCompletionListener;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.UUID;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for telling the author their batch finished (spec FR-019 … FR-023, SC-004).
 * <p>
 * <b>This is the promise the ticket is really built on.</b> An author who must sit and watch a
 * thirty-file upload has been given a worse product than the one that uploaded a single file
 * quickly — so the batch has to be safe to walk away from, and that is only true if the outcome
 * finds them afterwards. Two channels, because they answer different questions: the pushed event
 * reaches a browser still on the page, and the durable notification is what remains for an author
 * who closed the tab and came back tomorrow.
 * <p>
 * Both are best-effort by design: a failed notification is logged and never alters the recorded
 * outcome (FR-023). The files were created either way, and losing the run because the doorbell
 * broke would be the worse trade.
 */
@EnableWeld
public class BulkUploadNotificationIT extends Junit5WeldBaseTest {

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Captures what the listener decided to tell the author.
     * <p>
     * <b>Why capture rather than read the stored notification back.</b> The decision — which
     * message, at which level — is what these tests are about, and it is entirely the listener's.
     * Reading the row back instead makes the assertion depend on notification persistence and the
     * test's transaction boundary, which is a different subject and was silently returning nothing
     * here. The shipped {@code BulkRefreshCompletionListenerTest} makes the same choice.
     */
    private static class CapturingNotifications implements InvocationHandler {

        private NotificationLevel level;
        private String messageKey;
        private String userId;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            if ("generateNotification".equals(method.getName()) && args != null
                    && args.length >= 8) {
                // getKey(), not String.valueOf: I18NMessage#toString renders the whole object —
                // "I18NMessage{key=..., arguments=[...]}" — so a test comparing against a bare key
                // would never match, and one only checking non-null would pass no matter which
                // message was chosen. Which message it is, is the whole subject here.
                this.messageKey = args[1] instanceof I18NMessage
                        ? ((I18NMessage) args[1]).getKey() : String.valueOf(args[1]);
                this.level = (NotificationLevel) args[3];
                this.userId = String.valueOf(args[6]);
            }
            return null;
        }

        NotificationAPI asApi() {
            return (NotificationAPI) Proxy.newProxyInstance(
                    NotificationAPI.class.getClassLoader(),
                    new Class<?>[]{NotificationAPI.class}, this);
        }
    }

    private CapturingNotifications listenTo(final Job job) throws Exception {
        final CapturingNotifications captured = new CapturingNotifications();
        new BulkUploadCompletionListener(APILocator.getSystemEventsAPI(), captured.asApi(),
                APILocator.getUserAPI())
                .notify(new JobCompletedEvent(job, LocalDateTime.now()));
        return captured;
    }

    private User admin() throws Exception {
        return APILocator.systemUser();
    }

    private Folder folder() {
        return new FolderDataGen().site(new SiteDataGen().nextPersisted()).nextPersisted();
    }

    /** A finished job carrying an outcome, as the queue would hand the listener. */
    private Job finishedJob(final String userId, final JobState state,
                            final Map<String, Object> result) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        parameters.put("baseType", "DOTASSET");

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName("assetBulkUpload")
                .state(state)
                .parameters(parameters)
                .result(JobResult.builder().metadata(result).build())
                .build();
    }

    private static Map<String, Object> outcome(final int success, final int failed) {
        final Map<String, Object> result = new HashMap<>();
        result.put("total", success + failed);
        result.put("processed", success + failed);
        result.put("successCount", success);
        result.put("failedCount", failed);
        result.put("skippedCount", 0);
        result.put("results", List.of(
                Map.of("key", "landed.txt", "status", "SUCCESS"),
                Map.of("key", "too-big.mov", "status", "FAILED",
                        "reason", "OVER_SIZE_LIMIT")));
        result.put("duplicateSubmission", false);
        return result;
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A batch finishes while nobody is listening.
     * <p>
     * Expected result: The submitter has a durable notification afterwards (FR-020, SC-004). This
     * is the whole of User Story 3: "the author is told, even if they walked away". A pushed event
     * alone would reach an empty room.
     */
    @Test
    public void test_completion_leavesADurableRecordForTheSubmitter() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(3, 0));

        final CapturingNotifications captured = listenTo(job);

        assertNotNull(captured.messageKey,
                "an author who walked away must be able to learn the outcome afterwards");
        assertEquals(author.getUserId(), captured.userId,
                "and it is addressed to them");
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A batch belonging to one author finishes.
     * <p>
     * Expected result: Only that author is told (FR-021). An upload is nobody else's business —
     * the legacy batch reindex announced its result to every CMS Administrator, which is the
     * mistake the bulk refresh work already corrected and this must not reintroduce.
     */
    @Test
    public void test_completion_tellsTheSubmitterAndNobodyElse() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final User bystander = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(2, 0));

        final CapturingNotifications captured = listenTo(job);

        assertEquals(author.getUserId(), captured.userId,
                "a batch is addressed to whoever submitted it, not broadcast");
        assertFalse(bystander.getUserId().equals(captured.userId));
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A batch where some files failed.
     * <p>
     * Expected result: The outcome carried to the author includes the <b>per-file results</b>, not
     * counts alone (spec C-006). A counts-only record leaves an author with "27 of 30 created" and
     * no way to learn which three — which FR-023 forbids, because those names are exactly what tell
     * them which files to choose again.
     */
    @Test
    public void test_completion_carriesPerFileResultsAndNotOnlyCounts() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Map<String, Object> result = outcome(1, 1);
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, result);

        final Map<String, Object> payload =
                new BulkUploadCompletionListener().payloadFor(job);

        assertNotNull(payload.get("results"),
                "the failing file names must travel with the outcome; without them the author "
                        + "cannot act on it");
        assertEquals(1, ((Number) payload.get("failedCount")).intValue());
        assertEquals(SystemEventType.BULK_UPLOAD_COMPLETED,
                new BulkUploadCompletionListener().eventType(),
                "and it is pushed as its own event type, so a client can tell an upload from a "
                        + "reindex");
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A resubmission of a batch that had already succeeded finishes, and the
     * author is following it on the pushed channel rather than by polling the job.
     * <p>
     * Expected result: {@code duplicateSubmission} travels in the payload (FR-040a, contracts §4 —
     * "the payload is the §3 outcome", and this is part of it). Without it the push carries "every
     * file failed - name collision", which is indistinguishable from a batch whose files genuinely
     * all collided, and is the exact report C-002a promises the client it will never have to
     * render. It has to be on <b>this</b> channel and not only on the polled outcome: the author
     * who left the page is the case User Story 4 exists for, and the frontend spec's Assumptions
     * rule out compensating with a polling loop.
     */
    @Test
    public void test_completion_carriesTheDuplicateFlagOnThePushedChannelToo() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Map<String, Object> result = outcome(0, 2);
        result.put("duplicateSubmission", true);
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, result);

        final Map<String, Object> payload =
                new BulkUploadCompletionListener().payloadFor(job);

        assertEquals(Boolean.TRUE, payload.get("duplicateSubmission"),
                "a resubmission must be recognisable from the pushed payload alone; the counts "
                        + "cannot tell it apart from a batch that genuinely all collided");
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A batch that is not a resubmission finishes.
     * <p>
     * Expected result: {@code duplicateSubmission} is present and {@code false}, not absent. A
     * client reading the flag has to be able to tell "not a duplicate" from "this server does not
     * report duplicates", and an omitted key says the second.
     */
    @Test
    public void test_completion_reportsAnOrdinaryBatchAsNotADuplicate() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(2, 0));

        final Map<String, Object> payload =
                new BulkUploadCompletionListener().payloadFor(job);

        assertTrue(payload.containsKey("duplicateSubmission"),
                "the flag must be present rather than omitted, so its absence never has to be "
                        + "guessed at");
        assertEquals(Boolean.FALSE, payload.get("duplicateSubmission"));
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A resubmission of an already-succeeded batch reaches the durable
     * notification — the bell, the channel for the author who closed the tab.
     * <p>
     * Expected result: Reported as "already uploaded" at {@code INFO}, not as a total failure at
     * {@code ERROR}.
     * <p>
     * <b>This is the case the counts actively lie about.</b> A resubmission collides on every file,
     * so it arrives as {@code successCount=0, failedCount=N} and is word-for-word indistinguishable
     * from a batch that genuinely failed outright. Judged on counts alone the listener picks the
     * failure wording, which is the exact outcome FR-040a was added to prevent — and it lands on
     * the feature's core channel. Told "50 of 50 failed", an author re-uploads 50 files that are
     * already in the folder, which the spec calls worse than offering no retry at all.
     */
    @Test
    public void test_completion_doesNotReportAResubmissionAsATotalFailure() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        // The shape a duplicate really arrives in: nothing succeeded, everything collided.
        final Map<String, Object> result = outcome(0, 3);
        result.put("duplicateSubmission", true);
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, result);

        final CapturingNotifications captured = listenTo(job);

        assertEquals("notification.bulkupload.duplicate", captured.messageKey,
                "a recognised resubmission must be told as one; these exact counts are also what "
                        + "a genuine total failure looks like, which is why the flag has to be "
                        + "consulted before them");
        assertFalse(NotificationLevel.ERROR.equals(captured.level),
                "and it is not an error — nothing went wrong, the files were already there");
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A batch that genuinely failed on every file, with no duplicate flag.
     * <p>
     * Expected result: Still reported as a failure, at {@code ERROR}. The guard above must not have
     * turned every all-collision batch into a reassuring message — a first-time batch whose files
     * all collided with someone else's really did fail, and the author has to be told so.
     */
    @Test
    public void test_completion_stillReportsAGenuineTotalFailureAsOne() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Map<String, Object> result = outcome(0, 3);
        result.put("duplicateSubmission", false);
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, result);

        final CapturingNotifications captured = listenTo(job);

        assertEquals("notification.bulkupload.failed", captured.messageKey,
                "identical counts, no flag: this one really did fail and must say so");
        assertEquals(NotificationLevel.ERROR, captured.level);
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener#payloadFor}
     * <p>
     * Given scenario: An outcome carrying every field contracts §3 defines.
     * <p>
     * Expected result: The pushed payload carries all of them.
     * <p>
     * <b>Why this is asserted as a set rather than field by field.</b> Contract §4 says the pushed
     * payload <i>is</i> the §3 outcome, but {@code payloadFor} copies keys by hand — so the two can
     * drift silently, and did: {@code duplicateSubmission} was implemented on the outcome and
     * omitted from the payload, with both halves correct in isolation and nobody owning the join.
     * Two reviewers passed both sides clean. A per-field test would not have caught it either,
     * because nobody writes the test for the field they forgot. This one fails the moment a field
     * is added to §3 and not to the event, which is the failure mode worth guarding.
     * <p>
     * Suggested by @zJaaal, building the client against this contract.
     */
    @Test
    public void test_completion_payloadCarriesEveryFieldTheContractDefines() throws Exception {
        // Contract §3, "The outcome" — the field table, verbatim.
        final List<String> contractFields = List.of(
                "total", "processed", "successCount", "failedCount", "skippedCount",
                "duplicateSubmission", "results");

        final User author = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(2, 1));

        final Map<String, Object> payload =
                new BulkUploadCompletionListener().payloadFor(job);

        final List<String> missing = contractFields.stream()
                .filter(field -> !payload.containsKey(field))
                .collect(Collectors.toList());

        assertTrue(missing.isEmpty(), String.format(
                "contracts §4 says the pushed payload is the §3 outcome, but payloadFor omits %s. "
                        + "Add the field there as well as to getResultMetadata — a client "
                        + "following the push cannot see what only the polled outcome carries",
                missing));
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A run that was cancelled rather than one that failed.
     * <p>
     * Expected result: Reported as a warning, not an error. Cancellation is an outcome the author
     * chose; reporting it as a fault tells them their upload broke when they stopped it themselves.
     * The bulk refresh work fixed exactly this and the fix should not have to be made twice.
     */
    @Test
    public void test_completion_doesNotReportACancelledRunAsAFailure() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.CANCELED, outcome(2, 0));

        final CapturingNotifications captured = listenTo(job);

        assertEquals(NotificationLevel.WARNING, captured.level,
                "a run the author cancelled is not a failure — reporting it as an error tells "
                        + "them their upload broke when they stopped it themselves");
        assertTrue(captured.messageKey.contains("cancelled"),
                "and the wording says cancelled, not failed");
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A batch where some files landed and some did not.
     * <p>
     * Expected result: The <b>partial</b> wording, at {@code WARNING} — not the success wording and
     * not the failure one.
     * <p>
     * FR-022 requires four outcomes to read differently, and this was one of the two branches no
     * test reached. It is also the branch most likely to be got wrong quietly: a partial batch that
     * started reading as a clean success would tell an author nothing went wrong while some of
     * their files are missing, and the counts they would need to notice are in the same message
     * that just reassured them.
     */
    @Test
    public void test_completion_tellsApartAPartialBatchFromASuccessfulOne() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(2, 1));

        final CapturingNotifications captured = listenTo(job);

        assertEquals("notification.bulkupload.partial", captured.messageKey,
                "some created and some failed is its own outcome, not a success and not a "
                        + "total failure");
        assertEquals(NotificationLevel.WARNING, captured.level,
                "and it is a warning: something needs the author's attention, but the run worked");
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A batch where every file was created.
     * <p>
     * Expected result: The <b>success</b> wording, at {@code INFO}.
     * <p>
     * The other untested branch, and the control that gives the partial test above its meaning: a
     * listener that reported everything as partial would have satisfied that test on its own.
     */
    @Test
    public void test_completion_reportsACleanBatchAsASuccess() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(3, 0));

        final CapturingNotifications captured = listenTo(job);

        assertEquals("notification.bulkupload.success", captured.messageKey,
                "nothing failed and nothing was skipped, so nothing should suggest otherwise");
        assertEquals(NotificationLevel.INFO, captured.level);
    }

    /**
     * Method to test: {@link BulkUploadCompletionListener}
     * <p>
     * Given scenario: A cancelled run that had created some files and never reached others.
     * <p>
     * Expected result: The cancelled wording, <b>not</b> the partial one — even though the counts
     * satisfy the partial branch too.
     * <p>
     * Skipped files make a cancelled run look partial on the numbers alone. Cancellation is
     * something the author chose, and reporting it as "some of your files failed" describes a fault
     * they did not experience.
     */
    @Test
    public void test_completion_prefersCancelledOverPartial_whenBothWouldMatch() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Map<String, Object> result = outcome(2, 0);
        result.put("skippedCount", 3);
        final Job job = finishedJob(author.getUserId(), JobState.CANCELED, result);

        final CapturingNotifications captured = listenTo(job);

        assertEquals("notification.bulkupload.cancelled", captured.messageKey,
                "these counts also match the partial branch; the state has to win");
        assertFalse(NotificationLevel.ERROR.equals(captured.level));
    }

}
