package com.dotcms.rest;

import java.util.Map;

/**
 * Response Entity Map View
 * @author jsanca
 */
public class ResponseEntityMapView  extends ResponseEntityView<Map<String, Object>> {

    public ResponseEntityMapView(final Map<String, Object> entity) {
        super(entity);
    }
    
    public ResponseEntityMapView(final Map<String, Object> entity, final Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }
}
