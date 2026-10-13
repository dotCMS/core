package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.rest.ResponseEntityListView;
import java.util.List;

/**
 * Response of {@code GET /api/v1/content/_import/contenttypes}: the content types the current user
 * can import content into.
 */
public class ResponseEntityImportContentTypeListView extends ResponseEntityListView<ImportContentTypeView> {

    /**
     * Wraps the list of content types.
     *
     * @param contentTypes the content types the user can import into
     */
    public ResponseEntityImportContentTypeListView(final List<ImportContentTypeView> contentTypes) {
        super(contentTypes);
    }
}
