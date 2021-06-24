package com.dotcms.dotpubsub;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import com.dotmarketing.db.DbConnectionFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.cache.transport.CacheTransportTopic;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;

public class PostgresPubSubImplTest {

    static PostgresPubSubImpl pubsubA,pubsubB;
    static DotPubSubTopic topicA;
    
    static String fakeServerA = "fakeServerA";
    static String fakeServerB = "fakeServerB";
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        IntegrationTestInitService.getInstance().init();

        assumeTrue(isPostgres());

        pubsubA = new PostgresPubSubImpl(fakeServerA);
        topicA = new CacheTransportTopic(fakeServerA,pubsubA);
        pubsubA.subscribe(topicA);
        pubsubA.start();


        
    }


    @Test
    public void test_ping() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder()
                        .withType(CacheTransportTopic.CacheEventType.PING.name())
                        .withTopic(topicA)
                        .build();

        
        long messagesSent = topicA.messagesSent();
        long messagesRecieved= topicA.messagesReceived();
        
        
        pubsubA.publish(event);
        
        
        Thread.sleep(2000);

        assert(messagesSent < topicA.messagesSent());
        assert(messagesRecieved < topicA.messagesReceived());
        

    }
    
    
    @Test
    public void test_cluster_request_response() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CacheTransportTopic.CacheEventType.CLUSTER_REQ.name())
                        .withTopic(topicA).build();


        long messagesSent = topicA.messagesSent();
        long messagesRecieved= topicA.messagesReceived();
        
        pubsubA.publish(event);
        
        Thread.sleep(2000);
        
        assert(messagesSent < topicA.messagesSent());
        assert(messagesRecieved < topicA.messagesReceived());

        
    }
    
    
    @Test
    public void test_reconnection() throws Exception{

        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CacheTransportTopic.CacheEventType.PING.name() ).withTopic(topicA).build();

        
        long messagesSent = topicA.messagesSent();
        long messagesRecieved= topicA.messagesReceived();
        pubsubA.publish(event);
        
        
        Thread.sleep(2000);
        
        assert(messagesSent < topicA.messagesSent());
        assert(messagesRecieved < topicA.messagesReceived());
        
        messagesSent = topicA.messagesSent();
        messagesRecieved= topicA.messagesReceived();
        
        pubsubA.restart();
        
        pubsubA.publish(event);
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
        
        DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CacheTransportTopic.CacheEventType.PING.name() ).withTopic(fakeTopic).build();

        // publish to fake topic
        pubsubA.publish(event);

        
        Thread.sleep(2000);
        
        // got a faketopic event
        assert(messagesRecieved < fakeTopic.messagesReceived());
       
        event.equals(fakeTopic.lastEvent);

        
        messagesRecieved= fakeTopic.messagesReceived();
        
        
        // unsubscribe from fakeTopic
        pubsubA.unsubscribe(fakeTopic);
        
        // publishing an event does nothing
        pubsubA.publish(event);
        
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
    
    
    
    
    @Test
    public void test_sending_a_large_message_fails_and_recovers() throws Exception{
        
        
        // create a fake topic and subscribe to it
        FakeDotPubSubTopic fakeTopic = new FakeDotPubSubTopic(); 
        pubsubA.subscribe(fakeTopic);
        DotPubSubEvent workingEvent = new DotPubSubEvent.Builder()
                        .withType(CacheTransportTopic.CacheEventType.UKN.name())
                                        .withTopic(fakeTopic)
                                        .withMessage(RandomStringUtils.randomAlphabetic(7500))
                                        .build();
        
        DotPubSubEvent tooBigEvent = new DotPubSubEvent.Builder()
                        .withType(CacheTransportTopic.CacheEventType.UKN.name())
                                        .withTopic(fakeTopic)
                                        .withMessage(RandomStringUtils.randomAlphabetic(9000))
                                        .build();
        
        
        assert(pubsubA.publish(workingEvent));
            

        assertFalse(pubsubA.publish(tooBigEvent));
            

       

        
        assert(pubsubA.publish(workingEvent));
            
        
        
        
    }


    private static boolean isPostgres() {
        return DbConnectionFactory.isPostgres() && DbConnectionFactory.getDataSource() instanceof HikariDataSource;
    }
    
    
    
    
    
    
    

}
