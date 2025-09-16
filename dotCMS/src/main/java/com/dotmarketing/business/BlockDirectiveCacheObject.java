package com.dotmarketing.business;

import com.dotcms.cache.CacheValue;
import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

public class BlockDirectiveCacheObject implements CacheValue {


    private static final long serialVersionUID = 1L;
    private final long created;
    private final long ttl;
    private final Map<String, Serializable> map;


    public long getTtl() {
        return this.ttl;
    }

    public BlockDirectiveCacheObject(Map<String, Serializable> map, Duration ttlDuration) {
        this.created = System.currentTimeMillis();
        this.ttl = ttlDuration.toMillis();
        this.map = map;
    }

    @Override
    public Object getValue() {
        return this.map;
    }

    @Override
    public long getTtlInMillis() {
        return this.ttl;

    }

    public long getCreated() {

        return this.created;
    }

    public Map<String, Serializable> getMap(){

        return this.map;
    }

    public boolean isExpired() {
        return this.created + this.ttl <= System.currentTimeMillis();
    }



}
