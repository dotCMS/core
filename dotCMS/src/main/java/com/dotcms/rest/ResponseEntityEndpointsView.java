package com.dotcms.rest;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;

import java.util.Collection;

/**
 * Encapsulates the data of an collectios of endpoints
 * @author jsanca
 */
public class ResponseEntityEndpointsView extends ResponseEntityView<Collection<PublishingEndPoint>> {
    public ResponseEntityEndpointsView(final Collection<PublishingEndPoint> entity) {
        super(entity);
    }
}
