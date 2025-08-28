package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for language messages collection responses.
 * Contains Map of language key-value message pairs.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguageMessagesView extends ResponseEntityView<Map<Object, Object>> {
    public ResponseEntityLanguageMessagesView(final Map<Object, Object> entity) {
        super(entity);
    }
}
