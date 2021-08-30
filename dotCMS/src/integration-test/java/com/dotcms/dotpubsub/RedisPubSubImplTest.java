package com.dotcms.dotpubsub;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.cache.transport.CacheTransportTopic;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;

public class RedisPubSubImplTest {
    
    static RedisGroupsPubSubImpl pubsubA,pubsubB;
    static DotPubSubTopic topicA;
    static final String clusterId = UUIDGenerator.shorty();
    static final String fakeServerA = UUIDGenerator.shorty();
    static final String fakeServerB = UUIDGenerator.shorty();
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        Config.setProperty("DOT_REDIS_CLIENT_URL", "redis://password@oboxturbo");

        pubsubA = new RedisGroupsPubSubImpl(fakeServerA,clusterId,true);
        
        pubsubB = new RedisGroupsPubSubImpl(fakeServerB,clusterId,true);
        
        topicA = new CacheTransportTopic(fakeServerA,pubsubA);
    }

    @Test
    public void test_adding_removing_topics()  throws Exception {
        
        pubsubA.subscribe(new CacheTransportTopic(fakeServerA,pubsubA));
        pubsubB.subscribe(new CacheTransportTopic(fakeServerB,pubsubB));
        
        assert(pubsubA.getTopics().size()==1);
        assert(pubsubB.getTopics().size()==1);

        pubsubA.unsubscribe(new CacheTransportTopic(fakeServerA,pubsubA));
        pubsubB.unsubscribe(new CacheTransportTopic(fakeServerB,pubsubB));

        assert(pubsubA.getTopics().size()==0);
        assert(pubsubB.getTopics().size()==0);
        
    }
    
    
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
