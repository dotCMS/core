package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for single API token responses.
 * Contains a map with API token information (revoked, deleted, etc.).
 * 
 * @author Steve Bolton
 */
public class ResponseEntityApiTokenView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityApiTokenView(final Map<String, Object> entity) {
        super(entity);
    }
}