package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PageRenderedContextBuilder {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private String pageUri;
    private PageMode pageMode;
    private HTMLPageAsset page;

    public PageRenderedContextBuilder setRequest(HttpServletRequest request) {
        this.request = request;
        return this;
    }

    public PageRenderedContextBuilder setResponse(HttpServletResponse response) {
        this.response = response;
        return this;
    }

    public PageRenderedContextBuilder setUser(User user) {
        this.user = user;
        return this;
    }

    public PageRenderedContextBuilder setPageUri(String pageUri) {
        this.pageUri = pageUri;
        return this;
    }

    public PageRenderedContextBuilder setPageMode(PageMode pageMode) {
        this.pageMode = pageMode;
        return this;
    }

    public PageRenderedContextBuilder setPage(HTMLPageAsset page) {
        this.page = page;
        return this;
    }

    public PageRenderedContext build() {
        return new PageRenderedContext(request, response, user, pageUri, pageMode, page);
    }
}
