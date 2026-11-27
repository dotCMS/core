package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.Pagination;
import com.dotcms.rest.ResponseEntityListView;
import java.util.List;

/**
 * Response of {@code GET /api/v1/contenttype/_writable}: a page of the content types the current
 * user can write to, with the page, its size and the total number of matching types.
 */
public class ResponseEntityContentTypeSummaryListView extends ResponseEntityListView<ContentTypeSummaryView> {

    /**
     * Wraps a page of content types.
     *
     * @param contentTypes the content types of this page
     * @param pagination   the page, its size and the total number of matching types
     */
    public ResponseEntityContentTypeSummaryListView(final List<ContentTypeSummaryView> contentTypes,
            final Pagination pagination) {
        super(contentTypes, pagination);
    }
}
