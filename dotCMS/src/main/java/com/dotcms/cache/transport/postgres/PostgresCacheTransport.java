package com.dotcms.cache.transport.postgres;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.dotcms.cache.transport.postgres.CachePubSubTopic.CacheEventType;
import com.dotcms.cluster.bean.Server;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotcms.dotpubsub.PostgresPubSubImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.util.Logger;

public class PostgresCacheTransport implements CacheTransport {

    PostgresPubSubImpl pubsub;

    DotPubSubTopic topic;

    AtomicBoolean initialized = new AtomicBoolean(false);


    public PostgresCacheTransport() {

        Logger.info(this.getClass(), "PostgresCacheTransport");

    }



    @Override
    public void init(Server localServer) throws CacheTransportException {

        Logger.info(this.getClass(), "calling init");
        this.pubsub = new PostgresPubSubImpl();

        this.topic = new CachePubSubTopic();

        this.pubsub.subscribe(topic);

        this.pubsub.init();
        initialized.set(true);
    }



    @Override
    public void send(String message) throws CacheTransportException {


        final DotPubSubEvent event =
                        new DotPubSubEvent.Builder().withType(CacheEventType.INVAL.name()).withMessage(message).build();

        this.pubsub.publish(this.topic, event);


    }



    @Override
    public void testCluster() throws CacheTransportException {

        final DotPubSubEvent event =
                        new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name()).build();
        Logger.info(this.getClass(), "Testing cluster, sending PING from server:" + event.getOrigin() + ".");

        this.pubsub.publish(this.topic, event);

    }



    @Override
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)
                    throws CacheTransportException {

        final DotPubSubEvent event = new DotPubSubEvent.Builder()
                        .withType(CachePubSubTopic.CacheEventType.CLUSTER_REQ.name()).build();
        this.pubsub.publish(this.topic, event);
        return null;
    }



    @Override
    public void shutdown() throws CacheTransportException {
        Logger.info(this.getClass(), "shutdown()");
        this.pubsub.shutdown();
    }



    @Override
    public boolean isInitialized() {
        Logger.info(this.getClass(), "isInitialized");
        return initialized.get();
    }



    @Override
    public boolean shouldReinit() {

        return !initialized.get();
    }



    @Override
    public CacheTransportInfo getInfo() {
        return null;
    }



}
