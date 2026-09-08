package com.dotcms.rest.api.v1.asset.bulkupload;

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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.util.I18NMessage;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tells the submitting author their batch finished, on both channels (spec FR-019 … FR-023).
 * <p>
 * <b>Why two channels rather than one.</b> They answer different questions. The pushed system event
 * reaches a browser still on the page, so the interface reports the outcome without polling. The
 * durable notification is what remains for an author who closed the tab — and that is the case the
 * whole feature is built around, because an author made to sit and watch a thirty-file upload has
 * been given a worse product than the one that uploaded a single file quickly.
 * <p>
 * <b>Both are best-effort.</b> A failure on either is logged and never alters the recorded outcome
 * (FR-023): the files were created regardless, and losing the run because the doorbell broke would
 * be the worse trade.
 * <p>
 * Follows {@code BulkRefreshCompletionListener} (#37131) one-for-one, including two corrections it
 * had to make and this must not reintroduce — the outcome is addressed to the <b>submitter</b>
 * rather than broadcast to every CMS Administrator, and a run the author <b>cancelled</b> is
 * reported as a warning rather than as a fault.
 * <p>
 * <b>Registered at startup</b> by {@code LocalSystemEventSubscribersInitializer}, deliberately not
 * as a CDI bean: nothing else would inject it, so it would never be constructed, the subscription
 * would never happen, and a finished batch would tell nobody — silently.
 *
 * @author dotCMS
 */
public class BulkUploadCompletionListener implements EventSubscriber<JobCompletedEvent> {

    private static final String NOTIFICATION_TITLE_KEY = "notification.bulkupload.title";
    private static final String NOTIFICATION_SUCCESS_KEY = "notification.bulkupload.success";
    private static final String NOTIFICATION_PARTIAL_KEY = "notification.bulkupload.partial";
    private static final String NOTIFICATION_FAILED_KEY = "notification.bulkupload.failed";
    private static final String NOTIFICATION_CANCELLED_KEY = "notification.bulkupload.cancelled";

    private final SystemEventsAPI systemEventsAPI;
    private final NotificationAPI notificationAPI;
    private final UserAPI userAPI;

    public BulkUploadCompletionListener() {
        this(APILocator.getSystemEventsAPI(), APILocator.getNotificationAPI(),
                APILocator.getUserAPI());
    }

    /**
     * Visible for testing: lets a test capture what the listener decided to tell the author —
     * which message, at which level, to whom — without depending on notification persistence or on
     * the test's transaction boundary, which is a different subject.
     */
    public BulkUploadCompletionListener(final SystemEventsAPI systemEventsAPI,
                                 final NotificationAPI notificationAPI, final UserAPI userAPI) {
        this.systemEventsAPI = systemEventsAPI;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
    }

    @Override
    public void notify(final JobCompletedEvent event) {

        final Job job = null == event ? null : event.getJob();
        if (null == job || !BulkUploadHelper.QUEUE_NAME.equals(job.queueName())) {
            return;
        }

        final String userId = submitter(job);
        if (!UtilMethods.isSet(userId)) {
            // Without a submitter there is nobody to tell. Logged rather than returned silently,
            // because it means the job was created without its user parameter — a defect upstream.
            Logger.warn(this, String.format(
                    "Bulk upload job [%s] finished with no submitting user recorded; "
                            + "no notification sent", job.id()));
            return;
        }

        final Map<String, Object> payload = payloadFor(job);

        Try.run(() -> this.systemEventsAPI.pushAsync(
                        SystemEventType.BULK_UPLOAD_COMPLETED,
                        new Payload(payload, Visibility.USER, userId)))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to push the bulk upload completion event for job [%s]",
                        job.id()), e));

        Try.run(() -> record(job, payload, userId))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to record the bulk upload notification for job [%s]",
                        job.id()), e));
    }

    /**
     * The outcome as it travels to the author: the §3 outcome entire — counts, the per-file
     * results, and the duplicate-resubmission flag.
     * <p>
     * <b>The results are not optional.</b> A counts-only payload leaves an author with "27 of 30
     * created" and no way to learn which three — which FR-023 forbids, because those names are
     * exactly what tell them which files to choose again. Both specs previously summarised this as
     * "the run's counts", which says less than FR-014 … FR-016 actually record; the wording is
     * explicit here so no future change quietly builds the thinner thing.
     * <p>
     * The job id travels too: a submitter may have several runs going in several tabs, and without
     * it a client cannot tell which one this event settles.
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

            // Travels on this channel too, not only on the polled outcome. A client following the
            // push — which is the case User Story 4 is built around, the author who left the page —
            // would otherwise read a resubmission as "every file failed", which is exactly what
            // FR-040a exists to prevent. §4 says the payload is the §3 outcome, and this is part
            // of it.
            payload.put("duplicateSubmission", found.get("duplicateSubmission"));
        });

        return payload;
    }

    /** The event type this pushes, so a client can tell an upload from a reindex. */
    public SystemEventType eventType() {
        return SystemEventType.BULK_UPLOAD_COMPLETED;
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
            // upload broke when they stopped it themselves.
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
