package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;


/**
 * Builder for {@link PageContext}
 */
public class PageContextBuilder {
    private User user;
    private String pageUri;
    private PageMode pageMode;
    private HTMLPageAsset page;
    private boolean graphQL;

    private PageContextBuilder() {}

    public static PageContextBuilder builder() {
        return new PageContextBuilder();
    }

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

    public PageContextBuilder setGraphQL(final boolean graphQL) {
        this.graphQL = graphQL;
        return this;
    }

    public PageContext build() {
        return new PageContext(user, pageUri, pageMode, page, graphQL);
    }
}
