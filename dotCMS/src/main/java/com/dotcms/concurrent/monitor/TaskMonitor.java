package com.dotcms.concurrent.monitor;

/**
 * Interface for monitoring the progress of a task/process.
 
 * @author jsanca
 */
public interface TaskMonitor {

    /**
     * Called when the task starts.
     */
    void onTaskStarted(Object processId, Object subProcessId);

    /**
     * Called to update the progress of the task.
     *
     * @param progress The current progress of the task, represented as a percentage (0-100).
     */
    void onTaskProgress(Object processId, Object subProcessId, int progress);

    /**
     * Called when the task completes successfully.
     */
    void onTaskCompleted(Object processId, Object subProcessId);

    /**
     * Called when the task fails.
     *
     * @param error The error that caused the task to fail.
     */
    void onTaskFailed(Object processId, Object subProcessId, Exception error);
}
