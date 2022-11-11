package com.dotcms.rendering.velocity.viewtools.dotcache;

import static java.time.temporal.ChronoUnit.SECONDS;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.dotcms.cache.Expirable;

public final class ExpirableCacheEntry implements Expirable, Serializable {
    private static final long serialVersionUID = 1L;
    final long ttl;

    final LocalDateTime since;
    final Serializable results;

    public ExpirableCacheEntry(Serializable results, long ttl) {
        this.results = results;
        this.ttl = ttl;
        since = LocalDateTime.now();
    }

    public boolean isExpired() {
        return ttl != -1 && LocalDateTime.now().isAfter(since.plus(ttl, SECONDS));
    }

    @Override
    public long getTtl() {
        return this.ttl;
    }

    public Serializable getResults() {
        return isExpired() ? null : this.results;
    }
    
    public Serializable getStaleResults() {
        return this.results;
    }
    @Override
    public String toString() {
        return "ExpirableCacheEntry:{since:$0,ttl:$1,expired:$2,results:$3"
                        .replace("$0", since.toString())
                        .replace("$1", String.valueOf(ttl))
                        .replace("$2", String.valueOf(isExpired()))
                        .replace("$3", String.valueOf(results));              
                                            
        
    }
    
}