package com.dotcms.rest;

import java.util.Set;

/**
 * Generic entity view for Set&lt;String&gt; responses.
 * Use this class instead of creating specific view classes 
 * for responses that contain sets of strings.
 * 
 * @author dotCMS Team
 */
public class ResponseEntitySetStringView extends ResponseEntityView<Set<String>> {
    public ResponseEntitySetStringView(final Set<String> entity) {
        super(entity);
    }
}