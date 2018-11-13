package com.dotmarketing.portlets.folders.business;

import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.util.Date;

public class FolderEvent {

    private final String identifier;
    private final User   user;
    private final Object child;
    private final String childName;
    private final Folder parent;
    private final Date   date;

    public FolderEvent(final String id, final User user, final Object child,
                       final String childName, final Folder parent, final Date date) {

        this.identifier = id;
        this.user  = user;
        this.child = child;
        this.childName = childName;
        this.parent = parent;
        this.date   = date;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Object getChild() {
        return child;
    }

    public String getChildName() {
        return childName;
    }

    public Folder getParent() {
        return parent;
    }

    public Date getDate() {
        return date;
    }

    public User getUser() {
        return user;
    }
}
