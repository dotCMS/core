package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for languages available for a page responses.
 * Contains a list of existing languages that a page has been translated into.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguagesForPageView extends ResponseEntityView<List<ExistingLanguagesForPageView>> {
    public ResponseEntityLanguagesForPageView(final List<ExistingLanguagesForPageView> entity) {
        super(entity);
    }
}