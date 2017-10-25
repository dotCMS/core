package com.dotcms.publisher.endpoint.bean.factory;

import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.AWSS3PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.StaticPublishingEndPoint;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotmarketing.util.UtilMethods;

public class PublishingEndPointFactory {

    public PublishingEndPoint getPublishingEndPoint(final String protocol) {
        if (UtilMethods.isSet(protocol)) {
            return null;
        } else if (PushPublisher.PROTOCOL_HTTP.equalsIgnoreCase(protocol)
                || PushPublisher.PROTOCOL_HTTP.equalsIgnoreCase(protocol)) {
            return new PushPublishingEndPoint();
        } else if (AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(protocol)) {
            return new AWSS3PublishingEndPoint();
        } else if (StaticPublisher.PROTOCOL_STATIC.equalsIgnoreCase(protocol)) {
            return new StaticPublishingEndPoint();
        }

        return null;
    }
}
