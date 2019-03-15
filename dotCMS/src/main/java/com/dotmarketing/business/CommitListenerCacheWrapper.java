package com.dotmarketing.business;

import java.util.List;
import java.util.Set;

import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;

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

    public void initProviders() {
        dotcache.initProviders();
    }

    public Set<String> getGroups() {
        return dotcache.getGroups();
    }

    public void flushAll() {
        dotcache.flushAll();
    }

    public void flushGroup(String group) {
        dotcache.flushGroup(group);
    }

    public void flushAlLocalOnly(boolean ignoreDistributed) {
        dotcache.flushAlLocalOnly(ignoreDistributed);
    }

    public void flushGroupLocalOnly(String group, boolean ignoreDistributed) {
        dotcache.flushGroupLocalOnly(group, ignoreDistributed);
    }

    public Object get(String key, String group) throws DotCacheException {
        return dotcache.get(key, group);
    }

    public void removeLocalOnly(String key, String group, boolean ignoreDistributed) {
        dotcache.removeLocalOnly(key, group, ignoreDistributed);
    }

    public void shutdown() {
        dotcache.shutdown();
    }

    public List<CacheProviderStats> getCacheStatsList() {
        return dotcache.getCacheStatsList();
    }

    public CacheTransport getTransport() {
        return dotcache.getTransport();
    }

    public void setTransport(CacheTransport transport) {
        dotcache.setTransport(transport);
    }

    public void invalidateCacheMesageFromCluster(String message) {
        dotcache.invalidateCacheMesageFromCluster(message);
    }

    public Class<?> getImplementationClass() {
        return dotcache.getClass();
    }

    public void put(final String key, final Object content, final String group) {
        dotcache.put(key, content, group);
        try {
            if (DbConnectionFactory.inTransaction()) {
                HibernateUtil.addRollbackListener(new FlushCacheRunnable() {
                    public void run() {
                        dotcache.remove(key, group);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void remove(final String key, final String group) {
        if (DbConnectionFactory.inTransaction()) {
            try {
                HibernateUtil.addCommitListener(new FlushCacheRunnable() {
                    public void run() {
                        dotcache.remove(key, group);
                    }
                });
            } catch (DotHibernateException e) {
                dotcache.remove(key, group);
                throw new RuntimeException(e);
            }
        } else {
            dotcache.remove(key, group);
        }
    }

    public DotCacheAdministrator getImplementationObject() {
        return dotcache;
    }
}
