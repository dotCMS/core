package com.dotcms.concurrent;

import com.dotmarketing.util.Logger;

/**
 * This is a simple implementation of the TaskMonitor interface that does just logging of the progress.
 * @author jsanca
 */
public class LoggingTaskMonitor implements TaskMonitor {

    @Override
    public void onTaskStarted(final Object processId, final String message) {

        Logger.debug(this.getClass(), "Task started: " + processId + " - msg:" + message);
    }

    @Override
    public void onTaskProgress(final Object processId, final String message, final int progress) {

        Logger.debug(this.getClass(), "Task progress: " + processId + " - " + progress + "%"  + " - msg:" + message);
    }

    @Override
    public void onTaskCompleted(final Object processId, final String message) {

        Logger.debug(this.getClass(), "Task completed: " + processId + " - msg:" + message);
    }

    @Override
    public void onTaskFailed(final Object processId, final String message, final Exception error) {

        Logger.error(this.getClass(), "Task failed: " + processId  + " - msg:" + message, error);
    }
}
