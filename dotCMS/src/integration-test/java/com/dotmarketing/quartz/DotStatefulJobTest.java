package com.dotmarketing.quartz;

import static com.dotmarketing.quartz.QuartzUtils.getSequentialScheduler;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class DotStatefulJobTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        getSequentialScheduler().start();
    }

    /**
     * clean up precaution
     * @throws SchedulerException
     */
    private void removeAnyExistingJob() throws SchedulerException {
        final String jobName = DotStatefulJob.getJobName(MyStatefulJob.class);
        final String jobGroupName = DotStatefulJob.getJobGroupName(MyStatefulJob.class);
        QuartzUtils.removeJob(jobName, jobGroupName);
    }

    /**
     * Given scenario: We prep to trigger a bunch of tasks that must be executed sequentially (one at the time) never in parallel.
     * Expected Results: first all triggered tasks must be executed and completed. second their time mark must revel that none of theme overlapped.
     *
     * @throws SchedulerException
     * @throws ParseException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    @Test
    public void Test_Launch_Stateful_Jobs_Verify_They_Dont_Overlap_In_Time()
            throws SchedulerException, ParseException, ClassNotFoundException, InterruptedException {

        //in order to avoid conflicts clean any reference that could have been left.
        removeAnyExistingJob();

        //Now enqueue a few jos .. They will sleep randomly to simulate work.
        for (int i = 1; i <= MyStatefulJob.MAX_THREADS; i++) {
            MyStatefulJob.fireJob(ImmutableMap.of("index", i));
            //Now verify the detail has been added for the present trigger we're introducing
            final Optional<Map<String, Object>> triggerJobDetail = DotStatefulJob
                    .getTriggerJobDetail(MyStatefulJob.class);
            assertNotNull(triggerJobDetail);
            assertTrue(triggerJobDetail.isPresent());
            final Map<String, Object> detail = triggerJobDetail.get();
            assertNotNull(detail);
            //The size of the detail should have grown as we insert new triggers.
            assertEquals(detail.size(), i);
        }

        //Verify at least 1 job has been launched.
        final Optional<JobExecutionContext> jobExecutionContext = getJobExecutionContext();
        assertTrue(jobExecutionContext.isPresent());

        //Now lets wait for the all the threads to complete.
        MyStatefulJob.countDownLatch.await();

        //Now lets revise the execution times. Since they were supposed to run sequentially the should never overlap.
        final HashSet<MyStatefulJob> myStatefulJobs = new HashSet<>(MyStatefulJob.finishedJobs);
        final Iterator<MyStatefulJob> iterator = myStatefulJobs.iterator();
        while(iterator.hasNext()){
            final MyStatefulJob myStatefulJob = iterator.next();
            iterator.forEachRemaining(job -> {
                //They should never overlap.
                assertFalse(myStatefulJob.getTimeRange().overlaps(job.getTimeRange()));
            });
        }
    }

    /**
     * This gets you the execution context for the given job-name.
     * @return
     */
    private Optional<JobExecutionContext> getJobExecutionContext(){
        final String jobName = DotStatefulJob.getJobName(MyStatefulJob.class);
        try {
            final Scheduler sequentialScheduler = getSequentialScheduler();
            @SuppressWarnings("unchecked")
            final List<JobExecutionContext> executingJobs = sequentialScheduler.getCurrentlyExecutingJobs();
            return executingJobs.stream().filter(jobExecutionContext -> {
                final JobDetail jobDetail = jobExecutionContext.getJobDetail();
                return jobDetail != null && jobName.equals(jobDetail.getName());
            }).findFirst();
        } catch (Exception e) {
            Logger.error(DotStatefulJobTest.class, "Error retrieving execution context. " , e);
        }
        return Optional.empty();
    }

    /**
     * additional struct to follow execution time lapse.
     */
    public static class LocalTimeRange {

        private final LocalTime from;
        private final LocalTime to;

        LocalTimeRange(final LocalTime from, final LocalTime to) {
            requireNonNull(from, "from must not be null");
            requireNonNull(to, "to must not be null");
            this.from = from;
            this.to = to;
        }

        boolean overlaps(final LocalTimeRange other) {
            requireNonNull(other, "other must not be null");
            return isBetween(other.from, this.from, this.to)
                    || isBetween(other.to, this.from, this.to)
                    || isBetween(this.from, other.from, other.to)
                    || isBetween(this.to, other.from, other.to);
        }

        private static boolean isBetween(final LocalTime time, final LocalTime from, final LocalTime to) {
            if (from.isBefore(to)) { // same day
                return from.isBefore(time) && time.isBefore(to);
            } else { // spans to the next day.
                return from.isBefore(time) || time.isBefore(to);
            }
        }

        public static LocalTimeRange of(final LocalTime from, final LocalTime to){
           return new LocalTimeRange(from, to);
        }
    }

    /**
     * Given scenario: We test that the internal methods generate properly formed names
     * Expected Results: The call to the methods match the given patterns
     */
    @Test
    public void Test_Stateful_Job_Utility_Methods(){
        assertEquals("MyStatefulJob",DotStatefulJob.getJobName(MyStatefulJob.class));
        assertEquals("MyStatefulJob_Group",DotStatefulJob.getJobGroupName(MyStatefulJob.class));
        assertEquals("MyStatefulJob_Trigger_Group",DotStatefulJob.getTriggerGroupName(MyStatefulJob.class));
        assertTrue(DotStatefulJob.nextTriggerName(MyStatefulJob.class).startsWith("MyStatefulJob_Trigger_"));
    }

}
