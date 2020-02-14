package com.dotcms.cache.lettuce;

import java.io.Serializable;

/**
 * this class parses incoming string messages and turns them into actionable objects
 * 
 * @author will
 *
 */
final class Message implements Serializable {
    final String serverId;
    final MessageType type;
    final String message;
    final static String delimiter = ":x:";

    public Message(String serverId, MessageType type, String message) {
        super();
        this.serverId = serverId;
        this.type = type;
        this.message = message;
    }


    public Message(Object type, String value) {
        super();
        this.type = MessageType.from(String.valueOf(type));
        String[] split = value.split(delimiter, 2);
        this.serverId = split[0];
        this.message = split[1];
    }

    public String encode() {
        return this.serverId + delimiter + message;
    }

    @Override
    public String toString() {
        return "{fromServer:" + serverId + ", type:" + type + ", message:" + message + "}";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Message other = (Message) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (serverId == null) {
            if (other.serverId != null)
                return false;
        } else if (!serverId.equals(other.serverId))
            return false;
        if (type != other.type)
            return false;
        return true;
    }



}
