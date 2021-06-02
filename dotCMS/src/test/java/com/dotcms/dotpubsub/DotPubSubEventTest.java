package com.dotcms.dotpubsub;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;

public class DotPubSubEventTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @Test
    public void test_DotPubSubEvent_to_json_and_back() throws Exception {

        String message = "message";
        String type = "type";
        String origin = UUIDGenerator.generateUuid();
        String topic = "TOPIC";

        
        DotPubSubEvent event = new DotPubSubEvent.Builder().withMessage(message).withTopic(topic)
                .build();

        String eventToJson = event.toString();

        DotPubSubEvent serializedEvent = new DotPubSubEvent(eventToJson);

        assert (serializedEvent.equals(event));


        
        event = new DotPubSubEvent.Builder(event).withOrigin(origin).build();

        eventToJson = event.toString();

        serializedEvent = new DotPubSubEvent(eventToJson);

        assert (serializedEvent.equals(event));


        
        event = new DotPubSubEvent.Builder(event).withType(type).build();

        eventToJson = event.toString();

        serializedEvent = new DotPubSubEvent(eventToJson);

        assert (serializedEvent.equals(event));
        
        
    }

    
    @Test
    public void test_DotPubSubEvent_Builder() throws Exception {

        String message = "message";
        String type = "type";
        String topic = "TOPIC";
        String origin = UUIDGenerator.generateUuid();

        
        DotPubSubEvent event = new DotPubSubEvent
                        .Builder()
                        .withMessage(message)
                        .withOrigin(origin)
                        .withType(type)
                        .withTopic(topic)
                       
                        .build();

        assert event.getMessage().equals(message);

        // origin ID is shortified 
        assert event.getOrigin().equals(StringUtils.shortify(origin,10));
        
        // topics are lowercased
        assert event.getTopic().equals(topic.toLowerCase());

        assert event.getType().equals(type);

        
        
    }
    
    
    
    
}
