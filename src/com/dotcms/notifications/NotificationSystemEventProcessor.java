package com.dotcms.notifications;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventProcessor;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.UserNotificationPair;
import com.dotcms.notifications.view.NotificationView;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.liferay.portal.model.User;

import javax.websocket.Session;

/**
 * Decorates the {@link com.dotcms.api.system.event.SystemEventType}.NOTIFICATION in order to convert the {@link com.dotcms.notifications.bean.Notification}
 * to {@link com.dotcms.notifications.view.NotificationView}.
 * @author jsanca
 */
public class NotificationSystemEventProcessor implements SystemEventProcessor {

    private static final NotificationConverter CONVERTER =
            new NotificationConverter();


    @Override
    public SystemEvent process(final SystemEvent event,
                               final Session session) {

        final Payload payload      = event.getPayload();
        final Notification notification = (Notification) payload.getData();
        final User user            = (session instanceof SessionWrapper)?
                SessionWrapper.class.cast(session).getUser():null;

        final NotificationView notificationView =
                CONVERTER.convert(new UserNotificationPair(user, notification));

        return new SystemEvent(event.getId(), event.getEventType(),
                new Payload(notificationView, payload.getVisibility(), payload.getVisibilityId()),
                event.getCreationDate());
    } // process.
} // E:O:F:NotificationSystemEventProcessor.
