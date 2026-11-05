package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.receiver.BundleOrigin;
import com.dotcms.publishing.PublisherConfig;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.StatefulJob;

/**
 * Tests how {@link PushPublisherJob#run(JobExecutionContext)} reads how a bundle arrived from the
 * trigger data.
 */
public class PushPublisherJobTest {

    private static final class CapturingJob extends PushPublisherJob {
        private final Map<String, Serializable> data;
        String bundleName;
        BundleOrigin origin = BundleOrigin.REGISTERED_SENDER;
        String uploaderId = "unset";

        CapturingJob(final Map<String, Serializable> data) {
            this.data = data;
        }

        @Override
        protected Map<String, Serializable> getExecutionData(final Trigger trigger,
                final Class<? extends StatefulJob> jobClass) {
            return data;
        }

        @Override
        public PublisherConfig processBundle(final String bundleName, final PublishAuditStatus status,
                final BundleOrigin origin, final String uploaderId) {
            this.bundleName = bundleName;
            this.origin = origin;
            this.uploaderId = uploaderId;
            return null;
        }
    }

    private static JobExecutionContext context() {
        final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getTrigger()).thenReturn(Mockito.mock(Trigger.class));
        return context;
    }

    /**
     * Method to Test: {@link PushPublisherJob#run(JobExecutionContext)}
     * When: the trigger data has no information about how the bundle arrived, as for a job queued
     * before this information was recorded
     * Should: process the bundle with an unknown origin and no uploader, so no plugin is deployed.
     */
    @Test
    public void test_run_triggerWithoutOrigin_processesWithUnknownOrigin() throws Exception {
        final Map<String, Serializable> data = new HashMap<>();
        data.put("bundleName", "b.tar.gz");
        data.put("status", Mockito.mock(PublishAuditStatus.class));
        final CapturingJob job = new CapturingJob(data);

        job.run(context());

        assertEquals("b.tar.gz", job.bundleName);
        assertNull(job.origin);
        assertNull(job.uploaderId);
    }

    /**
     * Method to Test: {@link PushPublisherJob#run(JobExecutionContext)}
     * When: the trigger data carries a user upload and the uploader's id
     * Should: pass both on to the bundle processing.
     */
    @Test
    public void test_run_triggerWithUserUpload_passesOriginAndUploader() throws Exception {
        final Map<String, Serializable> data = new HashMap<>();
        data.put("bundleName", "b.tar.gz");
        data.put("status", Mockito.mock(PublishAuditStatus.class));
        data.put("origin", BundleOrigin.USER_UPLOAD.name());
        data.put("uploaderId", "user-1");
        final CapturingJob job = new CapturingJob(data);

        job.run(context());

        assertEquals(BundleOrigin.USER_UPLOAD, job.origin);
        assertEquals("user-1", job.uploaderId);
    }

    /**
     * Method to Test: {@link PushPublisherJob#run(JobExecutionContext)}
     * When: the trigger data carries a value that is not a known origin
     * Should: process the bundle with an unknown origin.
     */
    @Test
    public void test_run_triggerWithUnrecognizedOrigin_processesWithUnknownOrigin() throws Exception {
        final Map<String, Serializable> data = new HashMap<>();
        data.put("bundleName", "b.tar.gz");
        data.put("status", Mockito.mock(PublishAuditStatus.class));
        data.put("origin", "SOMETHING_ELSE");
        final CapturingJob job = new CapturingJob(data);

        job.run(context());

        assertNull(job.origin);
    }
}
