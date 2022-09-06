package com.dotcms.rest;

import java.util.Map;

public class ResponseEntityContentletView extends ResponseEntityView<Map<String, Object>> {
     
    public ResponseEntityContentletView(final Map<String, Object> entity) {
        super(entity);
    }
}
