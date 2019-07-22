package com.dotcms.contenttype.transform.contenttype;

import com.liferay.portal.model.User;

public class ContentTypeInternationalization {
    private final long languageId;
    private final boolean live;
    private final User user;

    public ContentTypeInternationalization(
            final long languageId,
            final boolean live,
            final User user) {

        this.languageId = languageId;
        this.live = live;
        this.user = user;
    }

    public long getLanguageId() {
        return languageId;
    }

    public boolean isLive() {
        return live;
    }

    public User getUser() {
        return user;
    }
}
