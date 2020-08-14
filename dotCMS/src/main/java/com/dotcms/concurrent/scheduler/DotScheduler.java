package com.dotcms.concurrent.scheduler;

import java.time.Duration;

/**
 * Encapsulates the logic to register and trigger stateful and stateless jobs, Recurring or on demand
 * @author jsanca
 */
public interface DotScheduler {


    String SCHEDULER_COREPOOLSIZE = "SCHEDULER_CORE_POOL_SIZE";

    /**
     * Register a single task that can be fire by demand, use {@link #fire(String)}
     *
     * @param dotTasks {@link DotTask} array
     * @return DotScheduler
     */
    DotScheduler registerStatefulTask           (final DotTask... dotTasks);

    /**
     * Register a recurring task for stateful (only one execution per cluster), it will be executed based on the cron expression on the {@link DotTaskRecurring}
     * @param dotTaskRecurrings {@link DotTaskRecurring} array
     * @return DotScheduler
     */
    DotScheduler registerStatefulRecurringTask  (final DotTaskRecurring... dotTaskRecurrings);

    /**
     * Register a recurring task for stateless (recurring execution on each node in a server),
     * it will be executed based on the cron expression on the {@link DotTaskRecurring}
     * @param dotTaskRecurrings
     * @return DotScheduler
     */
    DotScheduler registerStatelessRecurringTask (final DotTaskRecurring... dotTaskRecurrings);

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

    /**
     * Fires Task, if the task does not exists an NotTaskFoundException will be throwed
     * @param instanceId {@link String} identifier of the task instance
     */
    void        fire(final String instanceId);

    /**
     * Fires Task, if the task does not exists an NotTaskFoundException will be throw
     * @param instanceId {@link String} identifier of the task instance
     * @param delay {@link Duration} delay to wait for the task execution
     */
    void        fire(final String instanceId, final Duration delay);

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
    TaskStatus      status(final String instanceId);

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
        STOPPED, RUNNING, FAILED, CANCELED, NOT_FOUND
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
