package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for ISO languages and countries responses.
 * Contains Map with languages and countries data.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityIsoLanguagesCountriesView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityIsoLanguagesCountriesView(final Map<String, Object> entity) {
        super(entity);
    }
}
