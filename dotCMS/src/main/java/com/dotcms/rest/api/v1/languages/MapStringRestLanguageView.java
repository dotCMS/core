package com.dotcms.rest.api.v1.languages;

import java.util.HashMap;
import java.util.Map;

/**
 * Response entity view for Map<String, RestLanguage> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class MapStringRestLanguageView extends HashMap<String, RestLanguage> {
    
    public MapStringRestLanguageView() {
        super();
    }
    
    public MapStringRestLanguageView(Map<String, RestLanguage> map) {
        super(map);
    }
}