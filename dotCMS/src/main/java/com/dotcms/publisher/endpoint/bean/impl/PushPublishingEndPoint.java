package com.dotcms.publisher.endpoint.bean.impl;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.language.LanguageException;

public class PushPublishingEndPoint extends PublishingEndPoint {

    @Override
    public Class getPublisher() {
        return PushPublisher.class;
    }

    @Override
    public void validatePublishingEndPoint() throws DotDataException, LanguageException {
        //No need to validate anything.
    }

}
