package com.dotcms.cache;

import java.io.Serializable;

public class CacheValueImpl implements CacheValue, Serializable {

    final Object value;
    final long ttlInMillis;
    final long createdInMillis = System.currentTimeMillis();

    @Override
    public long getTtlInMillis() {
        return ttlInMillis;
    }

    @Override
    public Object getValue() {
        return value;
    }

    public CacheValueImpl(Object value, long ttlInMillis) {
        this.value = value;
        this.ttlInMillis = ttlInMillis <= 0 ? Long.MAX_VALUE : ttlInMillis;
    }

    public CacheValueImpl(Object value) {
        this.value = value;
        this.ttlInMillis = Long.MAX_VALUE;
    }

    @Override
    public boolean isExpired() {
        return createdInMillis + ttlInMillis < System.currentTimeMillis();
    }

}
