package com.dotcms.content.elasticsearch.business.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Event to notified when a contentlet is being publish or unpublish
 * @author jsanca
 */
public class ContentletPublishEvent<T extends Contentlet> implements Serializable {

    // true if it is a publish, false if it is unpublish
    private final boolean publish;
    private final T contentlet;
    private final User user;
    private final Date date;

    public ContentletPublishEvent(final T contentlet,
                                  final User user, final boolean publish) {

        this.contentlet = contentlet;
        this.publish    = publish;
        this.user       = user;
        this.date       = new Date();
    }

    private ContentletPublishEvent(final T contentlet,
                                  final User user, final boolean publish, final Date date) {

        this.contentlet = contentlet;
        this.publish    = publish;
        this.user       = user;
        this.date       = date;
    }

    public static <C extends Contentlet> ContentletPublishEvent<C> wrapContentlet (final C contentlet,
                                                                                   final ContentletPublishEvent event) {

        return new ContentletPublishEvent<>(contentlet, event.user, event.publish, event.date);
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

    public boolean isPublish() {
        return publish;
    }
}
