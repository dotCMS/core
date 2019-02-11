package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PageRenderedContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final User user;
    private final String pageUri;
    private final PageMode pageMode;
    private final HTMLPageAsset page;

    public PageRenderedContext(final HttpServletRequest request,
            final HttpServletResponse response, final User user, final String pageUri,
            final PageMode pageMode, final HTMLPageAsset page) {
        this.request = request;
        this.response = response;
        this.user = user;
        this.pageUri = pageUri;
        this.pageMode = pageMode;
        this.page = page;
    }


    public HTMLPageAsset getPage() {
        return page;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
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
