package com.dotcms.concurrent;

import com.dotmarketing.util.Logger;

/**
 * This is a simple implementation of the TaskMonitor interface that does just logging of the progress.
 * @author jsanca
 */
public class LoggingTaskMonitor implements TaskMonitor {

    @Override
    public void onTaskStarted(final Object processId, final Object subProcessId) {

        Logger.debug(this.getClass(), "Task started, process id: " + processId + " - sub process id:" + subProcessId);
    }

    @Override
    public void onTaskProgress(final Object processId, final Object subProcessId, final int progress) {

        Logger.debug(this.getClass(), "Task progress, process id: " + processId + " - " + progress + "%"  + " - sub process id:" + subProcessId);
    }

    @Override
    public void onTaskCompleted(final Object processId, final Object subProcessId) {

        Logger.debug(this.getClass(), "Task completed, process id: " + processId + " - sub process id:" + subProcessId);
    }

    @Override
    public void onTaskFailed(final Object processId, final Object subProcessId, final Exception error) {

        Logger.error(this.getClass(), "Task failed, process id: " + processId  + " - sub process id:" + subProcessId, error);
    }
}
