package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Event to notified when a contentlet is being checkin
 * @author jsanca
 */
public class ContentletCheckinEvent<T extends Contentlet> implements Serializable {

    private final T contentlet;
    private final boolean newVersionCreated;
    private final User user;
    private final Date date;

    public ContentletCheckinEvent(final T contentlet,
                                  final boolean newVersionCreated,
                                  final User user) {

        this.contentlet = contentlet;
        this.newVersionCreated = newVersionCreated;
        this.user = user;
        this.date = new Date();
    }

    public T getContentlet() {
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
