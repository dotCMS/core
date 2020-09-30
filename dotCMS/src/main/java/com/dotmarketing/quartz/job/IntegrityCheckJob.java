package com.dotmarketing.quartz.job;

import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class IntegrityCheckJob extends DotStatefulJob {

    public static final String JOB_NAME = "IntegrityCheckJob";
    public static final String JOB_GROUP = "dotcms_jobs";
    public static final String TRIGGER_NAME = "IntegrityCheckTrigger";
    public static final String TRIGGER_GROUP = "integrity_check_triggers";

    @Override
    public void run(JobExecutionContext jobContext) throws JobExecutionException {
        Logger.info(getClass(), "IntegrityCheckJob has started");
        try {
            final List<PublishingEndPoint> endpoints = APILocator.getPublisherEndPointAPI().getReceivingEndPoints();
            final ExecutorService executorService = Executors.newFixedThreadPool(endpoints.size());
            final List<Future<IntegrityUtil.IntegrityDataExecutionMetadata>> tasks = executorService
                    .invokeAll(endpoints
                            .stream()
                            .filter(endpoint -> endpoint != null && endpoint.getId() != null)
                            .map(endpoint -> new EndpointIntegrityCheckThread(endpoint.getId()))
                            .collect(Collectors.toList()));

        } catch (DotDataException | InterruptedException e) {
            Logger.error(getClass(), "Could not get endpoints to rin integrity check on each");
        }

    }

    public static JobDetail findJobDetail() throws SchedulerException {
        return QuartzUtils.getStandardScheduler().getJobDetail(JOB_NAME, JOB_GROUP);
    }

    public static JobDetail instanceJobDetail() {
        return new JobDetail(JOB_NAME, JOB_GROUP, IntegrityCheckJob.class);
    }

    /**
     * Evaluates if the {@link IntegrityCheckJob} is running.
     *
     * @return true if it does, otherwise false
     */
    public static boolean isJobRunning() {
        try {
            return QuartzUtils.isJobRunning(
                    QuartzUtils.getStandardScheduler(),
                    JOB_NAME,
                    JOB_GROUP,
                    TRIGGER_NAME,
                    TRIGGER_GROUP);
        } catch (SchedulerException e) {
            return false;
        }
    }

}
