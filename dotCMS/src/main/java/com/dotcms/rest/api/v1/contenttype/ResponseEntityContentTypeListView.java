package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for content type list responses.
 * Contains collections of content type data maps for bulk operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContentTypeListView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityContentTypeListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}