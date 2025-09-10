package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for detailed content type responses.
 * Contains comprehensive content type information including workflow actions and system mappings.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContentTypeDetailView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityContentTypeDetailView(final Map<String, Object> entity) {
        super(entity);
    }
    
    public ResponseEntityContentTypeDetailView(final Map<String, Object> entity, final String[] permissions) {
        super(entity, permissions);
    }
}