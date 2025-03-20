package com.dotcms.rest;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;

/**
 * View class for Environment/Endpoint
 * @author jsanca
 */
public class ResponseEntityEndpointView extends ResponseEntityView<PublishingEndPoint> {

    public ResponseEntityEndpointView(final PublishingEndPoint entity) {
        super(entity);
    }
}
