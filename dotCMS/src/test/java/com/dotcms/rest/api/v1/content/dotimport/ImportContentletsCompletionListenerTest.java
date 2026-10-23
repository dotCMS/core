package com.dotcms.rest.api.v1.content.dotimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.api.events.JobFailedEvent;
import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link ImportContentletsCompletionListener} (#37896).
 * <p>
 * Content Drive never polls an import job: its preview dialog and its status toast wait for this
 * event. So a finished job must announce itself to whoever submitted it, say whether it was a preview
 * or an import, and carry the counts — and a failed job must carry the reason.
 */
public class ImportContentletsCompletionListenerTest {

    private static final String JOB_ID = "import-job-1";
    private static final String USER_ID = "user-1";

    private SystemEventsAPI systemEventsAPI;
    private ImportContentletsCompletionListener listener;

    @Before
    public void setUp() {
        systemEventsAPI = mock(SystemEventsAPI.class);
        listener = new ImportContentletsCompletionListener(systemEventsAPI);
    }

    /**
     * Given scenario: an import job ({@code cmd=publish}) finishes successfully.
     * Expected result: an IMPORT_CONTENTLETS_COMPLETED event goes to the submitter only, with the
     * job id, state, command and the created / updated / failed counts.
     */
    @Test
    public void test_completedImport_pushesCountsToTheSubmitter() throws Exception {
        listener.notify(new JobCompletedEvent(
                job(JobState.SUCCESS, "publish", summary(4, 2, 1), null), LocalDateTime.now()));

        final Map<String, Object> data = pushedPayload(USER_ID);
        assertEquals(JOB_ID, data.get("jobId"));
        assertEquals(JobState.SUCCESS, data.get("state"));
        assertEquals("publish", data.get("cmd"));
        assertEquals(4, data.get("created"));
        assertEquals(2, data.get("updated"));
        assertEquals(1, data.get("failed"));
        assertFalse("A successful job carries no error message", data.containsKey("message"));
    }

    /**
     * Given scenario: a preview job ({@code cmd=preview}) finishes.
     * Expected result: the event says it was a preview, so the Drive routes it to the dialog and not
     * to the status toast.
     */
    @Test
    public void test_completedPreview_saysItWasAPreview() throws Exception {
        listener.notify(new JobCompletedEvent(
                job(JobState.SUCCESS, "preview", summary(3, 0, 0), null), LocalDateTime.now()));

        assertEquals("preview", pushedPayload(USER_ID).get("cmd"));
    }

    /**
     * Given scenario: an import job fails before producing a result.
     * Expected result: the event still reaches the submitter, with the failed state and the job's
     * error message, and no counts.
     */
    @Test
    public void test_failedJob_carriesTheErrorMessage() throws Exception {
        final ErrorDetail error = ErrorDetail.builder()
                .message("Content type [Blog] not found")
                .exceptionClass("com.dotmarketing.exception.DoesNotExistException")
                .stackTrace("")
                .timestamp(LocalDateTime.now())
                .processingStage("process")
                .build();

        listener.notify(new JobFailedEvent(
                job(JobState.FAILED, "publish", null, error), LocalDateTime.now()));

        final Map<String, Object> data = pushedPayload(USER_ID);
        assertEquals(JobState.FAILED, data.get("state"));
        assertEquals("Content type [Blog] not found", data.get("message"));
        assertFalse("A job with no result has no counts", data.containsKey("created"));
    }

    /**
     * Given scenario: a job from another queue finishes.
     * Expected result: ignored; this listener sits on the shared job events.
     */
    @Test
    public void test_otherQueues_areIgnored() throws Exception {
        final Job job = mock(Job.class);
        when(job.queueName()).thenReturn("assetBulkUpload");

        listener.notify(new JobCompletedEvent(job, LocalDateTime.now()));
        listener.notify(new JobFailedEvent(job, LocalDateTime.now()));

        verify(systemEventsAPI, never()).pushAsync(any(), any());
    }

    /**
     * Given scenario: an import job finishes with no submitting user recorded.
     * Expected result: nothing is pushed, since there is nobody to tell.
     */
    @Test
    public void test_jobWithoutUser_pushesNothing() throws Exception {
        final Job job = job(JobState.SUCCESS, "publish", summary(1, 0, 0), null);
        when(job.parameters()).thenReturn(ImmutableMap.<String, Object>of("cmd", "publish"));

        listener.notify(new JobCompletedEvent(job, LocalDateTime.now()));

        verify(systemEventsAPI, never()).pushAsync(any(), any());
    }

    /** The payload pushed as IMPORT_CONTENTLETS_COMPLETED, after checking it went to {@code userId}. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> pushedPayload(final String userId) throws Exception {
        final ArgumentCaptor<Payload> payload = ArgumentCaptor.forClass(Payload.class);
        verify(systemEventsAPI)
                .pushAsync(eq(SystemEventType.IMPORT_CONTENTLETS_COMPLETED), payload.capture());
        assertEquals(Visibility.USER, payload.getValue().getVisibility());
        assertEquals(userId, payload.getValue().getVisibilityValue());
        return (Map<String, Object>) payload.getValue().getData();
    }

    /** An import result's metadata, as the processor stores it: {@code data.summary.*}. */
    private static Map<String, Object> summary(final int created, final int updated,
            final int failed) {
        final Map<String, Object> counts = new HashMap<>();
        counts.put("created", created);
        counts.put("updated", updated);
        counts.put("failed", failed);
        return Map.of("data", Map.of("summary", counts));
    }

    private static Job job(final JobState state, final String cmd,
            final Map<String, Object> metadata, final ErrorDetail error) {
        final Job job = mock(Job.class);
        when(job.id()).thenReturn(JOB_ID);
        when(job.queueName()).thenReturn("importContentlets");
        when(job.state()).thenReturn(state);
        when(job.parameters()).thenReturn(ImmutableMap.<String, Object>of("userId", USER_ID, "cmd", cmd));
        final JobResult.Builder result = JobResult.builder();
        if (metadata != null) {
            result.metadata(metadata);
        }
        if (error != null) {
            result.errorDetail(error);
        }
        when(job.result()).thenReturn(
                metadata == null && error == null ? Optional.empty() : Optional.of(result.build()));
        return job;
    }
}
