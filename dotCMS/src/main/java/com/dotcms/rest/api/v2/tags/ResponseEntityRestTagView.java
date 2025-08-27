package com.dotcms.rest.api.v2.tags;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.tag.RestTag;

/**
 * Response wrapper for single RestTag
 */
public class ResponseEntityRestTagView extends ResponseEntityView<RestTag> {
    public ResponseEntityRestTagView(final RestTag entity) {
        super(entity);
    }
}