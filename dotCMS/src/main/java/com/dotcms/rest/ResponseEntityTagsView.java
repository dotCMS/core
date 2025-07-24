package com.dotcms.rest;

import com.dotcms.rest.tag.RestTag;

import java.util.Map;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected Map<String, RestTag> and related
 * Used for Tag resource endpoints that return tag collections
 * @author Steve Bolton
 */
public class ResponseEntityTagsView extends ResponseEntityView<Map<String, RestTag>> {
    public ResponseEntityTagsView(final Map<String, RestTag> entity) {
        super(entity);
    }
}