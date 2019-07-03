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
        this.user       = user;
        this.date       = new Date();
    }

    private ContentletArchiveEvent(final T contentlet,
                                   final User user,
                                   final boolean archive,
                                   final Date date) {

        this.archive    = archive;
        this.contentlet = contentlet;
        this.user       = user;
        this.date       = date;
    }

    public static <C extends Contentlet> ContentletArchiveEvent<C> wrapContentlet (final C contentlet,
                                                                                   final ContentletArchiveEvent event) {

        return new ContentletArchiveEvent<>(contentlet, event.user, event.archive, event.date);
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
