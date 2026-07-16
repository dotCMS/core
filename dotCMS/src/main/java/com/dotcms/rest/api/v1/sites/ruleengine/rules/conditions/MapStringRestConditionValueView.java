package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Response entity view for Map<String, RestConditionValue> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class MapStringRestConditionValueView extends HashMap<String, RestConditionValue> {
    
    public MapStringRestConditionValueView() {
        super();
    }
    
    public MapStringRestConditionValueView(Map<String, RestConditionValue> map) {
        super(map);
    }
}