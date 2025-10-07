package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for language variable page responses.
 * Contains paginated language variable data.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguageVariablePageView extends ResponseEntityView<LanguageVariablePageView> {
    public ResponseEntityLanguageVariablePageView(final LanguageVariablePageView entity) {
        super(entity);
    }
}