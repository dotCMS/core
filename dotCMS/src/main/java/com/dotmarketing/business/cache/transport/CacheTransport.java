package com.dotmarketing.business.cache.transport;

import java.util.Map;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import io.vavr.control.Try;

/**
 * @author Jonathan Gamba
 *         Date: 8/13/15
 */
public interface CacheTransport {

    void init ( Server localServer ) throws CacheTransportException;

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

    
    
    default Map<String, Boolean> validateCacheInCluster ( int maxWaitSeconds ) {
        int lookingForServers = Try.of(()->APILocator.getServerAPI().getAliveServers().size()-1).getOrElse(0);
        return validateCacheInCluster(null,lookingForServers,2);
    };

    
    
    
    /**
     * Disconnects and closes the channel
     *
     * @throws CacheTransportException
     */
    void shutdown () throws CacheTransportException;

    boolean isInitialized();

    boolean shouldReinit();

    
    default boolean requiresAutowiring() {
        return true;
    }
    

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
    
    
    

    default CacheTransportInfo getInfo() {
        

        return new CacheTransportInfo(){
            @Override
            public String getClusterName() {
                return ClusterFactory.getClusterId();
            }

            @Override
            public String getAddress() {
                return "n/a";
            }

            @Override
            public int getPort() {
                return -1;
            }


            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public int getNumberOfNodes() {
                return 1;
            }


            @Override
            public long getReceivedBytes() {
                return 0;
            }

            @Override
            public long getReceivedMessages() {
                return 0;
            }

            @Override
            public long getSentBytes() {
                return 0;
            }

            @Override
            public long getSentMessages() {
                return 0;
            }
        };
    }
    
    
    
}