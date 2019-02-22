package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

/**
 * Context for render a {@link HTMLPageAsset}
 */
public class PageContext {

    private final User user;
    private final String pageUri;
    private final PageMode pageMode;
    private final HTMLPageAsset page;

    public PageContext(
            final User user,
            final String pageUri,
            final PageMode pageMode,
            final HTMLPageAsset page) {

        this.user = user;
        this.pageUri = pageUri;
        this.pageMode = pageMode;
        this.page = page;
    }


    public HTMLPageAsset getPage() {
        return page;
    }

    public User getUser() {
        return user;
    }

    public String getPageUri() {
        return pageUri;
    }

    public PageMode getPageMode() {
        return pageMode;
    }
}
