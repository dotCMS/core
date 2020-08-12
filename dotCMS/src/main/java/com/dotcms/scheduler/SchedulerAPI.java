package com.dotcms.scheduler;

import java.time.Duration;
import java.time.Instant;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;

public class SchedulerAPI {

    private static class SingletonHolder {

        private static final SchedulerAPI INSTANCE = new SchedulerAPI();
    }

    private final Scheduler scheduler;

    private final SchedulerName schedulerName = new SchedulerName() {

        @Override
        public String getName() {
            return APILocator.getServerAPI().readServerId();
        }
    };


    public static SchedulerAPI getInstance() {
        return SingletonHolder.INSTANCE;
    }


    private OneTimeTask<DotTask> oneTime =
                    Tasks.oneTime("one-time-task", DotTask.class).execute((inst, ctx) -> {
                        inst.getData().runTask(inst);
                    });


    private SchedulerAPI() {


        this.scheduler = Scheduler.create(DbConnectionFactory.getDataSource(), oneTime)
                        .threads(Config.getIntProperty("SCHEDULER_NUMBER_OF_THREADS", 10))
                        .schedulerName(schedulerName).deleteUnresolvedAfter(Duration.ofDays(1)).build();

        this.scheduler.start();


        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Logger.info(this.getClass(), "Shutting down scheduler");
                scheduler.stop();
            }
        });


    }


    public void startScheduler() {


        getInstance().scheduler.start();

    }

    public void stopScheduler() {
        getInstance().scheduler.stop();

    }


    public void scheduleOneTimeTask(final DotTask task, Duration duration) {

        final TaskInstance<DotTask> myTask =
                        oneTime.instance(task.name() + "_" + UUIDGenerator.shorty(), task);

        getInstance().scheduler.schedule(myTask, Instant.now().plusMillis(duration.toMillis()));

    }


    public void scheduleOneTimeTask(final DotTask task) {
        scheduleOneTimeTask(task, task.initialDelay());
    }

    
    public void rescheduleOneTimeTask(final TaskInstance<DotTask> myTask, Duration delay) {
        getInstance().scheduler.reschedule(myTask, Instant.now().plusMillis(delay.toMillis()));
    }

    public <T> void scheduleRecurringTask(final DotTask task, final String cronExpression) {


        CronSchedule cron = new CronSchedule(cronExpression,
                        APILocator.getCompanyAPI().getDefaultCompany().getTimeZone().toZoneId());


        RecurringTask<DotTask> cronTask =
                        Tasks.recurring(task.name(), cron, DotTask.class).execute((inst, ctx) -> {
                            task.execute();
                        });

        getInstance().scheduler.schedule(cronTask.instance(task.name(), task),
                        Instant.now().plusMillis(task.initialDelay().toMillis()));
    }


    public <T> void scheduleRecurringTask(final DotTask task, final Duration runEvery) {

        final RecurringTask<DotTask> delayTask =
                        Tasks.recurring(task.name(), FixedDelay.of(runEvery), DotTask.class)
                                        .execute((inst, ctx) -> {
                                            task.execute();
                                        });

        getInstance().scheduler.schedule(delayTask.instance(task.name(), task),
                        Instant.now().plusMillis(task.initialDelay().toMillis()));

    }

    @CloseDBIfOpened
    public boolean shouldIRun(String id, final DotTask task) {

        if (!(task instanceof DotSyncronizedTask)) {
            return true;
        }

        String clazz = task.getClass().getName();
        final String topTask = new DotConnect()
                        .setMaxRows(1)
                        .setSQL(
            "select task_instance as test from scheduled_tasks where task_instance like ? and picked=? order by task_instance")
            .addParam(clazz + "%")
            .addParam(true)
            .getString("test");


        return id.equals(topTask);

    }

}
