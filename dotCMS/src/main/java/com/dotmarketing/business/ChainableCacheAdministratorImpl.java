/**
 *
 */
package com.dotmarketing.business;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.cluster.ClusterUtils;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

/**
 * Cache administrator that uses the CacheProviders infrastructure (Cache chains)
 *
 * @author Jason Tesser
 * @version 1.6.5
 *
 */
public class ChainableCacheAdministratorImpl implements DotCacheAdministrator {

    CacheTransport cacheTransport;

    private CacheProviderAPI cacheProviderAPI;
    private boolean useTransportChannel = false;

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

        if (getTransport() != null) {
            getTransport().shutdown();
        }

        this.cacheTransport = transport;
    }

    public ChainableCacheAdministratorImpl() {
        this(null);
    }

    public ChainableCacheAdministratorImpl(CacheTransport transport) {

        if (transport != null) {
            useTransportChannel = true;
            this.cacheTransport = transport;
        } else {
            useTransportChannel = false;
        }

        APILocator.getReindexQueueAPI();
    }

    public void initProviders() {

        try {
            // Initializing all the Cache providers
            cacheProviderAPI = APILocator.getCacheProviderAPI();
            cacheProviderAPI.init();
        } catch (Exception e) {
            throw new DotRuntimeException("Error initializing Cache providers", e);
        }

    }

    @WrapInTransaction
    public void setCluster(Server localServer) throws Exception {
        Logger.info(this, "***\t Starting Cluster Setup");

        ServerAPI serverAPI = APILocator.getServerAPI();

        String cacheProtocol, bindAddr, bindPort, cacheTCPInitialHosts, mCastAddr, mCastPort, preferIPv4;

        if (ClusterUtils.isTransportAutoWire()) {
            Logger.info(this, "Using automatic port placement as AUTOWIRE_CLUSTER_TRANSPORT is ON");

            String bindAddressFromProperty = Config.getStringProperty("CACHE_BINDADDRESS", null);

            if (UtilMethods.isSet(bindAddressFromProperty)) {
                try {
                    InetAddress addr = InetAddress.getByName(bindAddressFromProperty);
                    if (ClusterFactory.isValidIP(bindAddressFromProperty)) {
                        bindAddressFromProperty = addr.getHostAddress();
                    } else {
                        Logger.info(ClusterFactory.class,
                                "Address provided in CACHE_BINDADDRESS property is not " + "valid: " + bindAddressFromProperty);
                        bindAddressFromProperty = null;
                    }
                } catch (UnknownHostException e) {
                    Logger.info(ClusterFactory.class,
                            "Address provided in CACHE_BINDADDRESS property is not " + " valid: " + bindAddressFromProperty);
                    bindAddressFromProperty = null;
                }
            }

            cacheProtocol = Config.getStringProperty("CACHE_PROTOCOL", "tcp");

            bindAddr = bindAddressFromProperty != null ? bindAddressFromProperty : localServer.getIpAddress();

            if (UtilMethods.isSet(localServer.getCachePort())) {
                bindPort = Long.toString(localServer.getCachePort());
            } else {
                bindPort = ClusterFactory.getNextAvailablePort(localServer.getServerId(), ServerPort.CACHE_PORT);
            }
            localServer = Server.builder(localServer).withCachePort(Integer.parseInt(bindPort)).build();

            List<Server> aliveServers = serverAPI.getAliveServers(Collections.singletonList(localServer.getServerId()));
            aliveServers.add(localServer);

            StringBuilder initialHosts = new StringBuilder();

            int i = 0;
            for (Server server : aliveServers) {
                if (i > 0) {
                    initialHosts.append(",");
                }

                if (UtilMethods.isSet(server.getHost()) && !server.getHost().equals("localhost")) {
                    initialHosts.append(server.getHost()).append("[").append(server.getCachePort()).append("]");
                } else {
                    initialHosts.append(server.getIpAddress()).append("[").append(server.getCachePort()).append("]");
                }
                i++;
            }

            if (initialHosts.length() == 0) {
                if (bindAddr.equals("localhost")) {
                    initialHosts.append(localServer.getIpAddress()).append("[").append(bindPort).append("]");
                } else {
                    initialHosts.append(bindAddr).append("[").append(bindPort).append("]");
                }
            }

            cacheTCPInitialHosts = Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", initialHosts.toString());

            mCastAddr = Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10");
            mCastPort = Config.getStringProperty("CACHE_MULTICAST_PORT", "45588");
            preferIPv4 = Config.getStringProperty("CACHE_FORCE_IPV4", "true");
        } else {
            Logger.info(this, "Using manual port placement as AUTOWIRE_CLUSTER_TRANSPORT is OFF");

            cacheProtocol = Config.getStringProperty("CACHE_PROTOCOL", "tcp");
            bindAddr = Config.getStringProperty("CACHE_BINDADDRESS", null);
            bindPort = Config.getStringProperty("CACHE_BINDPORT", null);
            cacheTCPInitialHosts = Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", "localhost[5701]");
            mCastAddr = Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10");
            mCastPort = Config.getStringProperty("CACHE_MULTICAST_PORT", "45588");
            preferIPv4 = Config.getStringProperty("CACHE_FORCE_IPV4", "true");
        }

        if (UtilMethods.isSet(bindAddr)) {
            Logger.info(this, "***\t Using " + bindAddr + " as the bindaddress");

            Config.setProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_BIND_ADDRESS, bindAddr);
        } else {
            Logger.info(this, "***\t bindaddress is not set");
        }

        if (UtilMethods.isSet(bindPort)) {
            Logger.info(this, "***\t Using " + bindPort + " as the bindport");

            Config.setProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_BIND_PORT, bindPort);
        } else {
            Logger.info(this, "***\t bindport is not set");
        }

        if (cacheProtocol.equals("tcp")) {
            Logger.info(this, "***\t Setting up TCP initial hosts: " + cacheTCPInitialHosts);

            Config.setProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_TCP_INITIAL_HOSTS, cacheTCPInitialHosts);
        } else if (cacheProtocol.equals("udp")) {
            Logger.info(this, "***\t Setting up UDP address and port: " + mCastAddr + ":" + mCastPort);

            Config.setProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_UDP_MCAST_ADDRESS, mCastAddr);
            Config.setProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_UDP_MCAST_PORT, mCastPort);
        } else {
            Logger.info(this, "Not Setting up any Properties as no protocal was found");
        }

        Logger.info(this, "***\t Prefer IPv4: " + (preferIPv4.equals("true") ? "enabled" : "disabled"));
        System.setProperty("java.net.preferIPv4Stack", preferIPv4);

        if (cacheTransport != null && (cacheTransport.shouldReinit() || !cacheTransport.isInitialized())) {
            cacheTransport.init(localServer);
            useTransportChannel = true;
        } else {
            throw new CacheTransportException("No Cache transport implementation is defined");
        }
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

        if (getTransport() != null) {
            getTransport().shutdown();
            useTransportChannel = false;
        } else {
            throw new CacheTransportException("No Cache transport implementation is defined");
        }

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
