package com.dotmarketing.business;

import java.util.List;
import java.util.Set;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.db.listeners.CommitAPI;
import com.dotmarketing.db.listeners.FlushCacheListener;
import com.dotmarketing.db.listeners.RollbackListener;

/**
 * this class wraps our cache administrator and will automatically make cache removes and puts
 * respect transactions
 * 
 * @author will
 *
 */
class CommitListenerCacheWrapper implements DotCacheAdministrator {

    final DotCacheAdministrator dotcache;

    final CommitAPI commitAPI =  CommitAPI.getInstance();
    
    
    
    public CommitListenerCacheWrapper(DotCacheAdministrator dotcache) {
        this.dotcache = dotcache;
    }

    @Override
    public void initProviders() {
        dotcache.initProviders();
    }

    @Override
    public Set<String> getGroups() {
        return dotcache.getGroups();
    }

    @Override
    public void flushAll() {
        final FlushCacheListener runner = new FlushCacheListener() {
            public void run() {
                dotcache.flushAll();
            }
            
            public String key() {
                return "flushAll";
            }
            
        };
        

        commitAPI.addFlushCacheAsync(runner);
        
        
        dotcache.flushAll();
    }

    @Override
    public void flushGroup(final String group) {
        
        final FlushCacheListener runner = new FlushCacheListener() {
            public void run() {
                dotcache.flushGroup(group);
            }
            
            public String key() {
                return group;
            }
        };
            

        commitAPI.addFlushCacheAsync(runner);

        
        dotcache.flushGroup(group);
    }

    @Override
    public void flushAlLocalOnly(boolean ignoreDistributed) {
        dotcache.flushAlLocalOnly(ignoreDistributed);
    }

    @Override
    public void flushGroupLocalOnly(String group, boolean ignoreDistributed) {
        dotcache.flushGroupLocalOnly(group, ignoreDistributed);
    }

    @Override
    public Object get(String key, String group) throws DotCacheException {

        return dotcache.get(key, group);

    }

    @Override
    public Object getNoThrow(String key, String group) {

        return dotcache.getNoThrow(key, group);

    }

    @Override
    public void removeLocalOnly(String key, String group, boolean ignoreDistributed) {
        dotcache.removeLocalOnly(key, group, ignoreDistributed);
    }

    @Override
    public void shutdown() {
        dotcache.shutdown();
    }

    @Override
    public List<CacheProviderStats> getCacheStatsList() {
        return dotcache.getCacheStatsList();
    }

    @Override
    public CacheTransport getTransport() {
        return dotcache.getTransport();
    }

    @Override
    public void setTransport(CacheTransport transport) {
        dotcache.setTransport(transport);
    }

    @Override
    public void invalidateCacheMesageFromCluster(String message) {
        dotcache.invalidateCacheMesageFromCluster(message);
    }

    public Class<?> getImplementationClass() {
        return dotcache.getClass();
    }

    // only put when we are not in a transaction
    @Override
    public void put(final String key, final Object content, final String group) {
        dotcache.put(key, content, group);
        
        FlushCacheListener runner = new FlushCacheListener() {
            public void run() {
                dotcache.remove(key, group);
            }
            public String key() {
                return key + group;
            }
            
        };
        commitAPI.addRollBackListener(runner);

    }

    @Override
    public void remove(final String key, final String group) {
        dotcache.remove(key, group);
        FlushCacheListener runner = new FlushCacheListener() {
            public void run() {
                dotcache.remove(key, group);
            }
            public String key() {
                return key + group;
            }
            
        };  
        commitAPI.addFlushCacheAsync(runner);
        commitAPI.addRollBackListener(runner);
    }

    public DotCacheAdministrator getImplementationObject() {
        return dotcache;
    }

}
