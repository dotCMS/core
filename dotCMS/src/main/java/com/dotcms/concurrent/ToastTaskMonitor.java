package com.dotcms.concurrent;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

/**
 * This is a simple implementation of the TaskMonitor interface that does toast messages of the progress.
 * @author jsanca
 */
public class ToastTaskMonitor implements TaskMonitor {

    private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();

    @Override
    public void onTaskStarted(final Object processId, final Object subProcessId) {

        this.sendMessage("Task started, process id: " + processId + " - sub process id:" + subProcessId);
    }

    private void sendMessage (final String message) {

        try {

            final SystemMessage systemMessage   = new SystemMessageBuilder()
                    .setMessage(message)
                    .setLife(15000) // 15 secs for testing
                    .setSeverity(MessageSeverity.INFO).create();
            this.systemEventsAPI.push(new SystemEvent(SystemEventType.MESSAGE,
                    new Payload(systemMessage, Visibility.GLOBAL, null)));
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error pushing system event", e);
        }
    }

    @Override
    public void onTaskProgress(final Object processId, final Object subProcessId, final int progress) {

        this.sendMessage("Task progress, process id: " + processId + " - sub process id:" + subProcessId + " - " + progress + "%");
    }

    @Override
    public void onTaskCompleted(final Object processId, final Object subProcessId) {

        this.sendMessage("Task completed, process id: " + processId + " - sub process id:" + subProcessId);
    }

    @Override
    public void onTaskFailed(final Object processId, final Object subProcessId, final Exception error) {

        this.sendMessage("Task failed, process id: " + processId  + " - sub process id:" + subProcessId + " - " + error.getMessage());
    }
}
