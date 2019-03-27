package com.dotcms.publisher.endpoint.bean.factory;

import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.AWSS3PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.StaticPublishingEndPoint;

/**
 * Factory class to return:
 * 1. AWSS3PublishingEndPoint if protocol is awss3.
 * 2. StaticPublishingEndPoint if protocol is static.
 * 3. PushPublishingEndPoint if protocol is http, https or null
 */
public class PublishingEndPointFactory {

    public PublishingEndPoint getPublishingEndPoint(final String protocol) {

        if (AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(protocol)) {
            return new AWSS3PublishingEndPoint();
        } else if (StaticPublisher.PROTOCOL_STATIC.equalsIgnoreCase(protocol)) {
            return new StaticPublishingEndPoint();
        }

        return new PushPublishingEndPoint(); //No protocol, PROTOCOL_HTTP, PROTOCOL_HTTPS
    }
}
