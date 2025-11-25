package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import java.util.Objects;

/**
 * Provides the data context for rendering an {@link HTMLPageAsset}.
 *
 * @author Freddy Rodriguez
 * @since Feb 22nd, 2019
 */
public class PageContext {

    private final User user;
    private final String pageUri;
    private final PageMode pageMode;
    private final HTMLPageAsset page;
    private final boolean graphQL;
    private final boolean parseJSON;
    private final VanityURLView vanityUrl;

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

        this(user, pageUri, pageMode, page, graphQL, false, null);
    }

    public PageContext(
            final User user,
            final String pageUri,
            final PageMode pageMode,
            final HTMLPageAsset page,
            final boolean graphQL,
            final boolean parseJSON,
            final VanityURLView vanityUrl) {

        this.user = user;
        this.pageUri = pageUri;
        this.pageMode = pageMode;
        this.page = page;
        this.graphQL = graphQL;
        this.parseJSON = parseJSON;
        this.vanityUrl = vanityUrl;
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

    /**
     * If the page URL matches a Vanity URL, this method returns the associated Vanity URL
     * object.
     *
     * @return The {@link VanityURLView} object representing the Vanity URL.
     */
    public VanityURLView getVanityUrl() {
        return vanityUrl;
    }

    //Create a toString method
    @Override
    public String toString() {
        return "PageContext{" +
                "user=" + user +
                ", pageUri='" + pageUri + '\'' +
                ", pageMode=" + pageMode +
                ", page=" + page +
                ", graphQL=" + graphQL +
                ", parseJSON=" + parseJSON +
                '}';
    }
}
