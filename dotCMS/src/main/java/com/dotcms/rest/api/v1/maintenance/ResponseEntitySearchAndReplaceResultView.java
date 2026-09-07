package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the search and replace endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntitySearchAndReplaceResultView extends ResponseEntityView<SearchAndReplaceResultView> {

    public ResponseEntitySearchAndReplaceResultView(final SearchAndReplaceResultView entity) {
        super(entity);
    }
}
