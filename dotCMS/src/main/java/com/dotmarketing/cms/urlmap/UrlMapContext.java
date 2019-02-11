package com.dotmarketing.cms.urlmap;

import com.dotmarketing.beans.Host;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

public class UrlMapContext {
    private final PageMode mode;
    private final long languageId;
    private final String uri;
    private final Host host;
    private final User user;

    UrlMapContext(final PageMode mode, final long languageId, final String uri, final Host host,
            final User user) {

        this.mode = mode;
        this.languageId = languageId;
        this.uri = uri;
        this.host = host;
        this.user = user;
    }

    public PageMode getMode() {
        return mode;
    }

    public long getLanguageId() {
        return languageId;
    }

    public String getUri() {
        return uri.startsWith(StringPool.FORWARD_SLASH) ? uri : StringPool.FORWARD_SLASH + uri;
    }

    public Host getHost() {
        return host;
    }

    public User getUser() {
        return user;
    }
}
