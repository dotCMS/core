package com.dotcms.concurrent.monitor;

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
 * This is an implementation of the TaskMonitor interface that sends to the client
 * Progress Beans undert the Progress system event type
 * @author jsanca
 */
public class ProgressTaskMonitor implements TaskMonitor {

    private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();

    @Override
    public void onTaskStarted(final Object processId, final Object subProcessId) {

        this.push(new ProgressBean.Builder()
                .processId(processId).
                subProcessId(subProcessId).build());
    }

    private void push (final ProgressBean progressBean) {

        try {

            this.systemEventsAPI.push(new SystemEvent(SystemEventType.PROGRESS,
                    new Payload(progressBean)));
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error pushing system event", e);
        }
    }

    @Override
    public void onTaskProgress(final Object processId, final Object subProcessId, final int progress) {

        this.push(new ProgressBean.Builder()
                .processId(processId).
                subProcessId(subProcessId)
                .progress(progress).build());
    }

    @Override
    public void onTaskCompleted(final Object processId, final Object subProcessId) {

        this.push(new ProgressBean.Builder()
                .processId(processId).
                subProcessId(subProcessId)
                .completed(true).build());
    }

    @Override
    public void onTaskFailed(final Object processId, final Object subProcessId, final Exception error) {

        this.push(new ProgressBean.Builder()
                .processId(processId).
                subProcessId(subProcessId)
                .failed(true)
                .message(error.getMessage()).build());
    }
}
