package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * Generic response entity view for List&lt;Map&lt;String, Object&gt;&gt; responses.
 * Used for endpoints that return collections of map data structures.
 * 
 * @author Claude Code
 */
public class ResponseEntityListMapView extends ResponseEntityView<List<Map<String, Object>>> {
    
    public ResponseEntityListMapView(List<Map<String, Object>> entity) {
        super(entity);
    }
}