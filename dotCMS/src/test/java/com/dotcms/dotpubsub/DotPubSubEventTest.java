package com.dotcms.dotpubsub;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.util.UUIDGenerator;

public class DotPubSubEventTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @Test
    public void test_DotPubSubEvent_to_json_and_back() throws Exception {

        String message = "message";
        String type = "type";
        String origin = UUIDGenerator.generateUuid();

        
        DotPubSubEvent event = new DotPubSubEvent.Builder().withMessage(message).build();

        String eventToString = event.toString();

        DotPubSubEvent serializedEvent = new DotPubSubEvent(eventToString);

        assert (serializedEvent.equals(event));


        
        event = new DotPubSubEvent.Builder(event).withOrigin(origin).build();

        eventToString = event.toString();

        serializedEvent = new DotPubSubEvent(eventToString);

        assert (serializedEvent.equals(event));


        
        event = new DotPubSubEvent.Builder(event).withType(type).build();

        eventToString = event.toString();

        serializedEvent = new DotPubSubEvent(eventToString);

        assert (serializedEvent.equals(event));
        
        
    }

}
