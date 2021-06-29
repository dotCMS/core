package com.dotmarketing.business.cache.transport;

import java.io.Serializable;
import java.util.Map;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.StringUtils;
import com.google.common.collect.ImmutableMap;


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


    
     Map<String, Serializable> validateCacheInCluster ( int maxWaitInMillis ) ;

    
    
    
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
    

    public interface CacheTransportInfo extends Serializable {

        default String getClusterName() {
            return ClusterFactory.getClusterId();
        }

        
    	String getAddress();
    	int getPort();

    	boolean isOpen();
    	int getNumberOfNodes();

    	long getReceivedBytes();
    	long getReceivedMessages();
    	long getSentBytes();
    	long getSentMessages();
    	
    	default String getLicenseId() {
    	    return LicenseUtil.getDisplaySerial();
    	}
        default String getServerId() {
            return StringUtils.shortify(APILocator.getServerAPI().readServerId(),10);
        }
        default String getCacheTransport() {
            return CacheLocator.getCacheAdministrator().getTransport().getClass().getSimpleName();
        }
    	
        
    	default Map<String,Serializable> asMap() {
    	    return ImmutableMap.<String,Serializable>builder()
    	    .put("clusterName", getClusterName())
    	    .put("ipAddress", getAddress())
    	    .put("port", getPort())
    	    .put("open", isOpen())
    	    .put("numberOfNodes", getNumberOfNodes())
    	    .put("receivedBytes", getReceivedBytes())
    	    .put("receivedMessages", getReceivedMessages())
    	    .put("sentMessages", getSentMessages())
    	    .put("sentBytes", getSentBytes())
    	    .put("cacheTransport", getCacheTransport())
    	    .put("licenseId", getLicenseId())
    	    .put("serverId", getServerId())
    	    .build();
    	}
    	
    	
    	
    }
    
    
    

    default CacheTransportInfo getInfo() {
        

        return new CacheTransportInfo(){

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