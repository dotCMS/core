package com.dotcms.concurrent;

/**
 * Interface for monitoring the progress of a task/process.
 
 * @author jsanca
 */
public interface TaskMonitor {

    /**
     * Called when the task starts.
     */
    void onTaskStarted(Object processId);

    /**
     * Called to update the progress of the task.
     *
     * @param progress The current progress of the task, represented as a percentage (0-100).
     */
    void onTaskProgress(Object processId, int progress);

    /**
     * Called when the task completes successfully.
     */
    void onTaskCompleted(Object processId);

    /**
     * Called when the task fails.
     *
     * @param error The error that caused the task to fail.
     */
    void onTaskFailed(Object processId, Exception error);
}
