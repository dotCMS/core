package com.dotmarketing.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob.TriggerBuilder;
import com.dotmarketing.quartz.job.TestJobExecutor;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;

/**
 * Tests used to verify that {@link DotStatefulJob} instances only create one trigger for multiple
 * jobs
 *
 * @author nollymar
 */
public class DotStatefulJobTest {

    private final static int THREAD_AMOUNT = 7;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * The main purpose of this test is to verify the logic applied by the {@link DotJobStore} class
     * to create triggers for Stateful jobs
     * <b>Method to test:</b> {@link DotJobStore#storeJobAndTrigger(SchedulingContext, JobDetail,
     * Trigger)}
     * <p>
     * <b>Given scenario:</b> Multiple threads trigger jobs in a stateful job
     * <p>
     * <b>Expected result:</b> Only one trigger should be created to handle all the jobs
     */
    @Test
    public void testDotStatefulJobIsReallyStateful()
            throws SchedulerException, InterruptedException {
        Scheduler scheduler = QuartzUtils.getSequentialScheduler();

        final CyclicBarrier cyclicBarrier = new CyclicBarrier(THREAD_AMOUNT);
        final CountDownLatch countDownLatch = new CountDownLatch(THREAD_AMOUNT);

        for (int i = 0; i < THREAD_AMOUNT; i++) {
            final Thread worker = new Thread(
                    new StatefulTestThread(cyclicBarrier, countDownLatch, scheduler));
            worker.setName("Thread " + i);
            worker.start();
        }

        //wait until all threads are executed to verify the assert
        countDownLatch.await(15, TimeUnit.SECONDS);

        assertEquals(1, scheduler.getTriggerNames("StatefulTestJob_trigger_group").length);
    }


    /**
     * <b>Method to test:</b> {@link TriggerBuilder#build()}
     * <p>
     * <b>Given scenario:</b> Multiple threads trigger jobs in a stateful job
     * <p>
     * <b>Expected result:</b> Only one trigger should be created to handle all the jobs
     */
    @Test
    public void testTriggerBuilder() throws InterruptedException {

        final CyclicBarrier cyclicBarrier = new CyclicBarrier(THREAD_AMOUNT);
        final CountDownLatch countDownLatch = new CountDownLatch(THREAD_AMOUNT);
        final DotStatefulTestJob dotStatefulTestJob = new DotStatefulTestJob();

        for (int i = 0; i < THREAD_AMOUNT; i++) {
            final Thread worker = new Thread(
                    new StatefulBuilderTestThread(cyclicBarrier, countDownLatch, dotStatefulTestJob));
            worker.setName("Thread " + i);
            worker.start();
        }

        //wait until all threads are executed to verify the assert
        countDownLatch.await(15, TimeUnit.SECONDS);

        assertTrue(DotStatefulTestJob.getThreadsExecuted() > 1);
    }

    /**
     * Inner class with the logic to be executed by each thread in {@link
     * DotStatefulJobTest#testDotStatefulJobIsReallyStateful()} ()}
     */
    private class StatefulTestThread implements Runnable {

        private CyclicBarrier cyclicBarrier;
        private CountDownLatch countDownLatch;
        private Scheduler scheduler;

        private StatefulTestThread(final CyclicBarrier cyclicBarrier,
                final CountDownLatch countDownLatch,
                final Scheduler scheduler) {
            this.cyclicBarrier = cyclicBarrier;
            this.countDownLatch = countDownLatch;
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            final String thisThreadName = Thread.currentThread().getName();

            try {
                final JobDetail jobDetail = getJobDetail(thisThreadName);

                final Trigger trigger = new TriggerBuilder().jobDetail(jobDetail)
                        .triggerGroupName("StatefulTestJob_trigger_group").build();

                //wait for all threads to schedule jobs concurrently
                cyclicBarrier.await();
                scheduler.scheduleJob(jobDetail, trigger);
                countDownLatch.countDown();
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        }
    }

    /**
     * Inner class with the logic to be executed by each thread in {@link
     * DotStatefulJobTest#testTriggerBuilder()}
     */
    private class StatefulBuilderTestThread implements Runnable {

        private CyclicBarrier cyclicBarrier;
        private CountDownLatch countDownLatch;
        private DotStatefulTestJob dotStatefulTestJob;

        private StatefulBuilderTestThread(final CyclicBarrier cyclicBarrier,
                final CountDownLatch countDownLatch,
                final DotStatefulTestJob dotStatefulTestJob) {
            this.cyclicBarrier = cyclicBarrier;
            this.countDownLatch = countDownLatch;
            this.dotStatefulTestJob = dotStatefulTestJob;
        }

        @Override
        public void run() {
            final String thisThreadName = Thread.currentThread().getName();

            try {
                final JobDetail jd = getJobDetail(thisThreadName);
                final Trigger trigger = new TriggerBuilder().jobDetail(jd)
                        .triggerGroupName("StatefulTestJob_trigger_group").build();

                //wait for all threads to schedule jobs concurrently
                cyclicBarrier.await();
                TestJobExecutor.execute(dotStatefulTestJob, jd, trigger);
                countDownLatch.countDown();
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        }
    }

    /**
     * Method used to create a job detail instance
     */
    private JobDetail getJobDetail(String thisThreadName) {
        System.out.println("Triggering job for thread " + thisThreadName);
        final JobDetail jobDetail = new JobDetail(
                "StatefulTestJob-" + UUID.randomUUID().toString(),
                "stateful_test_job_group",
                DotStatefulTestJob.class);
        jobDetail.setJobDataMap(new JobDataMap());
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);
        return jobDetail;
    }
}
