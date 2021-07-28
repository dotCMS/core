package com.dotcms.publisher.receiver;

import com.dotcms.IntegrationTestBase;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishEndOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishFailureOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishStartOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishSuccessOnReceiverEvent;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestBundlePublisher extends IntegrationTestBase {

    public TestBundlePublisher() {
    }

    @BeforeClass
    public static void prepare() throws Exception{

        IntegrationTestInitService.getInstance().init();
        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        localSystemEventsAPI.subscribe(PushPublishStartOnReceiverEvent.class, (EventSubscriber<PushPublishStartOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishStartOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

        localSystemEventsAPI.subscribe(PushPublishFailureOnReceiverEvent.class, (EventSubscriber<PushPublishFailureOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishFailureOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

        localSystemEventsAPI.subscribe(PushPublishSuccessOnReceiverEvent.class, (EventSubscriber<PushPublishSuccessOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishSuccessOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

        localSystemEventsAPI.subscribe(PushPublishEndOnReceiverEvent.class, (EventSubscriber<PushPublishEndOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishEndOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

    }

    @Test
    public void trigger_events()  {

        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        final List<PublishQueueElement> publishQueueElements = new ArrayList<>();
        PublishQueueElement publishQueueElement = new PublishQueueElement();
        publishQueueElements.add(publishQueueElement);
        final PublisherConfig publisherConfig = new PublisherConfig();
        publisherConfig.setAssets(publishQueueElements);
        localSystemEventsAPI.notify(new PushPublishStartOnReceiverEvent(publishQueueElements));
        localSystemEventsAPI.notify(new PushPublishFailureOnReceiverEvent(publishQueueElements));
        localSystemEventsAPI.notify(new PushPublishSuccessOnReceiverEvent(publisherConfig));
        localSystemEventsAPI.notify(new PushPublishEndOnReceiverEvent(publishQueueElements));
    }

}
