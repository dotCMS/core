package com.dotcms.api.system.event.message.builder;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotmarketing.util.DateUtil;

/**
 * Buider to create the SystemMessage (Approach to show message to specific users and/or porlets as a popup).
 * - life time is by default 3 secs (the time displaying on the UI).
 * - severity is the type of the Message see {@link MessageSeverity} for details
 * - type is the actual type of the SystemMessage it could be SIMPLE (just a popup) or confirmation (it still on demo and not supported yet on the UI)
 * Builder for {@link SystemMessage}
 */
public class SystemMessageBuilder {
    private Object message;
    private String[] portletIdList;
    private long life = DateUtil.THREE_SECOND_MILLIS;
    private MessageSeverity severity = MessageSeverity.INFO;
    private MessageType type = MessageType.SIMPLE_MESSAGE;

    public SystemMessageBuilder(){}

    public SystemMessageBuilder setMessage(final Object message) {
        this.message = message;
        return this;
    }

    public SystemMessageBuilder setPortletIdList(final String[] portletIdList) {
        this.portletIdList = portletIdList;
        return this;
    }

    public SystemMessageBuilder setLife(final long life) {
        this.life = life;
        return this;
    }

    public SystemMessageBuilder setSeverity(final MessageSeverity severity) {
        this.severity = severity;
        return this;
    }

    public SystemMessageBuilder setType(final MessageType type) {
        this.type = type;
        return this;
    }

    public SystemMessage create() {
        return new SystemMessage(message, portletIdList, life, severity, type);
    }
}
