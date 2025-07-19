package com.dotcms.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic response entity view for List&lt;Map&lt;String, Object&gt;&gt; responses.
 * Used for endpoints that return collections of map data structures.
 * Whereas Map<String,Object> and this can be represented by type = "object" as just json
 * This class explicitly defines an array of json objects.
 * 
 * @author Claude Code
 */
public class ListMapView extends ArrayList<Map<String, Object>> {

    public ListMapView(List<Map<String, Object>> detailedAssets) {
        addAll(detailedAssets);
    }
}