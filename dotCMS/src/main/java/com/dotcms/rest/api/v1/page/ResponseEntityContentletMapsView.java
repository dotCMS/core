package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;
import java.util.Set;

/**
 * Entity View for contentlet maps collection responses.
 * Contains a set of contentlet data maps for page content retrieval.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContentletMapsView extends ResponseEntityView<Set<Map<String, Object>>> {
    public ResponseEntityContentletMapsView(final Set<Map<String, Object>> entity) {
        super(entity);
    }
}