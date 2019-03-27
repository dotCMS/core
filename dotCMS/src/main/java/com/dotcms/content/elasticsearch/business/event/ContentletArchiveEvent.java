package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Event to notified when a contentlet is being archive or unarchive
 * @author jsanca
 */
public class ContentletArchiveEvent implements Serializable {

    private final boolean    archive;
    private final Contentlet contentlet;
    private final User user;
    private final Date date;

    public ContentletArchiveEvent(final Contentlet contentlet,
                                  final User user, final boolean archive) {

        this.archive    = archive;
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

    public boolean isArchive() {
        return archive;
    }
}
