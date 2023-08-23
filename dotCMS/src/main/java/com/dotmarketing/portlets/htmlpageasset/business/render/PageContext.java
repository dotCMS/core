package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import java.util.Objects;

/**
 * Context for render a {@link HTMLPageAsset}
 */
public class PageContext {

    private final User user;
    private final String pageUri;
    private final PageMode pageMode;
    private final HTMLPageAsset page;
    private final boolean graphQL;
    private final boolean parseJSON;

    public PageContext(
            final User user,
            final String pageUri,
            final PageMode pageMode,
            final HTMLPageAsset page) {

        this(user, pageUri, pageMode, page, false);
    }

    public PageContext(
            final User user,
            final String pageUri,
            final PageMode pageMode,
            final HTMLPageAsset page,
            final boolean graphQL) {

        this(user, pageUri, pageMode, page, graphQL, false);
    }

    public PageContext(
            final User user,
            final String pageUri,
            final PageMode pageMode,
            final HTMLPageAsset page,
            final boolean graphQL,
            final boolean parseJSON) {

        this.user = user;
        this.pageUri = pageUri;
        this.pageMode = pageMode;
        this.page = page;
        this.graphQL = graphQL;
        this.parseJSON = parseJSON;
    }


    @Override
    public boolean equals(final Object another) {
        if (this == another) {
            return true;
        }
        if (another == null || getClass() != another.getClass()) {
            return false;
        }
        PageContext that = (PageContext) another;
        return Objects.equals(user, that.user) && Objects.equals(pageUri,
                that.pageUri) && pageMode == that.pageMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, pageUri, pageMode);
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

    public boolean isGraphQL() {
        return graphQL;
    }

    public boolean isParseJSON() {
        return parseJSON;
    }
}
