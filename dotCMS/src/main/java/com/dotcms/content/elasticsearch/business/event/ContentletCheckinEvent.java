package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

public class ContentletCheckinEvent implements Serializable {

    private final Contentlet contentlet;
    private final boolean newVersionCreated;
    private final User user;
    private final Date date;

    public ContentletCheckinEvent(final Contentlet contentlet,
                                  final boolean newVersionCreated,
                                  final User user) {

        this.contentlet = contentlet;
        this.newVersionCreated = newVersionCreated;
        this.user = user;
        this.date = new Date();
    }

    public Contentlet getContentlet() {
        return contentlet;
    }

    public boolean isNewVersionCreated() {
        return newVersionCreated;
    }

    public User getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }
}
