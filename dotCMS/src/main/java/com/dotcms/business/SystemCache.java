package com.dotcms.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

/**
 * Created by jasontesser on 3/17/17.
 */
public class SystemCache implements Cachable {

    protected static String PRIMARY_GROUP = "SYSTEM_GROUP";
    private String[] groupNames = {PRIMARY_GROUP};

    public String[] getGroups() {
        return groupNames;
    }
    public String getPrimaryGroup() {
        return PRIMARY_GROUP;
    }

    private DotCacheAdministrator cache;

    public SystemCache() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public void clearCache() {
        cache.flushGroup(PRIMARY_GROUP);
    }

    public Object get(String key) {
        Object o = null;
        try{
            o = (Object) cache.get(key,PRIMARY_GROUP);
        }catch (DotCacheException e) {
            Logger.debug(this, "Cache Entry not found", e);
        }

        return o;
    }

    public Object put(String key, Object object) {
        if(object == null){
            return null;
        }
        // Add the key to the cache
        cache.put(key, object,PRIMARY_GROUP);

        return object;
    }

    /* (non-Javadoc)
     * @see org.apache.velocity.runtime.resource.ResourceCache#remove(java.lang.Object)
     */
    public void remove(String key) {

        try{
            cache.remove(key,getPrimaryGroup());
        } catch ( Exception e ) {
            Logger.debug(this, e.getMessage(), e);
        }
    }

}
