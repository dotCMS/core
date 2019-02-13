package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Builder for {@link PageContext}
 */
public class PageContextBuilder {
    private User user;
    private String pageUri;
    private PageMode pageMode;
    private HTMLPageAsset page;

    public PageContextBuilder setUser(final User user) {
        this.user = user;
        return this;
    }

    public PageContextBuilder setPageUri(final String pageUri) {
        this.pageUri = pageUri;
        return this;
    }

    public PageContextBuilder setPageMode(final PageMode pageMode) {
        this.pageMode = pageMode;
        return this;
    }

    public PageContextBuilder setPage(final HTMLPageAsset page) {
        this.page = page;
        return this;
    }

    public PageContext build() {
        return new PageContext(user, pageUri, pageMode, page);
    }
}
