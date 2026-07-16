package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for single push publishing filter descriptor responses.
 * Contains a specific push publishing filter configuration.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFilterDescriptorView extends ResponseEntityView<FilterDescriptor> {
    public ResponseEntityFilterDescriptorView(final FilterDescriptor entity) {
        super(entity);
    }
}