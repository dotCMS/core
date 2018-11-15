package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Event to notified when a contentlet is being publish or unpublish
 * @author jsanca
 */
public class ContentletPublishEvent implements Serializable {

    private final Contentlet contentlet;
    private final User user;
    private final Date date;

    public ContentletPublishEvent(final Contentlet contentlet,
                                  final User user) {

        this.contentlet = contentlet;
        this.user = user;
        this.date = new Date();
    }

    public Contentlet getContentlet() {
        return contentlet;
    }

    public User getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }
}
