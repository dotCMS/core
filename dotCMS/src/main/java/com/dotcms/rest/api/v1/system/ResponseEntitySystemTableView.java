package com.dotcms.rest.api.v1.system;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for system table collection responses.
 * Contains Map of system table key-value pairs.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySystemTableView extends ResponseEntityView<Map<String, String>> {
    public ResponseEntitySystemTableView(final Map<String, String> entity) {
        super(entity);
    }
}
