package com.dotcms.rest.api.v1.categories;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for category responses.
 * Contains individual category information including hierarchy and metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityCategoryView extends ResponseEntityView<CategoryView> {
    public ResponseEntityCategoryView(final CategoryView entity) {
        super(entity);
    }
}