package com.dotcms.dotpubsub;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.cache.transport.postgres.CachePubSubTopic;
import com.dotcms.cache.transport.postgres.ServerResponseTopic;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;

public class PostgresPubSubImplTest {

    static PostgresPubSubImpl pubsubA,pubsubB;
    static DotPubSubTopic topicA,topicB;
    
    static String fakeServerA = "fakeServerA";
    static String fakeServerB = "fakeServerB";
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        IntegrationTestInitService.getInstance().init();

        pubsubA = new PostgresPubSubImpl(fakeServerA);
        topicA = new CachePubSubTopic(fakeServerA);
        topicB = new ServerResponseTopic();
        pubsubA.subscribe(topicA);
        pubsubA.subscribe(topicB);
        pubsubA.start();

        
        
    }

    
    @Test
    public void test_ping() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name()).build();

        
        long messagesSent = topicA.messagesSent();
        long messagesRecieved= topicA.messagesReceived();
        
        
        pubsubA.publish(topicA, event);
        
        assert(pubsubA.lastEventOut().getType().equalsIgnoreCase(CachePubSubTopic.CacheEventType.PING.name()));
        
        
        Thread.sleep(2000);

        assert(messagesSent < topicA.messagesSent());
        assert(messagesRecieved < topicA.messagesReceived());
        

    }
    
    
    @Test
    public void test_cluster_request_response() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.CLUSTER_REQ.name() ).build();

        
        long messagesSent = topicA.messagesSent();
        long messagesRecieved= topicA.messagesReceived();
        
        pubsubA.publish(topicA, event);
        
        Thread.sleep(2000);
        
        assert(messagesSent < topicA.messagesSent());
        assert(messagesRecieved < topicA.messagesReceived());

        
    }
    
    
    @Test
    public void test_reconnection() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name() ).build();

        
        long messagesSent = topicA.messagesSent();
        long messagesRecieved= topicA.messagesReceived();
        pubsubA.publish(topicA, event);
        
        
        Thread.sleep(2000);
        
        assert(messagesSent < topicA.messagesSent());
        assert(messagesRecieved < topicA.messagesReceived());
        
        messagesSent = topicA.messagesSent();
        messagesRecieved= topicA.messagesReceived();
        
        pubsubA.restart();
        
        pubsubA.publish(topicA, event);
        Thread.sleep(2000);
        
        assert(messagesSent < topicA.messagesSent());
        assert(messagesRecieved < topicA.messagesReceived());
        
    }
    
    
    
    
    
    @Test
    public void test_adding_removing_topic() throws Exception{

        // create a fake topic and subscribe to it
        FakeDotPubSubTopic fakeTopic = new FakeDotPubSubTopic(); 
        pubsubA.subscribe(fakeTopic);
        
        long messagesRecieved = fakeTopic.messagesReceived();
        
        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CachePubSubTopic.CacheEventType.PING.name() ).build();

        // publish to fake topic
        pubsubA.publish(fakeTopic, event);

        
        Thread.sleep(2000);
        
        // got a faketopic event
        assert(messagesRecieved < fakeTopic.messagesReceived());
       
        event.equals(fakeTopic.lastEvent);

        
        messagesRecieved= fakeTopic.messagesReceived();
        
        
        // unsubscribe from fakeTopic
        pubsubA.unsubscribe(fakeTopic);
        
        // publishing an event does nothing
        pubsubA.publish(fakeTopic, event);
        
        Thread.sleep(2000);
        
        // NO event received, nothing has been incremented
        assert(messagesRecieved == fakeTopic.messagesReceived());

    }
    
    
    
    

    class FakeDotPubSubTopic implements DotPubSubTopic {
        DotPubSubEvent lastEvent=null;
        int messagesSent, messagesRecieved = 0;

        @Override
        public Comparable getKey() {
            return "faketopic";
        }

        @Override
        public long messagesSent() {
            return messagesSent;
        }

        @Override
        public long messagesReceived() {

            return messagesRecieved;
        }

        @Override
        public void incrementSentCounters(DotPubSubEvent event) {
            messagesSent++;

        }

        @Override
        public void incrementReceivedCounters(DotPubSubEvent event) {
            messagesRecieved++;
            

        }

        @Override
        public void notify(DotPubSubEvent event) {
            incrementReceivedCounters(event);
            this.lastEvent = event;
            Logger.info(this.getClass(), "got FAKE event:" + event);

        }

    };
    
    
    
    
    
    
    
    
    

}
