package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;
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

    /**
     * Error-only constructor — used when the operation failed before a Content Type could be
     * returned. The entity is {@code null}; all error detail lives in the {@code errors} list.
     *
     * @param errors list of errors describing what went wrong
     */
    public ResponseEntityContentTypeDetailView(final List<ErrorEntity> errors) {
        super(errors, (Map<String, Object>) null);
    }
}