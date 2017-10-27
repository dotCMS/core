package com.dotcms.publisher.endpoint.bean.impl;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotmarketing.exception.PublishingEndPointValidationException;

/**
 * Implementation of {@link PublishingEndPoint} for old fashion Push Publish.
 */
public class PushPublishingEndPoint extends PublishingEndPoint {

    @Override
    public Class getPublisher() {
        return PushPublisher.class;
    }// getPublisher.

    @Override
    public void validatePublishingEndPoint() throws PublishingEndPointValidationException {
        //No need to validate anything.
    }// validatePublishingEndPoint.

}
