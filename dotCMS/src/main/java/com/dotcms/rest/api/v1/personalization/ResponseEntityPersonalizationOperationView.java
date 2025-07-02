package com.dotcms.rest.api.v1.personalization;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for personalization operation success responses.
 * Contains operation confirmation messages.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPersonalizationOperationView extends ResponseEntityView<String> {
    public ResponseEntityPersonalizationOperationView(final String entity) {
        super(entity);
    }
}