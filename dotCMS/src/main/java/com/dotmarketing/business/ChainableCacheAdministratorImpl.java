/**
 *
 */
package com.dotmarketing.business;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cache.postgres.PostgresCacheTransport;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

/**
 * Cache administrator that uses the CacheProviders infrastructure (Cache chains)
 *
 * @author Jason Tesser
 * @version 1.6.5
 *
 */
public class ChainableCacheAdministratorImpl implements DotCacheAdministrator {

    private static final CacheTransport cacheTransport = new PostgresCacheTransport();
    private static final boolean useTransportChannel = true;
    
    private CacheProviderAPI cacheProviderAPI;


    public static final String TEST_MESSAGE = "HELLO CLUSTER!";
    public static final String TEST_MESSAGE_NODE = "TESTNODE";
    public static final String VALIDATE_CACHE = "validateCacheInCluster-";
    public static final String VALIDATE_CACHE_RESPONSE = "validateCacheInCluster-response-";
    public static final String VALIDATE_SEPARATOR = "_";
    public static final String DUMMY_TEXT_TO_SEND = "DUMMY MSG TO TEST SEND";

    public CacheTransport getTransport() {
        return cacheTransport;
    }

    public void setTransport(CacheTransport transport) {


    }

    public ChainableCacheAdministratorImpl() {
        this(cacheTransport);
    }

    public ChainableCacheAdministratorImpl(CacheTransport transport) {
       
        APILocator.getReindexQueueAPI();
    }

    public void initProviders() {

        try {
            // Initializing all the Cache providers
            cacheProviderAPI = APILocator.getCacheProviderAPI();
            cacheProviderAPI.init();
            cacheTransport.init(APILocator.getServerAPI().getCurrentServer());
        } catch (Exception e) {
            throw new DotRuntimeException("Error initializing Cache providers", e);
        }

    }

    @WrapInTransaction
    public void setCluster(Server localServer) throws Exception {
        Logger.info(this, "***\t Starting Cluster Setup");
        cacheTransport.init(APILocator.getServerAPI().getCurrentServer());

    }

    /*
     * (non-Javadoc)
     *
     * @see com.dotmarketing.business.DotCacheAdministrator#flushAll()
     */
    public void flushAll() {

        flushAlLocalOnly(false);

        if (useTransportChannel) {

            if (!cacheProviderAPI.isDistributed()) {
                if (getTransport() != null) {
                    try {
                        getTransport().send("0:" + ROOT_GOUP);
                    } catch (Exception e) {
                        Logger.error(ChainableCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(),
                                e);
                    }
                } else {
                    throw new CacheTransportException("No Cache transport implementation is defined");
                }
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.dotmarketing.business.DotCacheAdministrator#flushGroup(java.lang. String)
     */

    public void flushGroup(String group) {

        if (group == null) {
            return;
        }
        group = group.toLowerCase();

        flushGroupLocalOnly(group, false);

        if (useTransportChannel) {
            if (!cacheProviderAPI.isGroupDistributed(group)) {
                try {
                    cacheTransport.send("0:" + group);
                } catch (Exception e) {
                    Logger.error(ChainableCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
                }
            }
        }
    }

    public void flushAlLocalOnly(boolean ignoreDistributed) {

        // Invalidates all the Cache
        cacheProviderAPI.removeAll(ignoreDistributed);
    }

    public void flushGroupLocalOnly(String group, boolean ignoreDistributed) {

        if (group == null) {
            return;
        }

        group = group.toLowerCase();

        // Invalidates the Cache for the given group
        cacheProviderAPI.remove(group, ignoreDistributed);
    }

    public Object get(String key, String group) throws DotCacheException {

        if (key == null || group == null) {
            return null;
        }

        key = key.toLowerCase();
        group = group.toLowerCase();

        // Find the Object for a given key in a given group
        return cacheProviderAPI.get(group, key);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dotmarketing.business.DotCacheAdministrator#put(java.lang.String, java.lang.Object,
     * java.lang.String[])
     */
    public void put(String key, final Object content, String group) {

        if (key == null || group == null) {
            return;
        }

        key = key.toLowerCase();
        group = group.toLowerCase();

        // Adds a given object gor a given group to a given key
        cacheProviderAPI.put(group, key, content);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dotmarketing.business.DotCacheAdministrator#remove(java.lang.String)
     */
    public void remove(final String key, final String group) {

        if (key == null || group == null) {
            return;
        }

        String k = key.toLowerCase();
        String g = group.toLowerCase();
        removeLocalOnly(k, g, false);

        if (useTransportChannel) {
            if (!cacheProviderAPI.isGroupDistributed(group)) {
                if (getTransport() != null) {
                    try {
                        getTransport().send(k + ":" + g);
                    } catch (Exception e) {
                        Logger.warnAndDebug(ChainableCacheAdministratorImpl.class,
                                "Unable to send invalidation to cluster : " + e.getMessage(), e);
                    }
                } else {
                    Logger.warn(ChainableCacheAdministratorImpl.class,
                            "No Cache transport implementation is defined - clustered dotcms will not work properly without a valid cache transport");
                }
            }
        }

    }

    public void removeLocalOnly(final String key, final String group, boolean ignoreDistributed) {

        if (key == null || group == null) {
            return;
        }

        // Invalidates from Cache a key from a given group
        cacheProviderAPI.remove(group, key, ignoreDistributed);

    }

    public Set<String> getGroups() {
        // Returns all groups in the cache
        return cacheProviderAPI.getGroups();
    }

    public List<CacheProviderStats> getCacheStatsList() {
        // Returns the stats for all the cache providers
        return cacheProviderAPI.getStats();
    }

    public void shutdown() {
        cacheProviderAPI.shutdown();
    }

    public void shutdownChannel() {
        


    }

    public boolean isClusteringEnabled() {
        return useTransportChannel;
    }

    public void send(String msg) {

        if (getTransport() != null) {

            try {
                getTransport().send(msg);
            } catch (Exception e) {
                Logger.warn(ChainableCacheAdministratorImpl.class, "Unable to send message to cluster : " + e.getMessage(), e);
            }

        } else {
            throw new CacheTransportException("No Cache transport implementation is defined");
        }

    }

    /**
     * Tests the transport channel of a cluster sending and receiving messages for a given number of
     * servers
     *
     * @param dateInMillis String use as Key on out Map of results.
     * @param numberServers Number of servers to wait for a response.
     * @param maxWaitSeconds seconds to wait for a response.
     * @return Map with DateInMillis, ServerInfo for each cache/live server in Cluster.
     */
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)
            throws DotCacheException {

        if (getTransport() != null) {

            try {
                return getTransport().validateCacheInCluster(dateInMillis, numberServers, maxWaitSeconds);
            } catch (CacheTransportException e) {
                Logger.error(ChainableCacheAdministratorImpl.class, e.getMessage(), e);
                throw new DotCacheException(e);
            }

        } else {
            throw new CacheTransportException("No Cache transport implementation is defined");
        }
    }

    public void testCluster() {

        if (getTransport() != null) {

            try {
                getTransport().testCluster();
            } catch (Exception e) {
                Logger.error(ChainableCacheAdministratorImpl.class, e.getMessage(), e);
            }

        } else {
            throw new CacheTransportException("No Cache transport implementation is defined");
        }

    }

    public void invalidateCacheMesageFromCluster(String message) {
        if (message == null) {
            return;
        } ;
        int i = message.lastIndexOf(":");
        if (i > 0) {

            String key = message.substring(0, i);
            String group = message.substring(i + 1, message.length());

            key = key.toLowerCase();
            group = group.toLowerCase();

            if (key.equals("0")) {

                if (group.equalsIgnoreCase(DotCacheAdministrator.ROOT_GOUP)) {
                    CacheLocator.getCacheAdministrator().flushAlLocalOnly(true);
                } else {
                    CacheLocator.getCacheAdministrator().flushGroupLocalOnly(group, true);
                }
            } else {
                CacheLocator.getCacheAdministrator().removeLocalOnly(key, group, true);
            }
        } else {
            Logger.error(this, "The cache to locally remove key is invalid. The value was " + message);
        }
    }

    @Override
    public Class getImplementationClass() {
        return ChainableCacheAdministratorImpl.class;
    }

    @Override
    public DotCacheAdministrator getImplementationObject() {
        return this;
    }

}
