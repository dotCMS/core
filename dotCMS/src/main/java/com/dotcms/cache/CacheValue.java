package com.dotcms.cache;

import java.io.Serializable;

public class CacheValue implements Serializable {

    public final Object value;
    public final long ttlInMillis;

    public CacheValue(Object value, long ttlInMillis) {
        this.value = value;
        this.ttlInMillis = ttlInMillis <= 0 ? Long.MAX_VALUE : ttlInMillis;
    }

    public CacheValue(Object value) {
        this.value = value;
        this.ttlInMillis = Long.MAX_VALUE;
    }
}
