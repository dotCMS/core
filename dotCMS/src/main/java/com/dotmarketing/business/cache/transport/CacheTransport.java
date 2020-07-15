package com.dotmarketing.business.cache.transport;

import java.util.Map;

/**
 * @author Jonathan Gamba
 *         Date: 8/13/15
 */
public interface CacheTransport {


    /**
     * Sends a message to the transport channel
     *
     * @param message
     * @throws CacheTransportException
     */
    void send ( String message ) throws CacheTransportException;

    /**
     * Tests the cluster transport channel
     *
     * @throws CacheTransportException
     */
    void testCluster () throws CacheTransportException;

    /**
     * Tests the transport channel of a cluster sending and receiving messages for a given number of servers
     *
     * @param dateInMillis   String use as Key on out Map of results.
     * @param numberServers  Number of servers to wait for a response.
     * @param maxWaitSeconds seconds to wait for a response.
     * @return Map with DateInMillis, ServerInfo for each cache/live server in Cluster.
     * @throws CacheTransportException
     */
    Map<String, Boolean> validateCacheInCluster ( String dateInMillis, int numberServers, int maxWaitSeconds ) throws CacheTransportException;

    /**
     * Disconnects and closes the channel
     *
     * @throws CacheTransportException
     */
    void shutdown () throws CacheTransportException;

    boolean isInitialized();

    boolean shouldReinit();

    /**
     * Returns stats about the cache transport
     */
    CacheTransportInfo getInfo();

    public interface CacheTransportInfo {
    	String getClusterName();
    	String getAddress();
    	int getPort();

    	boolean isOpen();
    	int getNumberOfNodes();

    	long getReceivedBytes();
    	long getReceivedMessages();
    	long getSentBytes();
    	long getSentMessages();
    }

    /**
     * Inits the cache transport
     * @throws CacheTransportException
     */
    void init() throws CacheTransportException;
}