package com.dotcms.rest.api.v1.event;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.api.system.event.SystemEvent;
import java.util.List;

/**
 * Entity View for system events collection responses.
 * Contains lists of system events for real-time notifications and updates.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySystemEventsView extends ResponseEntityView<List<SystemEvent>> {
    public ResponseEntitySystemEventsView(final List<SystemEvent> entity) {
        super(entity);
    }
}