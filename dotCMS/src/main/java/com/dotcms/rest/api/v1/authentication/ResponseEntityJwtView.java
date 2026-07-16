package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for JWT-only responses.
 * Contains a map with JWT information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityJwtView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityJwtView(final Map<String, Object> entity) {
        super(entity);
    }
}