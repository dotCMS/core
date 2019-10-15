package com.dotmarketing.cms.urlmap;

import com.dotmarketing.beans.Host;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Objects;

/**
 * Context to resolve a Page with the {@link com.dotcms.contenttype.model.type.ContentType}'s
 * URL mapper
 *
 * @see URLMapAPI#processURLMap(UrlMapContext)
 */
public class UrlMapContext {
    private final PageMode mode;
    private final long languageId;
    private final String uri;
    private final Host host;
    private final User user;

    public UrlMapContext(
            final PageMode mode,
            final long languageId,
            final String uri,
            final Host host,
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UrlMapContext that = (UrlMapContext) o;
        return languageId == that.languageId &&
                mode == that.mode &&
                Objects.equals(uri, that.uri) &&
                Objects.equals(host, that.host) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, languageId, uri, host, user);
    }
}
