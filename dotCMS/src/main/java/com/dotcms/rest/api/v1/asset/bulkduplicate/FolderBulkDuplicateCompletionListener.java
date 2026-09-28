package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tells the author their bulk folder duplication finished, on any terminal state (#37062, US5).
 * <p>
 * Mirrors {@code FolderBulkDeleteCompletionListener}: pushes
 * {@code BULK_FOLDER_DUPLICATE_COMPLETED} to the submitter alone, carrying the counts and the
 * per-folder results so a client can report without polling, and writes a durable notification
 * worded for a clean, a partial, a failed or a cancelled run. Both are best-effort: a delivery
 * failure is logged and never changes the run's recorded outcome, which was settled before this
 * runs.
 *
 * @author dotCMS
 */
public class FolderBulkDuplicateCompletionListener implements EventSubscriber<JobCompletedEvent> {

    private static final String NOTIFICATION_TITLE_KEY = "notification.bulkfolderduplicate.title";
    private static final String NOTIFICATION_SUCCESS_KEY = "notification.bulkfolderduplicate.success";
    private static final String NOTIFICATION_PARTIAL_KEY = "notification.bulkfolderduplicate.partial";
    private static final String NOTIFICATION_FAILED_KEY = "notification.bulkfolderduplicate.failed";
    private static final String NOTIFICATION_CANCELLED_KEY =
            "notification.bulkfolderduplicate.cancelled";

    private final SystemEventsAPI systemEventsAPI;
    private final NotificationAPI notificationAPI;
    private final UserAPI userAPI;

    public FolderBulkDuplicateCompletionListener() {
        this(APILocator.getSystemEventsAPI(), APILocator.getNotificationAPI(),
                APILocator.getUserAPI());
    }

    /**
     * Visible for testing: lets a test capture what the listener decided to tell the author —
     * which message, at which level, to whom — without depending on notification persistence or on
     * the test's transaction boundary, which is a different subject.
     */
    public FolderBulkDuplicateCompletionListener(final SystemEventsAPI systemEventsAPI,
            final NotificationAPI notificationAPI, final UserAPI userAPI) {
        this.systemEventsAPI = systemEventsAPI;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
    }

    @Override
    public void notify(final JobCompletedEvent event) {

        final Job job = null == event ? null : event.getJob();
        if (null == job || !FolderBulkDuplicateHelper.QUEUE_NAME.equals(job.queueName())) {
            return;
        }

        final String userId = submitter(job);
        if (!UtilMethods.isSet(userId)) {
            // Without a submitter there is nobody to tell. Logged rather than returned silently,
            // because it means the job was created without its user parameter — a defect upstream.
            Logger.warn(this, String.format(
                    "Bulk folder duplicate job [%s] finished with no submitting user recorded; "
                            + "no notification sent", job.id()));
            return;
        }

        final Map<String, Object> payload = payloadFor(job);

        Try.run(() -> this.systemEventsAPI.pushAsync(
                        SystemEventType.BULK_FOLDER_DUPLICATE_COMPLETED,
                        new Payload(payload, Visibility.USER, userId)))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to push the bulk folder duplicate completion event for job [%s]",
                        job.id()), e));

        Try.run(() -> record(job, payload, userId))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to record the bulk folder duplicate notification for job [%s]",
                        job.id()), e));
    }

    /**
     * The outcome as it travels to the author: the run's counters and the per-path results. A
     * cancelled run's unreached folders are among the results, as SKIPPED with no reason.
     * <p>
     * <b>The results are not optional.</b> A counts-only payload leaves an author with "27 of 30
     * duplicated" and no way to learn which three, which is exactly what those per-path records exist
     * to answer. The job id travels too: a submitter may have several runs going in several tabs,
     * and without it a client cannot tell which one this event settles.
     */
    public Map<String, Object> payloadFor(final Job job) {

        final Optional<Map<String, Object>> metadata = job.result().flatMap(JobResult::metadata);

        final Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", job.id());
        payload.put("state", job.state());

        metadata.ifPresent(found -> {
            payload.put("total", found.get("total"));
            payload.put("processed", found.get("processed"));
            payload.put("successCount", found.get("successCount"));
            payload.put("failedCount", found.get("failedCount"));
            payload.put("skippedCount", found.get("skippedCount"));
            payload.put("results", found.get("results"));
        });

        return payload;
    }

    /** The event type this pushes, so a client can tell a folder duplication from other background work. */
    public SystemEventType eventType() {
        return SystemEventType.BULK_FOLDER_DUPLICATE_COMPLETED;
    }

    /**
     * Records the durable notification, worded on what actually happened.
     */
    private void record(final Job job, final Map<String, Object> payload, final String userId)
            throws Exception {

        final int succeeded = intValue(payload, "successCount");
        final int failed = intValue(payload, "failedCount");
        final int skipped = intValue(payload, "skippedCount");

        final String messageKey;
        final NotificationLevel level;

        if (JobState.CANCELED == job.state()) {
            // An outcome the author chose, not a fault. Reporting it as an error tells them their
            // duplication broke when they stopped it themselves.
            messageKey = NOTIFICATION_CANCELLED_KEY;
            level = NotificationLevel.WARNING;
        } else if (JobState.SUCCESS != job.state() || (0 == succeeded && failed > 0)) {
            messageKey = NOTIFICATION_FAILED_KEY;
            level = NotificationLevel.ERROR;
        } else if (failed > 0 || skipped > 0) {
            messageKey = NOTIFICATION_PARTIAL_KEY;
            level = NotificationLevel.WARNING;
        } else {
            messageKey = NOTIFICATION_SUCCESS_KEY;
            level = NotificationLevel.INFO;
        }

        // The I18NMessage overload so the counts travel as arguments and the text resolves in the
        // recipient's own locale — not the system user's, which is what legacy did and which meant
        // the message could arrive in a language the reader does not use.
        this.notificationAPI.generateNotification(
                new I18NMessage(NOTIFICATION_TITLE_KEY),
                new I18NMessage(messageKey, null, succeeded, failed, skipped),
                null,
                level,
                NotificationType.GENERIC,
                Visibility.USER,
                userId,
                userId,
                this.userAPI.loadUserById(userId).getLocale());
    }

    private String submitter(final Job job) {
        final Object userId = job.parameters().get("userId");
        return null == userId ? null : String.valueOf(userId);
    }

    private int intValue(final Map<String, Object> payload, final String key) {
        final Object value = payload.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
