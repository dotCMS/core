package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Event to notified when a content is deleted
 * @author jsanca
 */
public class ContentletDeletedEvent<T extends Contentlet> implements Serializable {

    private final T contentlet;
    private final User       user;
    private final Date       date;

    public ContentletDeletedEvent(final T contentlet, final User user) {
        this.contentlet = contentlet;
        this.user = user;
        this.date = new Date();
    }

    private ContentletDeletedEvent(final T contentlet, final User user,
                                  final Date date) {
        this.contentlet = contentlet;
        this.user = user;
        this.date = date;
    }


    public static <C extends Contentlet> ContentletDeletedEvent<C> wrapContentlet (final C contentlet,
                                                                                   final ContentletDeletedEvent event) {

        return new ContentletDeletedEvent<>(contentlet, event.user, event.date);
    }

    public T getContentlet() {
        return this.contentlet;
    }

    public User getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }
}
