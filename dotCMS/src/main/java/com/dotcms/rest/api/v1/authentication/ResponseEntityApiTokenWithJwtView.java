package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for API token responses that include JWT.
 * Contains a map with both token and JWT information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityApiTokenWithJwtView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityApiTokenWithJwtView(final Map<String, Object> entity) {
        super(entity);
    }
}