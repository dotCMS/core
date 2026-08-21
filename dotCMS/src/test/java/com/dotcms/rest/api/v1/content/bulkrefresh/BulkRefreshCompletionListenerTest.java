package com.dotcms.rest.api.v1.content.bulkrefresh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.UserAPI;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link BulkRefreshCompletionListener}.
 * <p>
 * This is the piece that replaced polling, so what matters is that a finished run genuinely announces
 * itself: to the right person, with the counts it actually produced, and worded on what happened rather
 * than assuming success.
 */
public class BulkRefreshCompletionListenerTest {

    private static final String USER_ID = "user-1";

    private SystemEventsAPI systemEventsAPI;
    private NotificationAPI notificationAPI;
    private BulkRefreshCompletionListener listener;

    @Before
    public void setUp() throws Exception {
        systemEventsAPI = mock(SystemEventsAPI.class);
        notificationAPI = mock(NotificationAPI.class);

        final UserAPI userAPI = mock(UserAPI.class);
        final User user = mock(User.class);
        when(user.getLocale()).thenReturn(java.util.Locale.ENGLISH);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user);

        listener = new BulkRefreshCompletionListener(systemEventsAPI, notificationAPI, userAPI);
    }

    /**
     * Method to test: {@link BulkRefreshCompletionListener#notify}
     * <p>
     * Given scenario: A bulk refresh job finishes successfully.
     * <p>
     * Expected result: An event carrying the run's counters is pushed to the submitting user only.
     * Scoping it to the user is deliberate — the legacy batch reindex announced completion to every CMS
     * Administrator, which is nobody else's business.
     */
    @Test
    public void test_onJobCompleted_pushesTheCountersToTheSubmitter() throws Exception {
        listener.notify(event(JobState.SUCCESS, counters(3, 3, 0, 0, 5)));

        final ArgumentCaptor<Payload> payload = ArgumentCaptor.forClass(Payload.class);
        verify(systemEventsAPI)
                .pushAsync(eq(SystemEventType.BULK_REFRESH_COMPLETED), payload.capture());

        assertEquals(Visibility.USER, payload.getValue().getVisibility());
        assertEquals(USER_ID, payload.getValue().getVisibilityValue());

        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) payload.getValue().getData();
        assertEquals(3, data.get(BulkRefreshCompletionListener.EVENT_TOTAL));
        assertEquals(3, data.get(BulkRefreshCompletionListener.EVENT_SUCCESS_COUNT));
        assertEquals(5, data.get(BulkRefreshCompletionListener.EVENT_VERSIONS_INDEXED));
        assertEquals(JobState.SUCCESS, data.get(BulkRefreshCompletionListener.EVENT_STATE));
    }

    /**
     * Method to test: {@link BulkRefreshCompletionListener#notify}
     * <p>
     * Given scenario: A job from a different queue finishes.
     * <p>
     * Expected result: Ignored entirely. This listener sits on the shared job-completed event, so every
     * content import and every other queue's job passes through it.
     */
    @Test
    public void test_onJobCompleted_ignoresOtherQueues() throws Exception {
        final Job job = mock(Job.class);
        when(job.queueName()).thenReturn("importContentlets");

        listener.notify(new JobCompletedEvent(job, LocalDateTime.now()));

        verify(systemEventsAPI, never()).pushAsync(any(), any());
        verify(notificationAPI, never()).generateNotification(
                any(I18NMessage.class), any(I18NMessage.class), any(), any(), any(), any(),
                any(), any(), any());
    }

    /**
     * Method to test: {@link BulkRefreshCompletionListener#notify}
     * <p>
     * Given scenario: A clean run, then a run with failures, then a permanently failed job.
     * <p>
     * Expected result: The notification level tracks what actually happened. The legacy batch reindex
     * reported "finished successfully" unconditionally — even when every single item had failed — which
     * is the misleading signal this endpoint exists to remove.
     */
    @Test
    public void test_onJobCompleted_notificationLevelReflectsTheOutcome() throws Exception {
        listener.notify(event(JobState.SUCCESS, counters(2, 2, 0, 0, 2)));
        assertEquals(NotificationLevel.INFO, capturedLevel());

        setUpFresh();
        listener.notify(event(JobState.SUCCESS, counters(3, 2, 1, 0, 2)));
        assertEquals("A shortfall must not read as a clean run",
                NotificationLevel.WARNING, capturedLevel());

        setUpFresh();
        listener.notify(event(JobState.FAILED_PERMANENTLY, counters(0, 0, 0, 0, 0)));
        assertEquals("A dead job must not read as a success",
                NotificationLevel.ERROR, capturedLevel());
    }

    /**
     * Method to test: {@link BulkRefreshCompletionListener#notify}
     * <p>
     * Given scenario: A job whose parameters carry no submitting user.
     * <p>
     * Expected result: Nothing is pushed. There is nobody to tell, and guessing a recipient would send
     * somebody else's reindex outcome to the wrong person.
     */
    @Test
    public void test_onJobCompleted_withoutASubmitterTellsNobody() throws Exception {
        final Job job = mock(Job.class);
        when(job.queueName()).thenReturn(BulkRefreshHelper.BULK_REFRESH_QUEUE_NAME);
        when(job.parameters()).thenReturn(ImmutableMap.of());

        listener.notify(new JobCompletedEvent(job, LocalDateTime.now()));

        verify(systemEventsAPI, never()).pushAsync(any(), any());
    }

    /**
     * Method to test: {@link BulkRefreshCompletionListener#notify}
     * <p>
     * Given scenario: A terminal job that carried no result metadata at all.
     * <p>
     * Expected result: The event still goes out, carrying the state but no counters, so the client can
     * tell "finished but unreportable" apart from a clean run over nothing — which is what all-zero
     * counters would look like.
     */
    @Test
    public void test_onJobCompleted_withoutMetadataStillReportsTheState() throws Exception {
        final Job job = mock(Job.class);
        when(job.queueName()).thenReturn(BulkRefreshHelper.BULK_REFRESH_QUEUE_NAME);
        when(job.state()).thenReturn(JobState.SUCCESS);
        when(job.parameters()).thenReturn(
                ImmutableMap.of(BulkRefreshContentletsProcessor.PARAM_USER_ID, USER_ID));
        when(job.result()).thenReturn(Optional.empty());

        listener.notify(new JobCompletedEvent(job, LocalDateTime.now()));

        final ArgumentCaptor<Payload> payload = ArgumentCaptor.forClass(Payload.class);
        verify(systemEventsAPI).pushAsync(any(), payload.capture());

        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) payload.getValue().getData();
        assertEquals(JobState.SUCCESS, data.get(BulkRefreshCompletionListener.EVENT_STATE));
        assertTrue("No counters is different from zero counters",
                !data.containsKey(BulkRefreshCompletionListener.EVENT_TOTAL));
    }

    private void setUpFresh() throws Exception {
        setUp();
    }

    private NotificationLevel capturedLevel() throws Exception {
        final ArgumentCaptor<NotificationLevel> level =
                ArgumentCaptor.forClass(NotificationLevel.class);
        verify(notificationAPI).generateNotification(
                any(I18NMessage.class), any(I18NMessage.class), any(), level.capture(),
                any(), any(), any(), any(), any());

        return level.getValue();
    }

    private static Map<String, Object> counters(final int total, final int success, final int failed,
            final int skipped, final int versions) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put(BulkRefreshCompletionListener.EVENT_TOTAL, total);
        metadata.put(BulkRefreshCompletionListener.EVENT_SUCCESS_COUNT, success);
        metadata.put(BulkRefreshCompletionListener.EVENT_FAILED_COUNT, failed);
        metadata.put(BulkRefreshCompletionListener.EVENT_SKIPPED_COUNT, skipped);
        metadata.put(BulkRefreshCompletionListener.EVENT_VERSIONS_INDEXED, versions);

        return metadata;
    }

    private static JobCompletedEvent event(final JobState state,
            final Map<String, Object> metadata) {
        final Job job = mock(Job.class);
        when(job.queueName()).thenReturn(BulkRefreshHelper.BULK_REFRESH_QUEUE_NAME);
        when(job.state()).thenReturn(state);
        when(job.parameters()).thenReturn(
                ImmutableMap.of(BulkRefreshContentletsProcessor.PARAM_USER_ID, USER_ID));
        when(job.result()).thenReturn(
                Optional.of(JobResult.builder().metadata(metadata).build()));

        return new JobCompletedEvent(job, LocalDateTime.now());
    }
}
