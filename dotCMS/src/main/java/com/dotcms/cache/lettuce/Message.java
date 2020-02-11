package com.dotcms.cache.lettuce;

import java.util.Map;

final class Message{
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
    

    public Message(Map<String,Object> map) {
        super();
        Map.Entry<String, Object> unpacked = map.entrySet().stream().findFirst().get();
        this.type = MessageType.from(unpacked.getKey());
        String[] split = unpacked.getValue().toString().split(delimiter,2);
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
    
    
}
