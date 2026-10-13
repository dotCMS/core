package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.api.events.JobEvent;
import com.dotcms.jobs.business.api.events.JobFailedEvent;
import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tells whoever submitted a content import job — a preview or an import — that it ended (#37896).
 * <p>
 * Content Drive waits on this instead of polling the job: the import dialog for a preview, the
 * status toast for an import. It listens to both {@link JobCompletedEvent} and
 * {@link JobFailedEvent}, because a job that throws while processing ends with the failed event
 * only. A failed job can be reported twice (failed, then permanently failed); clients de-duplicate
 * by job id.
 */
public class ImportContentletsCompletionListener implements EventSubscriber<JobEvent> {

    /** The queue content imports run on. */
    static final String QUEUE_NAME = "importContentlets";

    private final SystemEventsAPI systemEventsAPI;

    /** Uses the platform's system events API. */
    public ImportContentletsCompletionListener() {
        this(APILocator.getSystemEventsAPI());
    }

    /**
     * @param systemEventsAPI where the completion event is pushed
     */
    public ImportContentletsCompletionListener(final SystemEventsAPI systemEventsAPI) {
        this.systemEventsAPI = systemEventsAPI;
    }

    /**
     * Pushes {@link SystemEventType#IMPORT_CONTENTLETS_COMPLETED} to the job's submitter when an
     * import job ends; ignores every other queue.
     *
     * @param event a job completed or job failed event
     */
    @Override
    public void notify(final JobEvent event) {
        final Job job = jobOf(event);
        if (null == job || !QUEUE_NAME.equals(job.queueName())) {
            return;
        }

        final Object submitter = null == job.parameters() ? null : job.parameters().get("userId");
        if (null == submitter || !UtilMethods.isSet(submitter.toString())) {
            Logger.warn(this, String.format(
                    "Content import job [%s] ended with no submitting user recorded; "
                            + "no completion event sent", job.id()));
            return;
        }

        Try.run(() -> this.systemEventsAPI.pushAsync(
                        SystemEventType.IMPORT_CONTENTLETS_COMPLETED,
                        new Payload(payloadFor(job), Visibility.USER, submitter.toString())))
                .onFailure(e -> Logger.error(this, String.format(
                        "Unable to push the content import completion event for job [%s]",
                        job.id()), e));
    }

    /**
     * The event payload: job id, state, command, and the summary counts and error message when
     * the job has them.
     *
     * @param job the ended import job
     * @return the payload
     */
    Map<String, Object> payloadFor(final Job job) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", job.id());
        payload.put("state", job.state());
        payload.put("cmd", job.parameters().get("cmd"));

        final Optional<JobResult> result = job.result();
        result.flatMap(JobResult::metadata).map(ImportContentletsCompletionListener::summaryOf)
                .ifPresent(summary -> {
                    payload.put("created", summary.get("created"));
                    payload.put("updated", summary.get("updated"));
                    payload.put("failed", summary.get("failed"));
                });
        result.flatMap(JobResult::errorDetail).map(ErrorDetail::message)
                .ifPresent(message -> payload.put("message", message));

        return payload;
    }

    /**
     * Reads {@code data.summary} from an import result's metadata.
     *
     * @param metadata the job result's metadata
     * @return the summary map, or {@code null} when absent
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> summaryOf(final Map<String, Object> metadata) {
        final Object data = metadata.get("data");
        if (!(data instanceof Map)) {
            return null;
        }
        final Object summary = ((Map<String, Object>) data).get("summary");
        return summary instanceof Map ? (Map<String, Object>) summary : null;
    }

    /**
     * The job an event is about.
     *
     * @param event a job event
     * @return its job, or {@code null} for an event this listener doesn't handle
     */
    private static Job jobOf(final JobEvent event) {
        if (event instanceof JobCompletedEvent) {
            return ((JobCompletedEvent) event).getJob();
        }
        if (event instanceof JobFailedEvent) {
            return ((JobFailedEvent) event).getJob();
        }
        return null;
    }
}
