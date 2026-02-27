package com.dotcms.rest.api.v1.demo;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Typed response view for the demo greeting endpoint.
 */
public class ResponseEntityDemoGreetingView extends ResponseEntityView<Map<String, String>> {
    public ResponseEntityDemoGreetingView(final Map<String, String> entity) {
        super(entity);
    }
}