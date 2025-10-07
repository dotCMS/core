package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for language list responses.
 * Contains collections of languages with view wrapper.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguageListView extends ResponseEntityView<List<LanguageView>> {
    public ResponseEntityLanguageListView(final List<LanguageView> entity) {
        super(entity);
    }
}