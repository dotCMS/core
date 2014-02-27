package com.dotcms.csspreproc;

import java.util.Date;

import com.dotcms.csspreproc.CachedCSS.ImportedAsset;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;

public class CSSCacheImpl extends CSSCache {

    protected final DotCacheAdministrator cache;
    protected final String group="CSSCache";
    protected final String[] groups={group};
    
    public CSSCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }
    
    @Override
    public String getPrimaryGroup() {
        return group;
    }

    @Override
    public String[] getGroups() {
        return groups;
    }

    @Override
    public void clearCache() {
        cache.flushGroup(group);
    }
    
    protected String buildKey(String hostId, String uri, boolean live) {
        return group+":"+hostId+":"+uri+":"+(live?"live":"working");
    }
    
    protected boolean isValid(CachedCSS cssObj) {
        for(ImportedAsset asset : cssObj.imported) {
            CachedCSS importedCache = get(cssObj.hostId, asset.uri, cssObj.live);
            if(importedCache==null || !importedCache.modDate.equals(asset.modDate)) {
                remove(cssObj.hostId, cssObj.uri, cssObj.live);
                return false;
            }
        }
        return true;
    }
    

    @Override
    protected CachedCSS get(String hostId, String uri, boolean live) {
        String key = buildKey(hostId, uri, live);
        CachedCSS cssObj=null;
        try {
            cssObj = (CachedCSS)cache.get(key, group);
            if(!isValid(cssObj)) {
                remove(hostId,uri,live);
                cssObj=null;
            }
        }
        catch(Exception ex) {
            Logger.debug(this, "cache entry not found: "+key, ex);
        }
        return cssObj;
    }

    @Override
    public void remove(String hostId, String uri, boolean live) {
        cache.remove(buildKey(hostId,uri,live), group);
    }

    @Override
    protected void add(CachedCSS cc) {
        cache.put(buildKey(cc.hostId,cc.uri,cc.live), cc, group);
    }
    
}
