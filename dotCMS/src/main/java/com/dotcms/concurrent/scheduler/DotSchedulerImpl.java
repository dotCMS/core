package com.dotcms.concurrent.scheduler;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Config;
import com.github.kagkarlsson.scheduler.Scheduler;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Pattern FIXED_DELAY_PATTERN = Pattern.compile("^FIXED_DELAY\\|(\\d+)s$");
    private final Map<String, DotTask> onceTaskMap                        = new ConcurrentHashMap<>();
    private final Map<String, DotTaskRecurring> recurringStatefulTaskMap  = new ConcurrentHashMap<>();
    private final Map<String, DotTaskRecurring> recurringStatelessTaskMap = new ConcurrentHashMap<>();
    private final AtomicBoolean        started                            = new AtomicBoolean(false);
    private final AtomicReference<Scheduler> schedulerRef                 = new AtomicReference<>(null);
    private final AtomicReference<ScheduledThreadPoolExecutor> scheduledThreadPoolExecutorRef = new AtomicReference<>(null);

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

        final Scheduler scheduler = this.buildScheduler();
        scheduler.start();
        this.started.set(true);
    }

    @Override
    public void restart() {

        this.stop();
        final Scheduler scheduler = this.buildScheduler();
        scheduler.start();
        this.started.set(true);
    }

    @Override
    public void fire(final String instanceId) {

    }

    @Override
    public void fire(final String instanceId, final Duration duration) {

    }

    @Override
    public void cancel(final String instanceId) {

    }

    @Override
    public SchedulerStatus status() {

        return this.started.get()? SchedulerStatus.RUNNING: SchedulerStatus.STOPPED;
    }

    @Override
    public TaskStatus status(final String instanceId) {

        return null;
    }

    private Scheduler buildScheduler() {

        // add the recurring stateless tasks
        final int corePoolSize = Config.getIntProperty(SCHEDULER_COREPOOLSIZE, 10);
        final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
        this.scheduledThreadPoolExecutorRef.set(scheduledThreadPoolExecutor);
        this.recurringStatelessTaskMap.values().forEach(dotTask -> this.fireRecurringStatelessTask(scheduledThreadPoolExecutor, dotTask));

        // todo: create all tasks based on the maps
        
        return null;
    }

    private void fireRecurringStatelessTask (final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor, final DotTaskRecurring dotTask) {

        final Duration initialDelay   = dotTask.getInitialDelay();
        final long     recurringDelay = toSeconds (dotTask.getCronExpression());
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(
                dotTask.getRunnable(), initialDelay.getSeconds(), recurringDelay, TimeUnit.SECONDS);
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
                .setSQL("select task_instance as task_instance from scheduled_tasks where task_instance like ? and picked=? order by execution_time, task_instance")
                .addParam(clazz + "%")
                .addParam(true)
                .getString("task_instance");


        return instanceId.equals(topTask);

    }
}
