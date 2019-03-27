package com.dotcms.api.system.event;

import com.liferay.portal.model.User;

import java.io.Serializable;

/**
 * Based on the event type there might be some processing needed or not.
 * @author jsanca
 */
public interface SystemEventProcessor extends Serializable {

    /**
     * Process a {@link SystemEvent}
     * @param event {@link SystemEvent}
     * @param sessionUser {@link User}
     * @return SystemEvent
     */
    public SystemEvent process (SystemEvent event, User sessionUser);

} // E:O:F:SystemEventProcessor.
