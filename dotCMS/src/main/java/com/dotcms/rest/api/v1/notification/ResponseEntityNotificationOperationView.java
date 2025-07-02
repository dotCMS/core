package com.dotcms.rest.api.v1.notification;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for notification operation responses.
 * Contains boolean result and success messages for operations like mark as read, delete.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityNotificationOperationView extends ResponseEntityView<Boolean> {
    public ResponseEntityNotificationOperationView(final Boolean entity) {
        super(entity);
    }
}