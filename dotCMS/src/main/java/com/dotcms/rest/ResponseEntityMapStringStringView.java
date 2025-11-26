package com.dotcms.rest;

import java.util.Map;

/**
 * Generic entity view for Map&lt;String, String&gt; responses.
 * Use this class instead of creating specific view classes 
 * for responses that contain maps with string keys and string values.
 * 
 * @author dotCMS Team
 */
public class ResponseEntityMapStringStringView extends ResponseEntityView<Map<String, String>> {
    public ResponseEntityMapStringStringView(final Map<String, String> entity) {
        super(entity);
    }
}