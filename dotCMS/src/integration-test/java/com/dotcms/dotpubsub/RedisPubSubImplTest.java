package com.dotcms.dotpubsub;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.cache.transport.CacheTransportTopic;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;

/**
 * Test for {@link RedisPubSubImpl}
 * @author jsanca
 */
public class RedisPubSubImplTest {
    
    static RedisPubSubImpl pubsubA,pubsubB;
    static RedisClient<String, Object> redisClient;
    static DotPubSubTopic topicA;
    static final String clusterId = UUIDGenerator.shorty();
    static final String fakeServerA = UUIDGenerator.shorty();
    static final String fakeServerB = UUIDGenerator.shorty();
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        pubsubA = new RedisPubSubImpl();
        
        pubsubB = new RedisPubSubImpl();
        
        topicA = new CacheTransportTopic(fakeServerA,pubsubA);

        redisClient = RedisClientFactory.getClient("pubsub");
    }

    /**
     * Method to test: {@link RedisPubSubImpl#subscribe(DotPubSubTopic)}
     * Given Scenario: subscribe two topics and verify all ok
     * ExpectedResult: after subscription, two subscribers should be add and one channel
     * after the unsubscription, no subscribers and not channel
     *
     */
    @Test
    public void test_adding_removing_topics()  throws Exception {

        final String channelName = "dotcache_topic";
        final CacheTransportTopic cacheTransportTopic1 = new CacheTransportTopic(fakeServerA,pubsubA);
        final CacheTransportTopic cacheTransportTopic2 = new CacheTransportTopic(fakeServerB,pubsubB);
        pubsubA.subscribe(cacheTransportTopic1);
        pubsubB.subscribe(cacheTransportTopic2);

        final Collection<String> channels    = redisClient.getChannels();
        final Collection<Object> subscribers =  redisClient.getSubscribers(channelName);

        assert(channels.size()==1);
        assert(channels.stream().anyMatch(channel -> channelName.equals(channel)));
        assert(subscribers.size()==2);

        pubsubA.unsubscribe(new CacheTransportTopic(fakeServerA,pubsubA));
        pubsubB.unsubscribe(new CacheTransportTopic(fakeServerB,pubsubB));

        final Collection<String> channelsAgain    = redisClient.getChannels();
        final Collection<Object> subscribersAgain =  redisClient.getSubscribers(channelName);

        assert(channelsAgain.size()==0);
        assert(subscribersAgain.size()==0);
    }

    /**
     * Method to test: {@link RedisPubSubImpl#subscribe(DotPubSubTopic)} and {@link RedisPubSubImpl#publish(DotPubSubEvent)}
     * Given Scenario: subscribe one topic and send a message
     * ExpectedResult: after sent a message the message receiver should be more than zero.
     *
     */
    @Test
    public void test_publisher()  throws Exception {

        // serverA subscribes to topis
        pubsubA.subscribe(topicA);
        
        Thread.sleep(3000);
        DotPubSubEvent event = new DotPubSubEvent.Builder().withTopic(topicA.getTopic()).withMessage("helloworld").build();
        
        // serverB sends a new event to topic
        pubsubB.publish(event);
        Thread.sleep(3000);
        
        assert(topicA.messagesReceived()>0);
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        
        pubsubA.stop();
        
        pubsubB.stop();
    }
    
    
    private static Map<String,String> getModifiableEnvironment() throws Exception{
        Class pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv");
        getenv.setAccessible(true);
        Object unmodifiableEnvironment = getenv.invoke(null);
        Class map = Class.forName("java.util.Collections$UnmodifiableMap");
        Field m = map.getDeclaredField("m");
        m.setAccessible(true);
        return (Map) m.get(unmodifiableEnvironment);
    }
}
