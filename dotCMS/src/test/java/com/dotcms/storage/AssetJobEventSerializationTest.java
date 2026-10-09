package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.api.events.JobCreatedEvent;
import com.dotcms.jobs.business.api.events.JobFailedEvent;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.util.marshal.JacksonMarshalUtilsImpl;
import com.dotmarketing.util.Config;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AssetJobEventSerializationTest {
    @Test
    void cleanupEventsCanBeReadBackFromTheNotificationStore() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            final var codec = new JacksonMarshalUtilsImpl();
            final var job = Job.builder().id("cleanup-test").queueName("bundleArchiveCleanup")
                    .state(JobState.PENDING).parameters(Map.of("bundleId", "Mixed-Case")).build();
            final var now = LocalDateTime.now();
            final var json = new com.fasterxml.jackson.databind.ObjectMapper();
            for (var event : List.of(new JobCreatedEvent(job.id(), job.queueName(), now, job.parameters()),
                    new JobCompletedEvent(job, now), new JobFailedEvent(job, now))) {
                final var encoded = new StringWriter();
                codec.marshal(encoded, event);
                final Object restored = codec.unmarshal(encoded.toString(), event.getClass());
                final var encodedAgain = new StringWriter();
                codec.marshal(encodedAgain, restored);
                assertEquals(json.readTree(encoded.toString()), json.readTree(encodedAgain.toString()));
            }
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }
}
