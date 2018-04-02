package com.dotcms.api.system.event;

import java.io.Serializable;
import java.util.Set;

/**
 * A system message encapsulates a simple message (could be raw or rich) and a list of portlets (optional) that can apply the
 * message.
 * This will be the payload of a system message on:
 * <ul>
 *     <li>RAW_MESSAGE</li>
 *     <li>RAW_ERROR_MESSAGE</li>
 *     <li>RICH_MEDIA_MESSAGE</li>
 * </ul>
 * @author jsanca
 */
public class SystemMessage implements Serializable {

    private final Object message;
    private final Set<String> portletIdList;

    public SystemMessage(final Object message, final Set<String> portletIdList) {
        this.message = message;
        this.portletIdList = portletIdList;
    }

    public Object getMessage() {
        return message;
    }

    public Set<String> getPortletIdList() {
        return portletIdList;
    }

    public boolean containsPortlet(final String portletId) {

        return this.portletIdList.contains(portletId);
    }

    @Override
    public String toString() {
        return "SystemMessage{" +
                "message=" + message +
                ", portletIdList=" + portletIdList +
                '}';
    }
} // E:O:F:SystemMessage.
