package com.dotcms.publisher.endpoint.bean.factory;

import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.AWSS3PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.StaticPublishingEndPoint;
import com.dotcms.publisher.pusher.PushPublisher;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PublishingEndPointFactoryTest {

    @Test
    public void getPublishingEndPoint_whenProtocolNotSet_returnNull() throws Exception {
        PublishingEndPointFactory factory = new PublishingEndPointFactory();
        assertNull(factory.getPublishingEndPoint(""));
    }

    @Test
    public void getPublishingEndPoint_whenProtocolIsHttp_return_PushPublishingEndPoint() throws Exception {
        PublishingEndPointFactory factory = new PublishingEndPointFactory();
        PublishingEndPoint endPoint = factory.getPublishingEndPoint(PushPublisher.PROTOCOL_HTTP);
        assertTrue(endPoint instanceof PushPublishingEndPoint);
    }

    @Test
    public void getPublishingEndPoint_whenProtocolIsHttpS_return_PushPublishingEndPoint() throws Exception {
        PublishingEndPointFactory factory = new PublishingEndPointFactory();
        PublishingEndPoint endPoint = factory.getPublishingEndPoint(PushPublisher.PROTOCOL_HTTPS);
        assertTrue(endPoint instanceof PushPublishingEndPoint);
    }

    @Test
    public void getPublishingEndPoint_whenProtocolIsAWS_S3_return_AWSS3PublishingEndPoint() throws Exception {
        PublishingEndPointFactory factory = new PublishingEndPointFactory();
        PublishingEndPoint endPoint = factory.getPublishingEndPoint(AWSS3Publisher.PROTOCOL_AWS_S3);
        assertTrue(endPoint instanceof AWSS3PublishingEndPoint);
    }

    @Test
    public void getPublishingEndPoint_whenProtocolIsStatic_return_StaticPublishingEndPoint() throws Exception {
        PublishingEndPointFactory factory = new PublishingEndPointFactory();
        PublishingEndPoint endPoint = factory.getPublishingEndPoint(StaticPublisher.PROTOCOL_STATIC);
        assertTrue(endPoint instanceof StaticPublishingEndPoint);
    }

}