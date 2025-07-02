package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for page copy operation responses.
 * Contains information about copied page content and operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPageCopyView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityPageCopyView(final Map<String, Object> entity) {
        super(entity);
    }
}