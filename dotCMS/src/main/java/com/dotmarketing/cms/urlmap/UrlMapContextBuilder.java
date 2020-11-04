package com.dotmarketing.cms.urlmap;

import com.dotmarketing.beans.Host;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

/**
 * Builder for {@link UrlMapContext}
 */
public class UrlMapContextBuilder {
    private PageMode mode;
    private long languageId;
    private String uri;
    private Host host;
    private User user;
    private boolean graphQL;

    private UrlMapContextBuilder(){}

    public static UrlMapContextBuilder builder(){
        return new UrlMapContextBuilder();
    }

    public UrlMapContextBuilder setMode(final PageMode mode) {
        this.mode = mode;
        return this;
    }

    public UrlMapContextBuilder setLanguageId(final long languageId) {
        this.languageId = languageId;
        return this;
    }

    public UrlMapContextBuilder setUri(final String uri) {
        this.uri = uri;
        return this;
    }

    public UrlMapContextBuilder setHost(final Host host) {
        this.host = host;
        return this;
    }

    public UrlMapContextBuilder setUser(final User user) {
        this.user = user;
        return this;
    }

    public UrlMapContextBuilder setGraphQL(final boolean graphQL) {
        this.graphQL = graphQL;
        return this;
    }

    public UrlMapContext build() {
        return new UrlMapContext(mode, languageId, uri, host, user, graphQL);
    }
}
