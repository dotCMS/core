package com.dotcms.concurrent;

import com.dotmarketing.util.Logger;

/**
 * This is a simple implementation of the TaskMonitor interface that does just logging of the progress.
 * @author jsanca
 */
public class LoggingTaskMonitor implements TaskMonitor {

    @Override
    public void onTaskStarted(final Object processId, String message) {

        Logger.debug(this.getClass(), "Task started: " + processId);
    }

    @Override
    public void onTaskProgress(final Object processId, String message, final int progress) {

        Logger.debug(this.getClass(), "Task progress: " + processId + " - " + progress + "%");
    }

    @Override
    public void onTaskCompleted(final Object processId, String message) {

        Logger.debug(this.getClass(), "Task completed: " + processId);
    }

    @Override
    public void onTaskFailed(Object processId, String message, Exception error) {

        Logger.error(this.getClass(), "Task failed: " + processId, error);
    }
}
