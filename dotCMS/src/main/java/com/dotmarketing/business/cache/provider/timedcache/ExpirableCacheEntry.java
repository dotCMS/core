package com.dotmarketing.business.cache.provider.timedcache;

import com.dotcms.business.Expirable;
import java.io.Serializable;

public class ExpirableCacheEntry implements Expirable, Serializable {
    private static final long serialVersionUID = 1L;
    private final long ttl;
    private final Object content;

    public ExpirableCacheEntry(Object content, long ttl) {
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