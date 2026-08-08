package com.dotcms.rest.api.v1.folder;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.util.PaginatedArrayList;

/**
 * Entity View for the paginated response of {@code GET /api/v1/folder/search}.
 *
 * <p>Exists so the response shape of that endpoint is expressed in the OpenAPI schema: the endpoint
 * returns the generic {@link com.dotcms.rest.ResponseEntityView} wrapper whose {@code entity} is a
 * {@link PaginatedArrayList} of {@link FolderSearchView}, which a generic view cannot describe.
 */
public class ResponseEntityFolderSearchView extends ResponseEntityView<PaginatedArrayList<FolderSearchView>> {

    public ResponseEntityFolderSearchView(final PaginatedArrayList<FolderSearchView> entity) {
        super(entity);
    }
}
