package com.dotcms.rest.api.v1.personas;

import java.util.HashMap;
import java.util.Map;

/**
 * View class for Map<String, RestPersona> responses in Swagger documentation.
 * This class extends HashMap to provide proper OpenAPI schema generation
 * for endpoints that return maps of personas keyed by string identifiers.
 */
public class MapStringRestPersonaView extends HashMap<String, RestPersona> {
    
    public MapStringRestPersonaView() {
        super();
    }
    
    public MapStringRestPersonaView(Map<String, RestPersona> map) {
        super(map);
    }
}