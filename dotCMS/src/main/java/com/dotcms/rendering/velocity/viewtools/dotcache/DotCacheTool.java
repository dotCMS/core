package com.dotcms.rendering.velocity.viewtools.dotcache;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotcms.concurrent.Debouncer;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.BlockDirectiveCacheImpl;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Function0;
import io.vavr.Lazy;

public class DotCacheTool implements ViewTool {

    final Lazy<BlockDirectiveCache> cache ;

    

    public DotCacheTool() {
        cache= Lazy.of(Function0.of(BlockDirectiveCacheImpl::new));
        cache.get();
        Logger.debug(getClass(), "starting DotCacheTool");
    }
    
    
    @VisibleForTesting
    public DotCacheTool(final DotCacheAdministrator admin) {

        cache = Lazy.of(()->new BlockDirectiveCacheImpl(admin, true));
        cache.get();
        Logger.debug(getClass(), "starting DotCacheTool");

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
        this.put(key, value, Integer.MAX_VALUE);
    }

    Debouncer debounceAdd = new Debouncer();
    public void put(final String key, final Serializable value, int ttl) {
        if (ttl <= 0) {
            cache.get().remove(key);
            return;
        }
        
        
        Map<String, Serializable> map = Map.of(key, value);
        

        debounceAdd.debounce(key, () -> cache.get().add(key, map, ttl), 1, TimeUnit.SECONDS);

    }


    public void remove(final String key) {
        cache.get().remove(key);
    }

    public void clear() {
        
        cache.get().clearCache();
        
    }
    
    
    


}
