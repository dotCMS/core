package com.dotcms.rest.api.v1.asset.bulkdelete;

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
 * Tells the submitting author their bulk folder delete finished, on both channels (#37063,
 * FR-031 … FR-035).
 * <p>
 * <b>Why two channels rather than one.</b> The pushed system event reaches a browser still on the
 * page, so the interface reports the outcome without polling. The durable notification is what
 * remains for an author who closed the tab — the case User Story 4 is built around, because an
 * author who deleted forty folders and walked away must still be able to find out whether all
 * forty went.
 * <p>
 * <b>Both are best-effort.</b> A failure on either is logged and never alters the recorded outcome
 * (FR-034): the folders were deleted (or not) regardless, and losing that record because the
 * doorbell broke would be the worse trade.
 * <p>
 * Follows {@link com.dotcms.rest.api.v1.asset.bulkupload.BulkUploadCompletionListener} one-for-one
 * — including the two corrections that listener's own javadoc calls out and this must not
 * reintroduce: addressed to the <b>submitter</b> rather than broadcast, and a run the author
 * <b>cancelled</b> is reported as a warning rather than as a fault. This feature has no
 * resubmission/duplicate concept, so that branch of the upload listener has no counterpart here.
 * <p>
 * <b>Registered at startup</b> by {@code LocalSystemEventSubscribersInitializer}, deliberately not
 * as a CDI bean: nothing else would inject it, so it would never be constructed, the subscription
 * would never happen, and a finished run would tell nobody — silently.
 *
 * @author dotCMS
 */
public class FolderBulkDeleteCompletionListener implements EventSubscriber<JobCompletedEvent> {

    private static final String NOTIFICATION_TITLE_KEY = "notification.bulkfolderdelete.title";
    private static final String NOTIFICATION_SUCCESS_KEY = "notification.bulkfolderdelete.success";
    private static final String NOTIFICATION_PARTIAL_KEY = "notification.bulkfolderdelete.partial";
    private static final String NOTIFICATION_FAILED_KEY = "notification.bulkfolderdelete.failed";
    private static final String NOTIFICATION_CANCELLED_KEY =
            "notification.bulkfolderdelete.cancelled";

    private final SystemEventsAPI systemEventsAPI;
    private final NotificationAPI notificationAPI;
    private final UserAPI userAPI;

    public FolderBulkDeleteCompletionListener() {
        this(APILocator.getSystemEventsAPI(), APILocator.getNotificationAPI(),
                APILocator.getUserAPI());
    }

    /**
     * Visible for testing: lets a test capture what the listener decided to tell the author —
     * which message, at which level, to whom — without depending on notification persistence or on
     * the test's transaction boundary, which is a different subject.
     */
    public FolderBulkDeleteCompletionListener(final SystemEventsAPI systemEventsAPI,
            final NotificationAPI notificationAPI, final UserAPI userAPI) {
        this.systemEventsAPI = systemEventsAPI;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
    }

    @Override
    public void notify(final JobCompletedEvent event) {

        final Job job = null == event ? null : event.getJob();
        if (null == job || !FolderBulkDeleteHelper.QUEUE_NAME.equals(job.queueName())) {
            return;
        }

        final String userId = submitter(job);
        if (!UtilMethods.isSet(userId)) {
            // Without a submitter there is nobody to tell. Logged rather than returned silently,
            // because it means the job was created without its user parameter — a defect upstream.
            Logger.warn(this, String.format(
                    "Bulk folder delete job [%s] finished with no submitting user recorded; "
                            + "no notification sent", job.id()));
            return;
        }

        final Map<String, Object> payload = payloadFor(job);

        Try.run(() -> this.systemEventsAPI.pushAsync(
                        SystemEventType.BULK_FOLDER_DELETE_COMPLETED,
                        new Payload(payload, Visibility.USER, userId)))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to push the bulk folder delete completion event for job [%s]",
                        job.id()), e));

        Try.run(() -> record(job, payload, userId))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to record the bulk folder delete notification for job [%s]",
                        job.id()), e));
    }

    /**
     * The outcome as it travels to the author: the run's counters, the per-path results, and —
     * on a cancelled run — where it stopped.
     * <p>
     * <b>The results are not optional.</b> A counts-only payload leaves an author with "27 of 30
     * deleted" and no way to learn which three, which is exactly what those per-path records exist
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
            if (found.containsKey("stoppedAt")) {
                // Only present on a cancelled run (FR-028) — a client following the push rather
                // than polling the job still needs to know where a cancelled run stopped.
                payload.put("stoppedAt", found.get("stoppedAt"));
            }
        });

        return payload;
    }

    /** The event type this pushes, so a client can tell a folder delete from other background work. */
    public SystemEventType eventType() {
        return SystemEventType.BULK_FOLDER_DELETE_COMPLETED;
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
            // delete broke when they stopped it themselves.
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
