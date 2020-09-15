package com.dotmarketing.quartz.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.rest.IntegrityResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author victor
 */
@RunWith(DataProviderRunner.class)
public class IntegrityDataGenerationJobTest extends IntegrationTestBase {

    private IntegrityDataGenerationJob integrityDataGenerationJob;
    private PublishingEndPoint endpoint;
    private String requestId;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void before() throws DotDataException {
        integrityDataGenerationJob = new IntegrityDataGenerationJob();
        endpoint = new PushPublishingEndPoint();
        endpoint.setId(UUID.randomUUID().toString());
        requestId = UUID.randomUUID().toString();

        DotConnect dotConnect = new DotConnect();
        dotConnect
                .setSQL("delete from QRTZ_EXCL_SIMPLE_TRIGGERS where TRIGGER_NAME = ? and TRIGGER_GROUP = ?")
                .addParam(IntegrityDataGenerationJob.TRIGGER_NAME)
                .addParam(IntegrityDataGenerationJob.TRIGGER_GROUP)
                .loadObjectResults();
        dotConnect
                .setSQL("delete from QRTZ_EXCL_TRIGGERS where TRIGGER_NAME = ? and TRIGGER_GROUP = ?")
                .addParam(IntegrityDataGenerationJob.TRIGGER_NAME)
                .addParam(IntegrityDataGenerationJob.TRIGGER_GROUP)
                .loadObjectResults();
        dotConnect
                .setSQL("delete from QRTZ_EXCL_JOB_DETAILS where JOB_NAME = ? and JOB_GROUP = ?")
                .addParam(IntegrityDataGenerationJob.JOB_NAME)
                .addParam(IntegrityDataGenerationJob.JOB_GROUP)
                .loadObjectResults();
    }

    @Test
    public void testGenerateIntegrationData() throws Exception {
        runJob(Sneaky.sneaked(() -> {
            integrityDataGenerationJob.run(getJobContext());
        }));

        DotConnect dotConnect = new DotConnect();
        assertEquals(
                dotConnect
                        .setSQL("select 1 as test from QRTZ_EXCL_SIMPLE_TRIGGERS where TRIGGER_NAME = ? and TRIGGER_GROUP = ?")
                        .addParam(IntegrityDataGenerationJob.TRIGGER_NAME)
                        .addParam(IntegrityDataGenerationJob.TRIGGER_GROUP)
                        .loadInt("test"),
                1);
        assertEquals(
                dotConnect
                        .setSQL("select 1 as test from QRTZ_EXCL_TRIGGERS where TRIGGER_NAME = ? and TRIGGER_GROUP = ?")
                        .addParam(IntegrityDataGenerationJob.TRIGGER_NAME)
                        .addParam(IntegrityDataGenerationJob.TRIGGER_GROUP)
                        .loadInt("test"),
                1);
        assertEquals(
                dotConnect
                        .setSQL("select 1 as test from QRTZ_EXCL_JOB_DETAILS where JOB_NAME = ? and JOB_GROUP = ?")
                        .addParam(IntegrityDataGenerationJob.JOB_NAME)
                        .addParam(IntegrityDataGenerationJob.JOB_GROUP)
                        .loadInt("test"),
                1);
        assertTrue(new File(
                IntegrityUtil.getIntegrityDataFile(
                        endpoint.getId(),
                        IntegrityUtil.INTEGRITY_DATA_TO_CHECK_ZIP_FILENAME))
                .exists());
        assertTrue(new File(
                IntegrityUtil.getIntegrityDataFile(endpoint.getId(), IntegrityUtil.INTEGRITY_DATA_STATUS_FILENAME))
                .exists());
        assertStatus(IntegrityResource.ProcessStatus.FINISHED.toString());
    }

    @Test
    public void test_interrupt() throws Exception {
        integrityDataGenerationJob = new IntegrityDataGenerationJob() {
            @Override
            public void run(JobExecutionContext jobContext) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {}
                super.run(jobContext);
            }
        };

        runJob(Sneaky.sneaked(() -> integrityDataGenerationJob.run(getJobContext())));
        interrupt(Sneaky.sneaked(() -> integrityDataGenerationJob.interrupt()));
        assertStatus(IntegrityResource.ProcessStatus.CANCELLED.toString());

    }

    private void runJob(Runnable runnable) {
        IntegrityDataGenerationJob.triggerIntegrityDataGeneration(endpoint, requestId);
        runnable.run();
    }

    private void interrupt(Runnable runnable) throws SchedulerException {

        IntegrityDataGenerationJob.getJobScheduler().interrupt(
                IntegrityDataGenerationJob.JOB_NAME,
                IntegrityDataGenerationJob.JOB_GROUP);
        runnable.run();
    }

    private JobExecutionContext getJobContext() throws SchedulerException {
        return new JobExecutionContext(
                IntegrityDataGenerationJob.getJobScheduler(),
                new TestJobExecutor.TriggerFiredBundleTest(getJobDetail(endpoint, requestId)),
                integrityDataGenerationJob);
    }

    private JobDetail getJobDetail(PublishingEndPoint endpoint, String requestId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(IntegrityUtil.REQUESTER_ENDPOINT, endpoint);
        jobDataMap.put(IntegrityUtil.INTEGRITY_DATA_REQUEST_ID, requestId);

        final JobDetail jobDetail = new JobDetail(
                IntegrityDataGenerationJob.JOB_NAME,
                IntegrityDataGenerationJob.JOB_GROUP, IntegrityDataGenerationJob.class);
        jobDetail.setJobDataMap(jobDataMap);
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);

        return jobDetail;
    }

    private void assertStatus(String status) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(IntegrityUtil.getIntegrityDataFile(endpoint.getId(), IntegrityUtil.INTEGRITY_DATA_STATUS_FILENAME)));
        assertEquals(status, properties.getProperty(IntegrityUtil.INTEGRITY_DATA_STATUS));
    }
}
