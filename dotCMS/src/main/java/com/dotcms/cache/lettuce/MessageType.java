package com.dotcms.cache.lettuce;

enum MessageType {
    INFO, INVALIDATE, CYCLE_KEY, PING, PONG, VALIDATE_CACHE_RESPONSE, VALIDATE_CACHE;

    static MessageType from(final String type) {
        if (type == null) {
            return INFO;
        }
        return valueOf(type);
    }
}
