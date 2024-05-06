/**
 * 
 */
package com.dotcms.rendering.js;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.rendering.velocity.services.MacroCacheRefresherJob;
import com.dotmarketing.business.CachableSupport;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;

/**
 * Javascript cache
 * @author jsanca
 */
public class JsCache implements CachableSupport<String, Object> {

    private final DotCacheAdministrator cache;

    private String primaryGroup = "JavascriptCache";
    // region's name for the cache
    private String[] groupNames = {primaryGroup};

    public JsCache() {

        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public Object get(final String resourceKey) {

        return cache.getNoThrow(resourceKey, primaryGroup);

    }

    @Override
    public Object put(final String key, final Object value) {

        cache.put(key, value, primaryGroup);
        return value;
    }

    @Override
    public void remove(final String key) {

        try {
            cache.remove(key, primaryGroup);
        } catch (Exception e) {
            Logger.debug(this, e.getMessage(), e);
        }
    }

    public void clearCache() {
        for (String group : groupNames) {
            cache.flushGroup(group);
        }

        DotConcurrentFactory.getInstance().getSubmitter().submit(new MacroCacheRefresherJob());
    }

    public String[] getGroups() {
        return groupNames;
    }

    public String getPrimaryGroup() {
        return primaryGroup;
    }
}
