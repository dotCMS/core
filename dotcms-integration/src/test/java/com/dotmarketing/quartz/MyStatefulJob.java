package com.dotmarketing.quartz;

import com.dotmarketing.quartz.DotStatefulJobTest.LocalTimeRange;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import java.io.Serializable;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

public class MyStatefulJob extends DotStatefulJob {

    static final int MAX_THREADS = 10;
    static CountDownLatch countDownLatch = new CountDownLatch(MAX_THREADS);
    static Set<MyStatefulJob> finishedJobs = Collections.synchronizedSet(new HashSet<>());

    static void init() {
        countDownLatch = new CountDownLatch(MAX_THREADS);
        finishedJobs = Collections.synchronizedSet(new HashSet<>());
    }

    private LocalTime startedAt;
    private LocalTime finishedAt;

    @Override
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        startedAt = LocalTime.now();
        Logger.info(DotStatefulJobTest.class, ":: Started ::");
        DateUtil.sleep(randInt(2000, 4000));
        Logger.info(DotStatefulJobTest.class, ":: Done ::");
        finishedAt = LocalTime.now();
        finishedJobs.add(this);
        countDownLatch.countDown();
    }

    LocalTimeRange getTimeRange() {
        return LocalTimeRange.of(startedAt, finishedAt);
    }

    /**
     * fire job
     * @param nextExecutionData
     * @throws ParseException
     * @throws SchedulerException
     * @throws ClassNotFoundException
     */
    static void fireJob(final Map<String, Serializable> nextExecutionData)
            throws ParseException, SchedulerException, ClassNotFoundException {
        enqueueTrigger(nextExecutionData, MyStatefulJob.class);
    }

    /**
     * random int range generator
     * @param min
     * @param max
     * @return
     */
    private int randInt(final int min, final int max) {
        final Random rand = new Random(System.currentTimeMillis());
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return rand.nextInt((max - min) + 1) + min;
    }
}
