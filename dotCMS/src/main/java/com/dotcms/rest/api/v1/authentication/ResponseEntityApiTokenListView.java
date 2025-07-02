package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for API token list responses.
 * Contains a map with "tokens" key holding a list of API tokens.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityApiTokenListView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityApiTokenListView(final Map<String, Object> entity) {
        super(entity);
    }
}