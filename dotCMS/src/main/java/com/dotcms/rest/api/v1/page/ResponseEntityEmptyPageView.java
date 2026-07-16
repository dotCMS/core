package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.EmptyPageView;

/**
 * Entity View for empty page responses with vanity URL information.
 * Contains vanity URL details when a page request results in a redirect.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityEmptyPageView extends ResponseEntityView<EmptyPageView> {
    public ResponseEntityEmptyPageView(final EmptyPageView entity) {
        super(entity);
    }
}