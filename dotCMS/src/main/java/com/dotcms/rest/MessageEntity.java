package com.dotcms.rest;

import java.io.Serializable;

/**
 * Encapsulates a message.
 * Usually the errors are returned to the client transformed on JSON.
 * @author jsanca
 */
public class MessageEntity implements Serializable {

    private final String message;

    public MessageEntity(final String message) {
        this.message = message;
    }

    public String getMessage() {

        return message;
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "message='" + message + '\'' +
                '}';
    }
} // E:O:F:MessageEntity.

