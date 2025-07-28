package com.dotcms.rest;

import java.util.Map;

/**
 * ResponseEntityView wrapper for Map<String, Object> data.
 * This is a reusable view class for any endpoint that returns a Map<String, Object>
 * structure wrapped in ResponseEntityView. Used across various REST endpoints
 * that return map-based data structures.
 */
public class ResponseEntityMapStringObjectView extends ResponseEntityView<Map<String, Object>> {
    
    public ResponseEntityMapStringObjectView(Map<String, Object> entity) {
        super(entity);
    }
}