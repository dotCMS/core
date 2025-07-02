package com.dotcms.rest.api.v1.notification;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for notification list responses.
 * Contains notification data including total counts and notification items.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityNotificationListView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityNotificationListView(final Map<String, Object> entity) {
        super(entity);
    }
}