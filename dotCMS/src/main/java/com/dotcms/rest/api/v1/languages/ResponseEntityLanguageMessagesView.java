package com.dotcms.rest.api.v1.languages;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for language messages responses.
 * Contains internationalization messages for specified language and country.
 * Note: This is deprecated - use v2 LanguagesResource instead.
 * 
 * @author Steve Bolton
 * @deprecated Use v2 LanguagesResource instead
 */
@Deprecated
public class ResponseEntityLanguageMessagesView extends ResponseEntityView<Object> {
    public ResponseEntityLanguageMessagesView(final Object entity) {
        super(entity);
    }
}