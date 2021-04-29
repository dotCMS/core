package com.dotcms.dotpubsub;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.cache.transport.postgres.CachePubSubTopic;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.UUIDGenerator;

public class PostgresPubSubImplTest {

    static PostgresPubSubImpl pubsub;
    static DotPubSubTopic topic;
    
    static String fakeServerId = UUIDGenerator.generateUuid();
    
    
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        IntegrationTestInitService.getInstance().init();

        
        pubsub = new PostgresPubSubImpl(fakeServerId);

        topic = new CachePubSubTopic(fakeServerId);

        pubsub.subscribe(topic);

        pubsub.start();

    }

    
    @Test
    public void test_ping_pong() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name()).build();

        
        long messagesSent = topic.messagesSent();
        long messagesRecieved= topic.messagesRecieved();
        
        
        pubsub.publish(topic, event);
        
        assert(pubsub.lastEventOut.getType().equalsIgnoreCase("ping"));
        
        
        Thread.sleep(2000);
        
        
        assert(pubsub.lastEventOut.getType().equalsIgnoreCase("pong"));

        assert(messagesSent < topic.messagesSent());
        assert(messagesRecieved < topic.messagesRecieved());
        
        Thread.sleep(2000);
    }
    
    
    @Test
    public void test_cluster_request_response() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.CLUSTER_REQ.name() ).build();

        
        long messagesSent = topic.messagesSent();
        long messagesRecieved= topic.messagesRecieved();
        
        pubsub.publish(topic, event);
        
        Thread.sleep(2000);
        
        assert(messagesSent < topic.messagesSent());
        assert(messagesRecieved < topic.messagesRecieved());
        
    }
    
    
    @Test
    public void test_reconnection() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name() ).build();

        
        long messagesSent = topic.messagesSent();
        long messagesRecieved= topic.messagesRecieved();
        pubsub.publish(topic, event);
        
        
        Thread.sleep(2000);
        
        assert(messagesSent < topic.messagesSent());
        assert(messagesRecieved < topic.messagesRecieved());
        
        messagesSent = topic.messagesSent();
        messagesRecieved= topic.messagesRecieved();
        
        pubsub.restart();
        
        pubsub.publish(topic, event);
        Thread.sleep(2000);
        
        assert(messagesSent < topic.messagesSent());
        assert(messagesRecieved < topic.messagesRecieved());
        
    }
    
    
    

}
