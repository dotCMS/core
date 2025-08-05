package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for container with content types responses.
 * Contains container information along with associated content type structures.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContainerWithContentTypesView extends ResponseEntityView<ContainerWithContentTypesView> {
    public ResponseEntityContainerWithContentTypesView(final ContainerWithContentTypesView entity) {
        super(entity);
    }
}