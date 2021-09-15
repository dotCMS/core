package com.dotmarketing.business.cache.provider.timedcache;

public class Expirable {
    private long ttl;
    private Object content;

    public Expirable(Object content, long ttl) {
        this.content = content;
        this.ttl = ttl;
    }

    public long getTtl() {
        return ttl;
    }

    public Object getContent() {
        return content;
    }
}