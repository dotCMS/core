package com.dotcms.contenttype.model.field;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for field type collection responses.
 * Contains list of field type configurations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFieldTypeListView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityFieldTypeListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}
