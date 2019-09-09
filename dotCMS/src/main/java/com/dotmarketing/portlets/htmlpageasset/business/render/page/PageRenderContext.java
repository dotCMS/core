package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

public class PageRenderContext {
    private PageMode mode;
    private Long languageId;
    private User user;

    public PageRenderContext(PageMode mode, User user) {
        this(mode, null, user);
    }

    public PageRenderContext(final PageMode mode, final  Long languageId, final  User user) {
        this.mode = mode;
        this.languageId = languageId;
        this.user = user;
    }

    public PageMode getMode() {
        return mode;
    }

    public Long getLanguageId() {
        return languageId;
    }

    public User getUser() {
        return user;
    }
}
