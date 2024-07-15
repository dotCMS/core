package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;

public class ResponseEntityPageView extends ResponseEntityView<PageView> {
    public ResponseEntityPageView(PageView entity) {
        super(entity);
    }
}