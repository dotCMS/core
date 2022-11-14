package com.dotcms.rendering.velocity.viewtools.dotcache;

import java.io.Serializable;
import java.util.Map;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.BlockDirectiveCacheImpl;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

public class DotCacheTool implements ViewTool {

    final Lazy<BlockDirectiveCache> cache = Lazy.of(BlockDirectiveCacheImpl::new);



    public DotCacheTool() {
        Logger.debug(getClass(), "starting dotvelocitycache");

    }



    @Override
    public void init(Object initData) {
        // We do not use this method
    }



    public Serializable get(final String key) {
        Map<String, Serializable> entry = cache.get().get(key);
        return entry.get(key);
    }


    public void put(final String key, final Serializable value) {
        Map<String, Serializable> map = Map.of(key, value);
        cache.get().add(key, map, Integer.MAX_VALUE);
    }

    
    public void put(final String key, final Serializable value, int ttl) {
        if (ttl == 0) {
            cache.get().remove(key);
            return;
        }
        Map<String, Serializable> map = Map.of(key, value);
        cache.get().add(key, map, ttl);
    }


    public void remove(final String key) {
        cache.get().remove(key);
    }



}
