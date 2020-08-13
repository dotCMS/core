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
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

public class SchedulerAPI {

    private static class SingletonHolder {
        private static final SchedulerAPI INSTANCE = new SchedulerAPI();
    }

    private final Scheduler scheduler;


    public static SchedulerAPI getInstance() {
        return SingletonHolder.INSTANCE;
    }


    private final OneTimeTask<DotTask> oneTime =
                    Tasks.oneTime("one-time-task", DotTask.class).execute((inst, ctx) -> {
                        inst.getData().runTask(inst.getId(), inst.getData());
                    });


    private SchedulerAPI() {


        this.scheduler = Scheduler.create(DbConnectionFactory.getDataSource(), oneTime)
                        .threads(Config.getIntProperty("SCHEDULER_NUMBER_OF_THREADS", 10))
                        .schedulerName(new SchedulerName() {
                            @Override
                            public String getName() {
                                return APILocator.getServerAPI().readServerId();
                            }
                        })
                        .enableImmediateExecution()
                        .deleteUnresolvedAfter(Duration.ofDays(1))
                        .build();

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

        Logger.info(this.getClass(),
                        "Scheduling Task: " + task + " in " + (duration.toMillis() / 1000) + "s");


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


    /**
     * If the task that is passed in is a DotSyncronizedTask, then only one
     * instance of its class should be allowed to run at any given time.
     * @param id
     * @param task
     * @return
     */
    @CloseDBIfOpened
    public boolean shouldIRun(String id, final DotTask task) {

        if (!(task instanceof DotSyncronizedTask)) {
            return true;
        }

        String clazz = task.getClass().getName();
        final String topTask = new DotConnect()
                        .setMaxRows(1)
                        .setSQL("select task_instance as task_instance from scheduled_tasks where task_instance like ? and picked=? order by execution_time, task_instance")
                        .addParam(clazz + "%")
                        .addParam(true)
                        .getString("task_instance");


        return id.equals(topTask);

    }

}
