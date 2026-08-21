package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tells the user who submitted a bulk refresh that it finished.
 *
 * <p>The endpoint answers {@code 202} long before the work is done, so something has to close the loop.
 * This listener does it by push rather than by the client asking: on a terminal job it emits a
 * {@link SystemEventType#BULK_REFRESH_COMPLETED} system event carrying the run's counters, which reaches
 * the browser over the websocket the admin UI already holds open, and it records a notification so the
 * outcome survives navigating away or closing the tab.
 *
 * <p><b>Why {@link JobCompletedEvent} and not the processor itself.</b> That event fires for every
 * terminal state — success, cancellation, and permanent failure
 * ({@code JobQueueManagerAPIImpl} marks the last of these completed too) — so the failure path needs no
 * special handling in the processor. {@code JobFailedEvent} is deliberately not used: it signals a
 * <i>retryable</i> failure, where the job goes back on the queue and is not finished at all.
 *
 * <p><b>Why we push our own event.</b> Job events never reach a browser: they travel as
 * {@code CLUSTER_WIDE_EVENT}, which is explicitly excluded from the websocket. Subscribing locally and
 * pushing a purpose-built event also keeps the payload to plain counters, which crosses nodes safely,
 * rather than depending on the job event classes deserializing on another node.
 *
 * <p><b>Registered at startup</b> by {@code LocalSystemEventSubscribersInitializer}, deliberately not as
 * a CDI bean subscribing to itself in {@code @PostConstruct}. CDI beans are lazy: nothing injects this
 * class, so it would never have been constructed, the subscription would never have happened, and a
 * finished reindex would simply never have been reported — with every test still green, because the unit
 * tests construct it directly and the integration tests assert the job's state rather than the event.
 *
 * @author dotCMS
 */
public class BulkRefreshCompletionListener implements EventSubscriber<JobCompletedEvent> {

    static final String EVENT_TOTAL = "total";
    static final String EVENT_SUCCESS_COUNT = "successCount";
    static final String EVENT_FAILED_COUNT = "failedCount";
    static final String EVENT_SKIPPED_COUNT = "skippedCount";
    static final String EVENT_VERSIONS_INDEXED = "versionsIndexed";
    static final String EVENT_STATE = "state";

    private static final String NOTIFICATION_TITLE_KEY = "notification.bulkrefresh.title";
    private static final String NOTIFICATION_SUCCESS_KEY = "notification.bulkrefresh.success";
    private static final String NOTIFICATION_PARTIAL_KEY = "notification.bulkrefresh.partial";
    private static final String NOTIFICATION_FAILED_KEY = "notification.bulkrefresh.failed";

    private final SystemEventsAPI systemEventsAPI;
    private final NotificationAPI notificationAPI;
    private final UserAPI userAPI;

    public BulkRefreshCompletionListener() {
        this(APILocator.getSystemEventsAPI(), APILocator.getNotificationAPI(),
                APILocator.getUserAPI());
    }

    @VisibleForTesting
    BulkRefreshCompletionListener(final SystemEventsAPI systemEventsAPI,
            final NotificationAPI notificationAPI, final UserAPI userAPI) {
        this.systemEventsAPI = systemEventsAPI;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
    }

    /**
     * Reports a finished bulk refresh, and ignores every other queue's jobs.
     */
    @Override
    public void notify(final JobCompletedEvent event) {

        final Job job = null == event ? null : event.getJob();
        if (null == job
                || !BulkRefreshHelper.BULK_REFRESH_QUEUE_NAME.equals(job.queueName())) {
            return;
        }

        final String userId = submitter(job);
        if (!UtilMethods.isSet(userId)) {
            // Without a submitter there is nobody to tell. Worth a line in the log rather than a
            // silent return, because it means the job was created without its user parameter.
            Logger.warn(this, String.format(
                    "Bulk refresh job [%s] finished with no submitting user recorded; "
                            + "no notification sent", job.id()));
            return;
        }

        final Map<String, Object> counters = counters(job);

        // Both channels are best-effort: a failed notification must not bring down the job queue's
        // event dispatch, and the run itself has already succeeded by this point.
        Try.run(() -> this.systemEventsAPI.pushAsync(
                        SystemEventType.BULK_REFRESH_COMPLETED,
                        new Payload(counters, Visibility.USER, userId)))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to push the bulk refresh completion event for job [%s]", job.id()), e));

        Try.run(() -> notify(job, counters, userId))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to record the bulk refresh notification for job [%s]", job.id()), e));
    }

    /**
     * Records the durable notification, worded on what actually happened.
     * <p>
     * Addressed to the submitter rather than to the CMS Administrator role, and gated on the outcome —
     * the legacy batch reindex did neither, announcing success to every administrator even when every
     * single item had failed.
     */
    private void notify(final Job job, final Map<String, Object> counters, final String userId)
            throws Exception {

        final int failed = intValue(counters, EVENT_FAILED_COUNT);
        final int skipped = intValue(counters, EVENT_SKIPPED_COUNT);
        final int succeeded = intValue(counters, EVENT_SUCCESS_COUNT);

        final String messageKey;
        final NotificationLevel level;
        if (JobState.SUCCESS != job.state() || (0 == succeeded && failed > 0)) {
            messageKey = NOTIFICATION_FAILED_KEY;
            level = NotificationLevel.ERROR;
        } else if (failed > 0 || skipped > 0) {
            messageKey = NOTIFICATION_PARTIAL_KEY;
            level = NotificationLevel.WARNING;
        } else {
            messageKey = NOTIFICATION_SUCCESS_KEY;
            level = NotificationLevel.INFO;
        }

        // The I18NMessage overload so the counts travel as arguments and the text is resolved in the
        // recipient's own locale. Legacy localized with the *system* user's locale, which meant the
        // message could arrive in a language the reader does not use.
        this.notificationAPI.generateNotification(
                new I18NMessage(NOTIFICATION_TITLE_KEY),
                new I18NMessage(messageKey, null, succeeded, failed, skipped),
                null,
                level,
                NotificationType.GENERIC,
                Visibility.USER,
                userId,
                userId,
                this.userAPI.loadUserById(userId).getLocale()
        );
    }

    /**
     * The run's counters, taken from the persisted job result.
     * <p>
     * Empty when the job carried none — a client is expected to treat that as a failure rather than as a
     * clean run over nothing, which is what all-zero counters would look like.
     */
    private Map<String, Object> counters(final Job job) {

        final Optional<Map<String, Object>> metadata =
                job.result().flatMap(JobResult::metadata);

        final Map<String, Object> counters = new HashMap<>();
        counters.put(EVENT_STATE, job.state());
        metadata.ifPresent(found -> {
            counters.put(EVENT_TOTAL, found.get(EVENT_TOTAL));
            counters.put(EVENT_SUCCESS_COUNT, found.get(EVENT_SUCCESS_COUNT));
            counters.put(EVENT_FAILED_COUNT, found.get(EVENT_FAILED_COUNT));
            counters.put(EVENT_SKIPPED_COUNT, found.get(EVENT_SKIPPED_COUNT));
            counters.put(EVENT_VERSIONS_INDEXED, found.get(EVENT_VERSIONS_INDEXED));
        });

        return counters;
    }

    private static String submitter(final Job job) {
        final Object userId = job.parameters()
                .get(BulkRefreshContentletsProcessor.PARAM_USER_ID);

        return null == userId ? null : String.valueOf(userId);
    }

    private static int intValue(final Map<String, Object> counters, final String key) {
        final Object value = counters.get(key);

        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
