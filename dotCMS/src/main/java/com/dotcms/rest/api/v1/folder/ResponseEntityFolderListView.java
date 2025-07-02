package com.dotcms.rest.api.v1.folder;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for folder list responses.
 * Contains collections of folder data maps for bulk operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFolderListView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityFolderListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}