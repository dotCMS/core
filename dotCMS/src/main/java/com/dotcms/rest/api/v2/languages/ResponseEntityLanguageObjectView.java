package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.languagesmanager.model.Language;

/**
 * Entity View for Language object responses.
 * Contains raw Language objects for compatibility with legacy endpoints.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguageObjectView extends ResponseEntityView<Language> {
    public ResponseEntityLanguageObjectView(final Language entity) {
        super(entity);
    }
}