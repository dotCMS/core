package com.dotcms.jobs.business.processor.impl;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Non-assertive: records, as a logged data point for #37565 (the un-bounded-transaction follow-up
 * this feature deliberately does not fix), the peak heap delta observed while one top-level folder
 * with a substantial content count is deleted (#37063, SC-004).
 * <p>
 * <b>Deliberately not a pass/fail gate.</b> Asserting a hard byte ceiling would be flaky across
 * environments (heap size, GC behavior, concurrent test load) and is not what SC-004 asks for —
 * the spec's own words are "recorded... so the follow-up ticket has a number to work against," not
 * "enforced." This test therefore has no assertion that can fail on the measurement itself; a
 * failure here means the delete itself broke, not that memory crossed some threshold.
 * <p>
 * <b>One data point, not a ceiling search.</b> Finding the actual point where a node's memory is
 * exhausted would mean scaling up folder size until something breaks, which is exactly the kind of
 * environment-dependent, CI-hazardous test SC-004 explicitly rules out. This logs one substantial,
 * realistic size instead, leaving the follow-up ticket to run the same measurement at whatever
 * other sizes it needs once it is actually scoped — a starting number, not an exhaustive one.
 */
@EnableWeld
public class FolderBulkDeleteMemoryCeilingIT extends Junit5WeldBaseTest {

    private static final int MEASURED_CONTENT_COUNT = 2000;
    private static final long SAMPLE_INTERVAL_MILLIS = 100;

    private static User admin;
    private static Host site;
    private static ContentType contentType;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}
     * Given Scenario: One top-level folder carrying {@value #MEASURED_CONTENT_COUNT} contentlets
     * ExpectedResult: No specific result is asserted — the run must simply complete. Its peak heap
     * delta is logged at INFO for a human (or #37565) to read, not checked against any threshold.
     */
    @Test
    public void test_process_measuresPeakHeapDeltaForOneSubstantialFolder_dataPointOnly()
            throws Exception {

        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        for (int i = 0; i < MEASURED_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(folder).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), folder.getName());
        final Map<String, Object> pathParam = new HashMap<>();
        pathParam.put("path", path);
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", admin.getUserId());
        parameters.put("paths", List.of(pathParam));

        final Job job = Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName(FolderBulkDeleteHelper.QUEUE_NAME)
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(new DefaultProgressTracker())
                .build();

        final Runtime runtime = Runtime.getRuntime();
        System.gc();
        final long baselineUsed = runtime.totalMemory() - runtime.freeMemory();

        final AtomicLong peakUsed = new AtomicLong(baselineUsed);
        final AtomicBoolean sampling = new AtomicBoolean(true);
        final Thread sampler = new Thread(() -> {
            while (sampling.get()) {
                final long used = runtime.totalMemory() - runtime.freeMemory();
                peakUsed.getAndUpdate(current -> Math.max(current, used));
                try {
                    Thread.sleep(SAMPLE_INTERVAL_MILLIS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "memory-ceiling-sampler");
        sampler.setDaemon(true);

        sampler.start();
        try {
            new FolderBulkDeleteProcessor().process(job);
        } finally {
            sampling.set(false);
            sampler.join(1000);
        }

        final long peakDeltaBytes = peakUsed.get() - baselineUsed;
        Logger.info(this, String.format(
                "Bulk folder delete memory data point (SC-004, #37565): %d contentlet(s) in one "
                        + "top-level folder, peak heap delta ~%d MB (baseline used %d MB, peak "
                        + "used %d MB)",
                MEASURED_CONTENT_COUNT, peakDeltaBytes / (1024 * 1024),
                baselineUsed / (1024 * 1024), peakUsed.get() / (1024 * 1024)));
    }
}
