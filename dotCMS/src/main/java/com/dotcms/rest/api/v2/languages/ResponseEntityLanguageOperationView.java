package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for language operation responses.
 * Contains operation confirmation messages for language actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguageOperationView extends ResponseEntityView<String> {
    public ResponseEntityLanguageOperationView(final String entity) {
        super(entity);
    }
}
