package com.dotmarketing.business;

import java.util.List;
import java.util.Set;

import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;

/**
 * this class wraps our cache administrator and will automatically make cache removes and puts
 * respect transactions
 * 
 * @author will
 *
 */
class CommitListenerCacheWrapper implements DotCacheAdministrator {

    final DotCacheAdministrator dotcache;

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
        if (DbConnectionFactory.inTransaction()) {
            final Runnable runner = new FlushCacheRunnable() {
                public void run() {
                    dotcache.flushAll();
                }
            };
            HibernateUtil.addRollbackListener("flushAll" ,runner);
            HibernateUtil.addCommitListener("flushAll" , runner);
        }
        
        dotcache.flushAll();
    }

    @Override
    public void flushGroup(String group) {
        
        if (DbConnectionFactory.inTransaction()) {
            final Runnable runner = new FlushCacheRunnable() {
                public void run() {
                    dotcache.flushGroup(group);
                }
            };
            HibernateUtil.addRollbackListener("flushGroup" + group,runner);
            HibernateUtil.addCommitListener("flushGroup" + group, runner);
    }
        
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
        if (DbConnectionFactory.inTransaction()) {
            HibernateUtil.addRollbackListener(group+key,new FlushCacheRunnable() {
                public void run() {
                    dotcache.remove(key, group);
                }
            });
        }
    }

    @Override
    public void remove(final String key, final String group) {
        if (DbConnectionFactory.inTransaction()) {
                final String flushKey=String.valueOf(key+group).toLowerCase();
                final Runnable runner = new FlushCacheRunnable() {
                    public void run() {
                        dotcache.remove(key, group);
                    }
                };
                HibernateUtil.addRollbackListener(flushKey,runner);
                HibernateUtil.addCommitListener(flushKey, runner);

        }
        dotcache.remove(key, group);
    }

    public DotCacheAdministrator getImplementationObject() {
        return dotcache;
    }

}
