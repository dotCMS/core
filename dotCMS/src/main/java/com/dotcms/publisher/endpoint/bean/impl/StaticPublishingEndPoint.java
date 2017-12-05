package com.dotcms.publisher.endpoint.bean.impl;

import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.PublishingEndPointValidationException;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;

/**
 * Implementation of {@link PublishingEndPoint} for Static/Local Publish.
 */
public class StaticPublishingEndPoint extends PublishingEndPoint {

    @Override
    public Class getPublisher() {
        return StaticPublisher.class;
    }

    @Override
    public void validatePublishingEndPoint() throws PublishingEndPointValidationException {
        final String staticPublishPath = ConfigUtils.getStaticPublishPath();
        final File staticPublishFolder = new File(staticPublishPath);

        //We need to validate read/write permission under the path user wants to store the bundles.
        if (!staticPublishFolder.canRead() || !staticPublishFolder.canWrite()) {
            throw new PublishingEndPointValidationException(
                    "publisher_Endpoint_type_static_cant_read_write");
        }
    }

}
