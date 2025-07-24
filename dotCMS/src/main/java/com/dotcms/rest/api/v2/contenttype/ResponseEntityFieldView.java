package com.dotcms.rest.api.v2.contenttype;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for single field responses.
 * Contains individual field information as JSON map.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFieldView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityFieldView(final Map<String, Object> entity) {
        super(entity);
    }
}