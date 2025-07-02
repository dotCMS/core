package com.dotcms.rest.api.v1.folder;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for folder paths collection responses.
 * Contains lists of folder paths for deletion and path-based operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFolderPathsView extends ResponseEntityView<List<String>> {
    public ResponseEntityFolderPathsView(final List<String> entity) {
        super(entity);
    }
}