package com.dotcms.rest.api.v1.folder;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for folder search result responses.
 * Contains lists of folder search results with metadata including path, hostname, and permissions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFolderSearchResultView extends ResponseEntityView<List<FolderSearchResultView>> {
    public ResponseEntityFolderSearchResultView(final List<FolderSearchResultView> entity) {
        super(entity);
    }
}