package com.dotcms.rest.api.v2.contenttype;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for field list responses.
 * Contains lists of fields as JSON maps.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFieldListView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityFieldListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}