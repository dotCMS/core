package com.dotcms.rest.api.v1.categories;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for category responses with child count information.
 * Contains category data along with the count of child categories.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityCategoryWithChildCountView extends ResponseEntityView<CategoryWithChildCountView> {
    public ResponseEntityCategoryWithChildCountView(final CategoryWithChildCountView entity) {
        super(entity);
    }
}