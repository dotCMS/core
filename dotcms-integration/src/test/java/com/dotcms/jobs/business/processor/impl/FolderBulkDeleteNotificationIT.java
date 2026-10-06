package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteCompletionListener;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotcms.util.I18NMessage;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.liferay.portal.model.User;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for telling the author their bulk folder delete finished (#37063, FR-031 …
 * FR-035, US4).
 * <p>
 * <b>Follows {@code BulkUploadNotificationIT} one-for-one</b> for the synthetic-job tests (T045,
 * T046, T048): a {@link Job} is built directly with the outcome the queue would have produced, and
 * {@link FolderBulkDeleteCompletionListener#notify} is called against a captured
 * {@link NotificationAPI} proxy — this is what the decision the listener makes is entirely about,
 * and reading a persisted notification row back instead would make the assertion depend on
 * notification persistence and the test's transaction boundary, a different subject.
 * <p>
 * T047 is the exception: SC-006/US4 scenario 3 is specifically about the outcome surviving a
 * "walked away and came back" gap, so it drives the <b>real</b> queue and re-fetches the job fresh
 * — the closest an in-process test gets to disconnecting and reconnecting.
 */
@EnableWeld
public class FolderBulkDeleteNotificationIT extends Junit5WeldBaseTest {

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Captures what the listener decided to tell the author — see
     * {@code BulkUploadNotificationIT.CapturingNotifications} for why capturing, not reading a
     * persisted row back, is the right level for these tests.
     */
    private static class CapturingNotifications implements InvocationHandler {

        private NotificationLevel level;
        private String messageKey;
        private String userId;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            if ("generateNotification".equals(method.getName()) && args != null
                    && args.length >= 8) {
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
        new FolderBulkDeleteCompletionListener(APILocator.getSystemEventsAPI(), captured.asApi(),
                APILocator.getUserAPI())
                .notify(new JobCompletedEvent(job, LocalDateTime.now()));
        return captured;
    }

    /** A finished job carrying an outcome, as the queue would hand the listener. */
    private Job finishedJob(final String userId, final JobState state,
            final Map<String, Object> result) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName(FolderBulkDeleteHelper.QUEUE_NAME)
                .state(state)
                .parameters(parameters)
                .result(JobResult.builder().metadata(result).build())
                .build();
    }

    private static Map<String, Object> outcome(final int success, final int failed,
            final int skipped) {
        final Map<String, Object> result = new HashMap<>();
        result.put("total", success + failed + skipped);
        result.put("processed", success + failed + skipped);
        result.put("successCount", success);
        result.put("failedCount", failed);
        result.put("skippedCount", skipped);
        result.put("results", List.of(
                Map.of("key", "//default/deleted", "status", "SUCCESS"),
                Map.of("key", "//default/refused", "status", "FAILED",
                        "reason", "PERMISSION_DENIED")));
        return result;
    }

    /**
     * Method to test: {@link FolderBulkDeleteCompletionListener}
     * <p>
     * Given scenario: A bulk folder delete finishes.
     * <p>
     * Expected result: The submitter — and only the submitter, never a bystander — is notified,
     * both on the pushed channel (its own distinguishable event type, FR-035) and the durable one
     * (FR-031, FR-032). An author who deleted forty folders and closed the tab must still be able
     * to find out whether all forty went; that is what the durable half exists to answer.
     */
    @Test
    public void test_completion_notifiesOnlyTheSubmitter_pushedAndDurable() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final User bystander = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(3, 0, 0));

        final CapturingNotifications captured = listenTo(job);

        assertNotNull(captured.messageKey,
                "an author who walked away must be able to learn the outcome afterwards");
        assertEquals(author.getUserId(), captured.userId,
                "a run is addressed to whoever submitted it, not broadcast");
        assertFalse(bystander.getUserId().equals(captured.userId));
        assertEquals(SystemEventType.BULK_FOLDER_DELETE_COMPLETED,
                new FolderBulkDeleteCompletionListener().eventType(),
                "pushed as its own event type, so a client can tell a folder delete from any "
                        + "other background work");
    }

    /**
     * Method to test: {@link FolderBulkDeleteCompletionListener}
     * <p>
     * Given scenario: A clean run, a partial one, and a cancelled one each finish.
     * <p>
     * Expected result: Each reads differently (FR-033), and the cancelled one is worded as an
     * outcome the author chose, never as a fault — even though its counts alone would also satisfy
     * the partial branch.
     */
    @Test
    public void test_completion_wordingDiffersForCleanPartialAndCancelledRuns() throws Exception {
        final User author = new UserDataGen().nextPersisted();

        final CapturingNotifications clean =
                listenTo(finishedJob(author.getUserId(), JobState.SUCCESS, outcome(3, 0, 0)));
        assertEquals("notification.bulkfolderdelete.success", clean.messageKey,
                "nothing failed and nothing was skipped, so nothing should suggest otherwise");
        assertEquals(NotificationLevel.INFO, clean.level);

        final CapturingNotifications partial =
                listenTo(finishedJob(author.getUserId(), JobState.SUCCESS, outcome(2, 1, 0)));
        assertEquals("notification.bulkfolderdelete.partial", partial.messageKey,
                "some deleted and some failed is its own outcome, not a success and not a total "
                        + "failure");
        assertEquals(NotificationLevel.WARNING, partial.level);

        final Map<String, Object> cancelledOutcome = outcome(2, 0, 3);
        final CapturingNotifications cancelled = listenTo(
                finishedJob(author.getUserId(), JobState.CANCELED, cancelledOutcome));
        assertEquals("notification.bulkfolderdelete.cancelled", cancelled.messageKey,
                "these counts also satisfy the partial branch; the state has to win, and a run the "
                        + "author cancelled must not read as a fault");
        assertFalse(NotificationLevel.ERROR.equals(cancelled.level));
    }

    /**
     * Method to test: {@link FolderBulkDeleteCompletionListener}, driven through the real queue
     * <p>
     * Given scenario: A submission of real folders runs to a terminal state, and the outcome is
     * then requested through a fresh {@code getJob} call — the closest an in-process test gets to
     * an author disconnecting and reconnecting later.
     * <p>
     * Expected result: The counts and the per-path records are still readable (SC-006, US4
     * scenario 3) — proving the same durability the outcome envelope already gives US1-US3 also
     * answers this story, without anything extra to build for it.
     */
    @Test
    public void test_completion_outcomeStillReadableAfterReconnecting() throws Exception {

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        final User admin = APILocator.systemUser();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder first = new FolderDataGen().site(site).nextPersisted();
        final Folder second = new FolderDataGen().site(site).nextPersisted();

        final List<Map<String, Object>> pathParams = new ArrayList<>();
        for (final Folder folder : List.of(first, second)) {
            final Map<String, Object> p = new HashMap<>();
            p.put("path", String.format("//%s/%s/", site.getHostname(), folder.getName()));
            pathParams.add(p);
        }
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", admin.getUserId());
        parameters.put("paths", pathParams);

        final String jobId =
                jobQueueManagerAPI.createJob(FolderBulkDeleteHelper.QUEUE_NAME, parameters);

        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.SUCCESS);

        // A fresh read, not the reference above — simulating the author reconnecting rather than
        // having kept a live handle on the run the whole time.
        final Job reconnected = jobQueueManagerAPI.getJob(jobId);
        final Map<String, Object> metadata =
                reconnected.result().orElseThrow().metadata().orElseThrow();

        assertEquals(2, ((Number) metadata.get("total")).intValue());
        assertEquals(2, ((Number) metadata.get("successCount")).intValue());

        @SuppressWarnings("unchecked")
        final List<Object> rawResults = (List<Object>) metadata.get("results");
        final ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());
        final List<BatchItemResult> results = rawResults.stream()
                .map(raw -> mapper.convertValue(raw, BatchItemResult.class))
                .collect(Collectors.toList());

        assertEquals(2, results.size(),
                "the per-path records must still be readable after reconnecting, not only the "
                        + "counts");
        assertTrue(results.stream().allMatch(r -> r.status().name().equals("SUCCESS")));
    }

    /**
     * Method to test: {@link FolderBulkDeleteCompletionListener}
     * <p>
     * Given scenario: Both the pushed channel and the durable channel fail to deliver.
     * <p>
     * Expected result: {@code notify} does not propagate the failure (FR-034) — the run's recorded
     * outcome, already settled before the listener ever runs, is untouched by a broken doorbell.
     */
    @Test
    public void test_completion_deliveryFailureDoesNotChangeTheRecordedOutcome() throws Exception {
        final User author = new UserDataGen().nextPersisted();
        final Job job = finishedJob(author.getUserId(), JobState.SUCCESS, outcome(3, 0, 0));

        final SystemEventsAPI throwingEvents = (SystemEventsAPI) Proxy.newProxyInstance(
                SystemEventsAPI.class.getClassLoader(), new Class<?>[]{SystemEventsAPI.class},
                (proxy, method, args) -> {
                    throw new RuntimeException("push channel is down");
                });
        final NotificationAPI throwingNotifications = (NotificationAPI) Proxy.newProxyInstance(
                NotificationAPI.class.getClassLoader(), new Class<?>[]{NotificationAPI.class},
                (proxy, method, args) -> {
                    throw new RuntimeException("durable channel is down");
                });

        final FolderBulkDeleteCompletionListener listener = new FolderBulkDeleteCompletionListener(
                throwingEvents, throwingNotifications, APILocator.getUserAPI());

        assertDoesNotThrow(() -> listener.notify(new JobCompletedEvent(job, LocalDateTime.now())),
                "a delivery failure on either channel is best-effort and must never propagate out "
                        + "of the listener");
        assertEquals(JobState.SUCCESS, job.state(),
                "the run's own recorded outcome is unaffected by a notification failure — it was "
                        + "already settled before the listener ran");
    }
}
