package com.dotcms.api.system.event.message.builder;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
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
    private final String[] portletIdList;
    private final long life;
    private final MessageSeverity severity;
    private final MessageType type;

    protected SystemMessage(final Object message,
                         final String[] portletIdList,
                         final long life,
                         final MessageSeverity severity,
                         final MessageType type) {
        this.message = message;
        this.portletIdList = portletIdList;
        this.life = life;
        this.severity = severity;
        this.type = type;
    }

    public Object getMessage() {
        return message;
    }

    public String[] getPortletIdList() {
        return portletIdList;
    }

    public long getLife() {
        return life;
    }

    public MessageSeverity getSeverity() {
        return severity;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "SystemMessage{" +
                "message=" + message +
                ", portletIdList=" + portletIdList +
                ", life=" + life +
                ", severity=" + severity +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(final Object another) {
        if (this == another) return true;
        if (another == null || getClass() != another.getClass()) return false;
        final SystemMessage that = (SystemMessage) another;
        return life == that.life &&
                Objects.equals(message, that.message) &&
                Arrays.equals(portletIdList, that.portletIdList) &&
                severity == that.severity &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(message, life, severity, type);
        result = 31 * result + Arrays.hashCode(portletIdList);
        return result;
    }
} // E:O:F:SystemMessage.
