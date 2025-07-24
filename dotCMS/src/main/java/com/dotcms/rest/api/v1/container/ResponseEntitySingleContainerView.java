package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.containers.model.ContainerView;

/**
 * Entity View for single container responses.
 * Contains individual container information including metadata and configuration.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySingleContainerView extends ResponseEntityView<ContainerView> {
    public ResponseEntitySingleContainerView(final ContainerView entity) {
        super(entity);
    }
}