package com.dotcms.rest.api.v1.categories;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for the category bulk delete response.
 * Carries the per-item outcome plus the total number of categories removed by the cascade.
 *
 * @author dotCMS
 */
public class ResponseEntityCategoryBulkDeleteResultView extends ResponseEntityView<CategoryBulkDeleteResultView> {
    public ResponseEntityCategoryBulkDeleteResultView(final CategoryBulkDeleteResultView entity) {
        super(entity);
    }
}
