package com.dotcms.concurrent.scheduler;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Default implementation
 * @author jsanca
 */
public class DotSchedulerImpl implements DotScheduler {

    private static final Pattern FIXED_DELAY_PATTERN                      = Pattern.compile("^FIXED_DELAY\\|(\\d+)s$");
    private static final Pattern DAILY_PATTERN_WITH_TIMEZONE              = Pattern.compile("^DAILY\\|((\\d{2}:\\d{2})(,\\d{2}:\\d{2})*)(\\|(.+))?$");;
    private static final String SELECT_TASK_INSTANCE                      = "select task_instance as task_instance from scheduled_tasks where task_instance like ? and picked=? order by execution_time, task_instance";
    private final Map<String, DotTask>          onceTaskMap               = new ConcurrentHashMap<>();
    private final Map<String, DotTaskRecurring> recurringStatefulTaskMap  = new ConcurrentHashMap<>();
    private final Map<String, DotTaskRecurring> recurringStatelessTaskMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture> statelessFutureTaskMap     = new ConcurrentHashMap<>();
    private final AtomicBoolean                 started                   = new AtomicBoolean(false);
    private final AtomicReference<Scheduler>    schedulerRef              = new AtomicReference<>(null);
    private final OneTimeTask<DotTask> oneTimeTaskTemplate                = Tasks.oneTime("one-time-task", DotTask.class).execute(this::executeOnce);
    private final AtomicReference<ScheduledThreadPoolExecutor> scheduledThreadPoolExecutorRef = new AtomicReference<>(null);

    public DotSchedulerImpl() {

        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
                Logger.info(DotScheduler.class, ()->"Shutting down scheduler");
                DotSchedulerImpl.this.stop();
            }));
    }

    @Override
    public DotScheduler registerStatefulTask(final DotTask... dotTasks) {

        Stream.of(dotTasks).forEach(dotTask -> this.onceTaskMap.put(dotTask.getInstanceId(), dotTask));
        return this;
    }

    @Override
    public DotScheduler registerStatefulRecurringTask(final DotTaskRecurring... dotTaskRecurrings) {

        Stream.of(dotTaskRecurrings).forEach(dotTask -> this.recurringStatefulTaskMap.put(dotTask.getInstanceId(), dotTask));
        return this;
    }

    @Override
    public DotScheduler registerStatelessRecurringTask(final DotTaskRecurring... dotTaskRecurrings) {

        Stream.of(dotTaskRecurrings).forEach(dotTask -> this.recurringStatelessTaskMap.put(dotTask.getInstanceId(), dotTask));
        return this;
    }

    @Override
    public DotTask unregisterTask(final String instanceId) {

        if (this.onceTaskMap.containsKey(instanceId)) {

            return this.onceTaskMap.remove(instanceId);
        } else if (this.recurringStatefulTaskMap.containsKey(instanceId)) {

            return this.recurringStatefulTaskMap.remove(instanceId);
        } else if (this.recurringStatelessTaskMap.containsKey(instanceId)) {

            return this.recurringStatelessTaskMap.remove(instanceId);
        }

        return null;
    }

    @Override
    public void start() {

        Logger.info(DotScheduler.class, ()->"Starting scheduler");
        final Scheduler scheduler = this.buildScheduler();
        scheduler.start();
        this.started.set(true);
    }

    @Override
    public void restart() {

        this.stop();
        this.start();
    }

    @Override
    public void fire(final String instanceId) {

        if (this.onceTaskMap.containsKey(instanceId)) {

            this.fire(this.onceTaskMap.get(instanceId), this.onceTaskMap.get(instanceId).getInitialDelay());
        } else {

            throw new NotTaskFoundException("The DotTask: " + instanceId + ", does not exists");
        }
    }

    @Override
    public void fire(final String instanceId, final Duration duration) {

        if (this.onceTaskMap.containsKey(instanceId)) {

            this.fire(this.onceTaskMap.get(instanceId), this.onceTaskMap.get(instanceId).getInitialDelay());
        } else {

            throw new NotTaskFoundException("The DotTask: " + instanceId + ", does not exists");
        }
    }

    private void fire(final DotTask dotTask, final Duration duration) {

        Logger.info(this.getClass(),
                ()->"Scheduling Task: " + dotTask + " in " + (duration.toMillis() / 1000) + "s");

        final TaskInstance<DotTask> myTask =
                oneTimeTaskTemplate.instance(dotTask.getClass().getName() + "_" + dotTask.getInstanceId(), dotTask);

        this.schedulerRef.get().schedule(myTask, Instant.now().plusMillis(duration.toMillis()));
    }

    @Override
    public void cancel(final String instanceId) {

        final DotTask dotTask = this.onceTaskMap.getOrDefault(instanceId,
                this.recurringStatefulTaskMap.getOrDefault(instanceId, null));

        if (null != dotTask) {

            final TaskInstance<DotTask> myTask =
                    oneTimeTaskTemplate.instance(dotTask.getClass().getName() + "_" + dotTask.getInstanceId(), dotTask);

            this.schedulerRef.get().cancel(myTask);
        } else {

            if (this.statelessFutureTaskMap.containsKey(instanceId) &&
                    !this.statelessFutureTaskMap.get(instanceId).isCancelled()) {

                // if running, will wait until finish to cancel.
                this.statelessFutureTaskMap.get(instanceId).cancel(false);
            } else {

                throw new NotTaskFoundException("The DotTask: " + instanceId + ", does not exists");
            }
        }
    }

    @Override
    public SchedulerStatus status() {

        return this.started.get()? SchedulerStatus.RUNNING: SchedulerStatus.STOPPED;
    }

    @Override
    public TaskStatus status(final String instanceId) {

        if (this.statelessFutureTaskMap.containsKey(instanceId)) {

            if (this.statelessFutureTaskMap.get(instanceId).isCancelled()) {

                return TaskStatus.CANCELED;
            } else if (!this.statelessFutureTaskMap.get(instanceId).isDone()) {

                return !this.statelessFutureTaskMap.get(instanceId).isDone()?
                        TaskStatus.RUNNING:TaskStatus.STOPPED;
            }
        }

        // todo: check the stateful tasks

        return TaskStatus.UNKNOWN;
    }

    /*
     * This tasks checks if the task is not already running, if it is, the call will wait (queue) for some seconds and try to be
     * execute again!
     */
    private void executeOnce(final TaskInstance<DotTask> taskInstance, final ExecutionContext executionContext) {

        if (this.shouldRun(taskInstance.getId(), taskInstance.getData())) {

            Logger.info(DotScheduler.class, ()-> "Running: " +  taskInstance.getData().getClass());
            taskInstance.getData().getRunnable().run();
        } else {

            final int rescheduleDelay = Config.getIntProperty(DOTSYNCRONIZEDTASK_RESCHEDULE_DELAY_SEC, 15);
            Logger.info(DotScheduler.class, ()-> "Rescheduling: " + taskInstance.getData().getClass()
                    + " for " + rescheduleDelay + "s in the future");
            this.fire(taskInstance.getData(), Duration.ofSeconds(rescheduleDelay));
        }
    }

    /*
     * This tasks checks if the task is not already running, if it is, the call will wait (queue) for some seconds and try to be
     * execute again!
     */
    private void executeRecurring(final TaskInstance<DotTaskRecurring> taskInstance, final ExecutionContext executionContext) {

        Logger.info(DotScheduler.class, ()-> "Running recurring stateful task: " + taskInstance.getData().getClass()
                + " at " + new Date());
        taskInstance.getData().getRunnable().run();
    }

    private Scheduler buildScheduler() {

        // adding the recurring stateless tasks
        final int corePoolSize = Config.getIntProperty(SCHEDULER_COREPOOLSIZE, 10);
        final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
        this.scheduledThreadPoolExecutorRef.set(scheduledThreadPoolExecutor);
        this.recurringStatelessTaskMap.values().forEach(dotTask -> this.fireRecurringStatelessTask(scheduledThreadPoolExecutor, dotTask));

        // adding the stateful once and recurring tasks
        final List<Task<?>> knownTasks = new ArrayList<>();
        knownTasks.add(oneTimeTaskTemplate);
        this.recurringStatefulTaskMap.values().forEach(dotTaskRecurring -> this.addRecurringStatefulTask(knownTasks, dotTaskRecurring));
        this.schedulerRef.set(Scheduler.create(DbConnectionFactory.getDataSource(), knownTasks)
                .threads(Config.getIntProperty(SCHEDULER_NUMBER_OF_THREADS, 10))
                .schedulerName(()->APILocator.getServerAPI().readServerId())
                .enableImmediateExecution()
                .deleteUnresolvedAfter(Duration.ofDays(1))
                .build());

        return schedulerRef.get();
    }

    private void addRecurringStatefulTask(final List<Task<?>> knownTasks, final DotTaskRecurring dotTaskRecurring) {

        if (FIXED_DELAY_PATTERN.matcher(dotTaskRecurring.getCronExpression()).matches() ||
                DAILY_PATTERN_WITH_TIMEZONE.matcher(dotTaskRecurring.getCronExpression()).matches()) {

            final RecurringTask<DotTaskRecurring> recurringTask = Tasks.recurring(
                    dotTaskRecurring.getClass().getName(),
                    Schedules.parseSchedule(dotTaskRecurring.getCronExpression()), DotTaskRecurring.class)
                    .execute(this::executeRecurring);
            knownTasks.add(recurringTask);
        } else { // cron expression

            final RecurringTask<DotTaskRecurring> recurringTask = Tasks.recurring(
                    dotTaskRecurring.getClass().getName(),
                    new CronSchedule(dotTaskRecurring.getCronExpression()), DotTaskRecurring.class)
                    .execute(this::executeRecurring);
            knownTasks.add(recurringTask);
        }
    }

    private void fireRecurringStatelessTask (final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor, final DotTaskRecurring dotTask) {

        final Duration initialDelay   = dotTask.getInitialDelay();
        final long     recurringDelay = toSeconds (dotTask.getCronExpression());
        this.statelessFutureTaskMap.put(dotTask.getInstanceId(),
                scheduledThreadPoolExecutor.scheduleWithFixedDelay(
                        dotTask.getRunnable(), initialDelay.getSeconds(), recurringDelay, TimeUnit.SECONDS));
    }

    private long toSeconds(final String cronExpression) {

        final long defaultRecurringSeconds = Config.getLongProperty("DOTSCHEDULER__RECURRING_SECONDS", 15l);
        final Matcher matcher              = FIXED_DELAY_PATTERN.matcher(cronExpression);
        return matcher.matches()?
                ConversionUtils.toLong(matcher.group(1), defaultRecurringSeconds):
                defaultRecurringSeconds;
    }

    private void stop () {

        final Scheduler scheduler = this.schedulerRef.get();
        if (null != scheduler) {
            scheduler.stop();
        }

        final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = this.scheduledThreadPoolExecutorRef.get();
        if (null != scheduledThreadPoolExecutor && !scheduledThreadPoolExecutor.isTerminated()
                && !scheduledThreadPoolExecutor.isShutdown()) {

            scheduledThreadPoolExecutor.shutdownNow();
        }
    }

    /**
     * If the task instanceId is not already running, returns true.
     * @param instanceId {@link String} identifier of the task instance
     * @param task {@link DotTask} task to check the class name
     * @return boolean returns true if should runs
     */
    @CloseDBIfOpened
    private boolean shouldRun(final String instanceId, final DotTask task) {

        final String clazz   = task.getClass().getName();
        final String topTask = new DotConnect()
                .setMaxRows(1)
                .setSQL(SELECT_TASK_INSTANCE)
                .addParam(clazz + "%")
                .addParam(true)
                .getString("task_instance");

        return instanceId.equals(topTask);
    }
}
