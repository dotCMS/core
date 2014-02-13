package com.dotcms.csspreproc;

import com.dotmarketing.business.Cachable;

public abstract class CSSCache implements Cachable {
    protected abstract CachedCSS get(String hostId, String uri, boolean live);
    
    public abstract void remove(String hostId, String uri, boolean live); 
    
    protected abstract void add(CachedCSS cachedCSS);
}
