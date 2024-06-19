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
import java.util.Date;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.TriggerFiredBundle;

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

    /**
     * Method to test: IntegrityDataGenerationJob.run() which generates integrity data generation.
     * Given Scenario: Given an integrity data generation is triggered for a provided endpoint and request id.
     * ExpectedResult: to have Quartz job tables populated as well as the integrity data file created with its status file.
     *
     */
    @Test
    public void test_generateIntegrationData() throws Exception {
        runJob(Sneaky.sneaked(() -> integrityDataGenerationJob.run(getJobContext())));

        assertTrue(new File(
                IntegrityUtil.getIntegrityDataFilePath(
                        endpoint.getId(),
                        IntegrityUtil.INTEGRITY_DATA_TO_CHECK_ZIP_FILENAME))
                .exists());
        assertTrue(new File(
                IntegrityUtil.getIntegrityDataFilePath(endpoint.getId(), IntegrityUtil.INTEGRITY_DATA_STATUS_FILENAME))
                .exists());
        assertStatus(IntegrityResource.ProcessStatus.FINISHED.toString());
    }

    private void runJob(Runnable runnable) {
        runnable.run();
    }

    private JobExecutionContext getJobContext() throws SchedulerException {
        JobDetail jobDetail = getJobDetail(endpoint, requestId);

        // Create a simple trigger
        SimpleTriggerImpl trigger = new SimpleTriggerImpl();
        trigger.setName("testTrigger");
        trigger.setGroup("testGroup");
        trigger.setStartTime(new Date());
        trigger.setRepeatCount(0);
        trigger.setRepeatInterval(0);

        // Create the TriggerFiredBundle
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                jobDetail,
                trigger,
                null,
                false,
                null,
                null,
                null,
                null
        );

        // Get the scheduler
        Scheduler scheduler = IntegrityDataGenerationJob.getJobScheduler();

        // Return the JobExecutionContext
        return new JobExecutionContextImpl(scheduler, bundle, integrityDataGenerationJob);
    }

    private JobDetail getJobDetail(PublishingEndPoint endpoint, String requestId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(IntegrityUtil.REQUESTER_KEY, endpoint.getId());
        jobDataMap.put(IntegrityUtil.INTEGRITY_DATA_REQUEST_ID, requestId);

        return JobBuilder.newJob(IntegrityDataGenerationJob.class)
                .withIdentity(IntegrityDataGenerationJob.JOB_NAME, IntegrityDataGenerationJob.JOB_GROUP)
                .usingJobData(jobDataMap)
                .storeDurably(false)
                .requestRecovery(true)
                .build();
    }

    private void assertStatus(String status) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(IntegrityUtil.getIntegrityDataFilePath(
                endpoint.getId(),
                IntegrityUtil.INTEGRITY_DATA_STATUS_FILENAME)));
        assertEquals(status, properties.getProperty(IntegrityUtil.INTEGRITY_DATA_STATUS));
    }
}