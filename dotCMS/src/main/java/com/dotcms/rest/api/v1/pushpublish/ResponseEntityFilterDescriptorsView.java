package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for push publishing filter descriptors list responses.
 * Contains a list of available push publishing filters.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFilterDescriptorsView extends ResponseEntityView<List<FilterDescriptor>> {
    public ResponseEntityFilterDescriptorsView(final List<FilterDescriptor> entity) {
        super(entity);
    }
}