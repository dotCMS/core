package com.dotcms.publisher.endpoint.bean.impl;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotmarketing.exception.PublishingEndPointValidationException;
import com.dotmarketing.util.UtilMethods;
import java.io.IOException;

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
     * cannot be used. The key is resolved the same way push publishing resolves it at run time, so
     * a key made only of spaces, or a key file that is missing, unreadable or blank, counts as none.
     *
     * @throws PublishingEndPointValidationException the endpoint has no usable key
     */
    @Override
    public void validatePublishingEndPoint() throws PublishingEndPointValidationException {
        try {
            if (getAuthKey() != null && !UtilMethods.isEmpty(getAuthKey().toString())
                    && PushPublisher.retriveEndpointKey(this).isPresent()) {
                return;
            }
        } catch (final IOException e) {
            // an unreadable key file is no key
        }
        throw new PublishingEndPointValidationException("publisher_Endpoint_authKey_required");
    }// validatePublishingEndPoint.

}
