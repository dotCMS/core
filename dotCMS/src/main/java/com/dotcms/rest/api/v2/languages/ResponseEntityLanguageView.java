package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for single language responses.
 * Contains individual language information with view wrapper.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguageView extends ResponseEntityView<LanguageView> {
    public ResponseEntityLanguageView(final LanguageView entity) {
        super(entity);
    }
}