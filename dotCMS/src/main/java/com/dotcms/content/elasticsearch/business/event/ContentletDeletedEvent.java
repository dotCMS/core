package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Event to notified when a content is deleted
 * @author jsanca
 */
public class ContentletDeletedEvent implements Serializable {

    private final Contentlet contentlet;
    private final User       user;
    private final Date       date;

    public ContentletDeletedEvent(final Contentlet contentlet, final User user) {
        this.contentlet = contentlet;
        this.user = user;
        this.date = new Date();
    }

    public Contentlet getContentlet() {
        return this.contentlet;
    }

    public User getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }
}
