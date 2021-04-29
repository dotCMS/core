package com.dotcms.dotpubsub;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.cache.transport.postgres.CachePubSubTopic;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.UUIDGenerator;

public class PostgresPubSubImplTest {

    static PostgresPubSubImpl pubsub;
    static DotPubSubTopic topic;
    
    static String serverId1 = UUIDGenerator.generateUuid();
    
    
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        IntegrationTestInitService.getInstance().init();

        
        pubsub = new PostgresPubSubImpl(serverId1);

        topic = new CachePubSubTopic();

        pubsub.subscribe(topic);

        pubsub.start();

    }

    
    @Test
    public void test_ping_pong() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name()).build();

        
        long messagesSent = pubsub.getMessagesSent();
        
        long messagesRecieved= pubsub.getMessagesRecieved();
        pubsub.publish(topic, event);
        
        assert(pubsub.lastEventOut.getType().equalsIgnoreCase("ping"));
        
        
        Thread.sleep(2000);
        
        
        assert(pubsub.lastEventOut.getType().equalsIgnoreCase("pong"));

        assert(messagesSent < pubsub.getMessagesSent());

        assert(messagesRecieved < pubsub.getMessagesRecieved());
        Thread.sleep(2000);
    }
    
    
    @Test
    public void test_cluster_request_response() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.CLUSTER_REQ.name() ).build();

        
        long messagesSent = pubsub.getMessagesSent();
        
        long messagesRecieved= pubsub.getMessagesRecieved();
        pubsub.publish(topic, event);
        
        Thread.sleep(2000);
        
        assert(messagesSent < pubsub.getMessagesSent());

        assert(messagesRecieved < pubsub.getMessagesRecieved());
        
    }
    
    
    @Test
    public void test_reconnection() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name() ).build();

        
        long messagesSent = pubsub.getMessagesSent();
        
        long messagesRecieved= pubsub.getMessagesRecieved();
        pubsub.publish(topic, event);
        
        
        Thread.sleep(2000);
        
        assert(messagesSent < pubsub.getMessagesSent());

        assert(messagesRecieved < pubsub.getMessagesRecieved());
        
        pubsub.restart();
        
        pubsub.publish(topic, event);
        Thread.sleep(2000);
        assert(messagesSent < pubsub.getMessagesSent());

        assert(messagesRecieved < pubsub.getMessagesRecieved());
        
    }
    
    
    

}
