package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageLivePreviewVersionBean;

/**
 * Entity View for page live preview version comparison responses.
 * Contains information about live and preview versions of a page and whether they differ.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPageLivePreviewVersionView extends ResponseEntityView<PageLivePreviewVersionBean> {
    public ResponseEntityPageLivePreviewVersionView(final PageLivePreviewVersionBean entity) {
        super(entity);
    }
}