package com.dotcms.rest.api.v2.contenttype;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for field operation responses.
 * Contains operation results including deleted IDs and remaining fields.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFieldOperationView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityFieldOperationView(final Map<String, Object> entity) {
        super(entity);
    }
}