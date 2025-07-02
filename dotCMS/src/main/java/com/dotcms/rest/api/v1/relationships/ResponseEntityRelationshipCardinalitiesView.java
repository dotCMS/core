package com.dotcms.rest.api.v1.relationships;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for relationship cardinalities collection responses.
 * Contains available relationship cardinality types with their metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityRelationshipCardinalitiesView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityRelationshipCardinalitiesView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}