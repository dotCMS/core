package com.dotcms.publisher.endpoint.bean.impl;

import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import java.io.File;

public class StaticPublishingEndPoint extends PublishingEndPoint {

    @Override
    public Class getPublisher() {
        return StaticPublisher.class;
    }

    @Override
    public void validatePublishingEndPoint() throws DotDataException, LanguageException {
        final String staticPublishPath = ConfigUtils.getStaticPublishPath();
        final File staticPublishFolder = new File(staticPublishPath);

        if (!staticPublishFolder.canRead() || !staticPublishFolder.canWrite()) {
            throw new DotDataException(
                    LanguageUtil.get("publisher_Endpoint_type_static_cant_read_write"));
        }
    }

}
