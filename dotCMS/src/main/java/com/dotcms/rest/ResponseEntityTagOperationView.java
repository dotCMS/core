package com.dotcms.rest;

import com.dotcms.rest.tag.RestTag;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object for tag operations
 * that return both error information and tag data
 * Used for Tag resource endpoints that perform operations and may have errors
 * @author Steve Bolton
 */
public class ResponseEntityTagOperationView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityTagOperationView(final Map<String, Object> additionalData, final List<ErrorEntity> errors) {
        super(additionalData, errors, Collections.emptyList());
    }
}