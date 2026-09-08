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
                this.messageKey = String.valueOf(args[1]);
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
}
