package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Event to notified when a contentlet is being archive or unarchive
 * @author jsanca
 */
public class ContentletArchiveEvent<T extends Contentlet> implements Serializable {

    private final boolean    archive;
    private final T contentlet;
    private final User user;
    private final Date date;

    public ContentletArchiveEvent(final T contentlet,
                                  final User user, final boolean archive) {

        this.archive    = archive;
        this.contentlet = contentlet;
        this.user = user;
        this.date = new Date();
    }

    public T getContentlet() {
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
