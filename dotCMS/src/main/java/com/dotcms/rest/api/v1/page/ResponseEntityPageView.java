package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;

/**
 * Entity View for page rendering responses.
 * Contains the complete rendered page data including metadata, content, and layout information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPageView extends ResponseEntityView<PageView> {
    public ResponseEntityPageView(final PageView entity) {
        super(entity);
    }
}