package com.dotcms.publisher.endpoint.bean.impl;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.PublishingEndPointValidationException;
import com.dotmarketing.util.UtilMethods;

/**
 * Implementation of {@link PublishingEndPoint} for old fashion Push Publish.
 */
public class PushPublishingEndPoint extends PublishingEndPoint {

    @Override
    public Class getPublisher() {
        return PushPublisher.class;
    }// getPublisher.

    /**
     * Requires a key: the receiver checks every request against it, so an endpoint without one
     * cannot be used. A key made only of spaces counts as none.
     *
     * @throws PublishingEndPointValidationException the endpoint has no key
     */
    @Override
    public void validatePublishingEndPoint() throws PublishingEndPointValidationException {
        if (getAuthKey() == null || !UtilMethods.isSet(getAuthKey().toString())
                || !UtilMethods.isSet(PublicEncryptionFactory.decryptString(getAuthKey().toString()))) {
            throw new PublishingEndPointValidationException("publisher_Endpoint_authKey_required");
        }
    }// validatePublishingEndPoint.

}
