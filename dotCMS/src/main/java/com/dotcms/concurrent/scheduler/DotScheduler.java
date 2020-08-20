package com.dotcms.concurrent.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;

import java.time.Duration;

/**
 * Encapsulates the logic to register and trigger stateful and stateless jobs, Recurring or on demand
 * @author jsanca
 */
public interface DotScheduler {

    /**
     * Thread size for the stateless scheduler, 10 threads by defaults
     */
    String SCHEDULER_COREPOOLSIZE      = "SCHEDULER_CORE_POOL_SIZE";

    /**
     * Number of thead for the stateful scheduler, 10 threads by defaults
     */
    String SCHEDULER_NUMBER_OF_THREADS = "SCHEDULER_NUMBER_OF_THREADS";

    /**
     * Seconds to way when the task is actually running, 15 seconds by default
     */
    String DOT_SYNCHRONIZED_TASK_RESCHEDULE_DELAY_SEC = "DOT_SYNCHRONIZED_TASK_RESCHEDULE_DELAY_SECONDS";

    /**
     * Register a single task that can be fire by demand, use {@link #fire(DotTask)}
     *
     * @param tasks {@link DotTask} array
     * @return DotScheduler
     */
    DotScheduler registerStatefulTask           (final DotTask... tasks);

    /**
     * Register a recurring task for stateful (only one execution per cluster), it will be executed based on the cron expression on the {@link DotRecurringTask}
     * @param recurringTasks {@link DotRecurringTask} array
     * @return DotScheduler
     */
    DotScheduler registerStatefulRecurringTask  (final DotRecurringTask... recurringTasks);

    /**
     * Register a recurring task for stateless (recurring execution on each node in a server),
     * it will be executed based on the cron expression on the {@link DotRecurringTask}
     * @param recurringTasks
     * @return DotScheduler
     */
    DotScheduler registerStatelessRecurringTask (final DotRecurringTask... recurringTasks);

    /**
     * Unregister a task
     * @param instanceId {@link String} identifier of the task instance
     * @return DotTask null if does not exists
     */
    DotTask       unregisterTask                 (final String instanceId);

    /**
     * Starts the scheduler, if already start won't do anything.
     * Note: it will be shutdown when the server shutdowns
     */
    void        start   ();

    /**
     * Restart the scheduler, it is useful when a new task is registered or unregistered once the scheduler already started
     */
    void        restart ();

    void executeOnce(final TaskInstance<DotTask> taskInstance, final ExecutionContext executionContext);

    boolean isDotTaskRunning(final String instanceId);

    /**
     * Fires Task, if the task does not exists an NotTaskFoundException will be thrown
     * @param dotTask {@link DotTask} dot task
     */
    void        fire(final DotTask dotTask);

    /**
     * Fires Task, if the task does not exists an NotTaskFoundException will be throw
     * @param dotTask {@link DotTask} dot task
     * @param delay {@link Duration} delay to wait for the task execution
     */
    void        fire(final DotTask dotTask, final Duration delay);

    /**
     * Fires Task (again), if the task does not exists an NotTaskFoundException will be throw
     * @param dotTask {@link DotTask} dot task
     * @param delay {@link Duration} delay to wait for the task execution
     */
    void        fireAgain(final DotTask dotTask, final Duration delay);

    // todo: check if when re-fire is needed with a delay, instead of the sending the instanceId, we need the actual instance (the DotTask)

    /**
     * Cancels a task,  if the task does not exists an NotTaskFoundException will be throw
     * @param instanceId {@link String} identifier of the task instance
     */
    void        cancel(final String instanceId);

    /**
     * Returns the status of scheduler
     * @return SchedulerStatus
     */
    SchedulerStatus status();

    /**
     * Returns the status of a task
     * @param instanceId {@link String} identifier of the task instance
     * @return TaskStatus
     */
    TaskStatus taskStatus(final String instanceId);

    /**
     * Status for the Scheduler
     */
    enum SchedulerStatus {
        STOPPED, RUNNING
    }

    /**
     * Status for a task
     */
    enum TaskStatus {
        STOPPED, RUNNING, FAILED, CANCELED, UNKNOWN
    }

    /**
     * Returned when the task requested does not exists
     */
    class NotTaskFoundException extends RuntimeException {

        public NotTaskFoundException(final String message) {
            super(message);
        }
    }
}
