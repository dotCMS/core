package com.dotcms.cache.transport.postgres;

import java.util.Map;
import com.dotcms.cluster.bean.Server;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotcms.dotpubsub.PostgresPubSubImpl;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.util.Logger;

public class PostgresCacheTransport implements CacheTransport {

    PostgresPubSubImpl pubsub;



    public PostgresCacheTransport() {

        Logger.info(this.getClass(), "PostgresCacheTransport");

    }



    @Override
    public void init(Server localServer) throws CacheTransportException {

        Logger.info(this.getClass(), "calling init");
        this.pubsub = new PostgresPubSubImpl();
        
        
        DotPubSubTopic topic = new DotPubSubTopic() {
            
            @Override
            public Comparable getKey() {
                return APILocator.getShortyAPI().shortify(ClusterFactory.getClusterId()).toLowerCase();
            }
        };
        
        this.pubsub .subscribe(topic);
        
        this.pubsub.init();

    }



    @Override
    public void send(String message) throws CacheTransportException {
        this.pubsub.publish(null);
        Logger.info(this.getClass(), "calling end(String message)");

    }



    @Override
    public void testCluster() throws CacheTransportException {
        Logger.info(this.getClass(), "calling testCluster()");

    }



    @Override
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)
                    throws CacheTransportException {
        Logger.info(this.getClass(),
                        "validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)");
        return null;
    }



    @Override
    public void shutdown() throws CacheTransportException {
        Logger.info(this.getClass(), "shutdown()");

    }



    @Override
    public boolean isInitialized() {
        Logger.info(this.getClass(), "isInitialized");
        return false;
    }



    @Override
    public boolean shouldReinit() {
        Logger.info(this.getClass(), "shouldReinit");
        return false;
    }



    @Override
    public CacheTransportInfo getInfo() {
        Logger.info(this.getClass(), "getInfo");
        return null;
    }



}
