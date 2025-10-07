package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for JWT token creation responses.
 * Contains the generated JWT token and metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityJwtTokenView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityJwtTokenView(final Map<String, Object> entity) {
        super(entity);
    }
}