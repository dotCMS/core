package com.dotcms.rest;

import java.util.Map;

/**
 * Response Entity Map View
 * @author jsanca
 */
public class ResponseEntityMapMapView extends ResponseEntityView<Map<String, Map<String,Object>>> {

    public ResponseEntityMapMapView(Map<String, Map<String, Object>> entity) {
        super(entity);
    }
}
