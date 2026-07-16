package com.dotcms.rest.api.v1.notification;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for notification count responses.
 * Contains the count of new/unread notifications.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityNotificationCountView extends ResponseEntityView<Long> {
    public ResponseEntityNotificationCountView(final Long entity) {
        super(entity);
    }
}