package com.dotcms.api.system.event;

import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;

import javax.websocket.Session;
import java.io.Serializable;

/**
 * Based on the event type there might be some processing needed or not.
 * @author jsanca
 */
public interface SystemEventProcessor extends Serializable {

    /**
     * Process a {@link SystemEvent}
     * @param event {@link SystemEvent}
     * @param session {@link Session}
     * @return SystemEvent
     */
    public SystemEvent process (SystemEvent event, Session session);

} // E:O:F:SystemEventProcessor.
