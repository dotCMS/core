package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.rest.Pagination;
import com.dotcms.rest.ResponseEntityListView;
import java.util.List;

/**
 * Response of {@code GET /api/v1/content/_import/contenttypes}: a page of the content types the
 * current user can import content into, with the page, its size and the total number of matching
 * types.
 */
public class ResponseEntityImportContentTypeListView extends ResponseEntityListView<ImportContentTypeView> {

    /**
     * Wraps a page of content types.
     *
     * @param contentTypes the content types of this page
     * @param pagination   the page, its size and the total number of matching types
     */
    public ResponseEntityImportContentTypeListView(final List<ImportContentTypeView> contentTypes,
            final Pagination pagination) {
        super(contentTypes, pagination);
    }
}
