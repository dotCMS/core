package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Response entity view for Map<String, RestConditionGroup> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class MapStringRestConditionGroupView extends HashMap<String, RestConditionGroup> {
    
    public MapStringRestConditionGroupView() {
        super();
    }
    
    public MapStringRestConditionGroupView(Map<String, RestConditionGroup> map) {
        super(map);
    }
}