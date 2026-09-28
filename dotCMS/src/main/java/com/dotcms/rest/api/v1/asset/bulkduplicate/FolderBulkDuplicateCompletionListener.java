package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;

// RED STUB (#37062 T061): notifies nobody; replaced in T062. Not registered, so it never runs.
public class FolderBulkDuplicateCompletionListener implements EventSubscriber<JobCompletedEvent> {

    public FolderBulkDuplicateCompletionListener() {
        this(APILocator.getSystemEventsAPI(), APILocator.getNotificationAPI(),
                APILocator.getUserAPI());
    }

    public FolderBulkDuplicateCompletionListener(final SystemEventsAPI systemEventsAPI,
            final NotificationAPI notificationAPI, final UserAPI userAPI) {
    }

    @Override
    public void notify(final JobCompletedEvent event) {
        // RED STUB
    }

    public SystemEventType eventType() {
        return null;
    }
}
