package com.dotcms.notifications.bean;

import com.liferay.portal.model.User;

import java.io.Serializable;

/**
 * Just encapsulates a User and notification
 * @author jsanca
 */
public class UserNotificationPair implements Serializable {

    private final User user;
    private final Notification notification;


    public UserNotificationPair(User user, Notification notification) {
        this.user = user;
        this.notification = notification;
    }

    public User getUser() {
        return user;
    }

    public Notification getNotification() {
        return notification;
    }
} // E:O:F:UserNotificationPair.
