package com.dotcms.rest.api.v1.fileasset;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for file asset resource link responses.
 * Contains resource link information with href, text, and MIME type.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityResourceLinkView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityResourceLinkView(final Map<String, Object> entity) {
        super(entity);
    }
}
