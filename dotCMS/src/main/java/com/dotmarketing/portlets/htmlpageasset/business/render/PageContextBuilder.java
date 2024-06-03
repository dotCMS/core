package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

/**
 * This class provides some background information on the context in which an HTML Page is being accessed, usually via
 * REST Endpoints. For example:
 * <ul>
 *     <li>The page's URL.</li>
 *     <li>The {@link User} accessing the page -- anonymous access or a User editing it in the back-end.</li>
 *     <li>The {@link PageMode} in which the page is being accessed -- Edit Mode, Live, Preview, etc.</li>
 *     <li>The actual {@link HTMLPageAsset} object representing the page.</li>
 *     <li>Flags for enabling the JSON parsing or rendering for GraphQL.</li>
 * </ul>
 *
 * @author Freddy Rodriguez
 * @since Feb 22nd, 2019
 */
public class PageContextBuilder {

    private User user;
    private String pageUri;
    private PageMode pageMode;
    private HTMLPageAsset page;
    private boolean graphQL;
    private boolean parseJSON;
    private VanityURLView vanityUrl;

    private PageContextBuilder() {}

    /**
     * Creates an instance of the Page Context Builder.
     *
     * @return The {@link PageContextBuilder} object.
     */
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

    public PageContextBuilder setParseJSON(final boolean parseJSON) {
        this.parseJSON = parseJSON;
        return this;
    }

    /**
     * If the page URL matches a Vanity URL, this method sets the associated Vanity URL object.
     *
     * @param vanityUrl The {@link VanityURLView} object representing the Vanity URL.
     *
     * @return The {@link PageContextBuilder} object.
     */
    public PageContextBuilder setVanityUrl(final VanityURLView vanityUrl) {
        this.vanityUrl = vanityUrl;
        return this;
    }

    public PageContext build() {
        return new PageContext(user, pageUri, pageMode, page, graphQL, parseJSON, vanityUrl);
    }

}
