package com.dotcms.notifications.view;

import com.dotcms.notifications.bean.*;
import com.dotcms.util.I18NMessage;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Encapsulate a view version of the {@link NotificationView}
 *
 * This is what we return to the clients with pretty dates and i18n messages.
 *
 * @author jsanca
 */
public class NotificationView implements Serializable, Cloneable {

    private final String id;
    private final NotificationDataView notificationData;
    private final NotificationType type;
    private final NotificationLevel level;
    private final String userId;
    private final Date timeSent;
    private final Boolean wasRead;
    private final String prettyDate;



    /**
     * Creates a Notification object.
     *
     * @param id
     *            - The ID of this notification. If a new object is being
     *            created, leave this parameter as {@code null} so the system
     *            generates an appropriate ID.
     * @param type
     *            - The type or notification according to the
     *            {@link NotificationType} class.
     * @param level
     *            - The urgency level or category according to the
     *            {@link NotificationLevel} class.
     * @param userId
     *            - The ID of the user that this notification is going to be
     *            sent to.
     * @param timeSent
     *            - The creation date of this notification.
     * @param wasRead
     *            - If set to {@code true}, this notification will be marked as
     *            "read" by the user. Otherwise, set to {@code false}.
     * @param notificationData
     *            - The additional information that make up this notification.
     */
    public NotificationView(final String id, final NotificationType type,
                        final NotificationLevel level, final String userId,
                        final Date timeSent,
                        final Boolean wasRead,
                        final NotificationDataView notificationData,
                        final String prettyDate) {
        this.id = id;
        this.userId = userId;
        this.notificationData = notificationData;
        this.type = type;
        this.level = level;
        this.timeSent = timeSent;
        this.wasRead = wasRead;
        this.prettyDate = prettyDate;
    }

    /**
     * Returns the ID of this notification.
     *
     * @return The notification ID.
     */
    public String getId() {
        return id;
    }


    /**
     * Returns the title of this notification.
     *
     * @return The notification title.
     */
    public String getTitle() {
        return this.notificationData.getTitle();
    }


    /**
     * Returns the message of this notification.
     *
     * @return The message title.
     */
    public String getMessage() {
        return this.notificationData.getMessage();
    }


    /**
     * Returns the type of this notification.
     *
     * @return The notification type.
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * Returns the level of this notification.
     *
     * @return The notification level.
     */
    public NotificationLevel getLevel() {
        return level;
    }


    /**
     * Returns the user ID of this notification.
     *
     * @return The notification user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the date that this notification was sent.
     *
     * @return The notification sent date.
     */
    public Date getTimeSent() {
        return timeSent;
    }


    /**
     * Returns the status of this notification, indicating if it has been read
     * or not.
     *
     * @return The notification status.
     */
    public Boolean getWasRead() {
        return wasRead;
    }


    /**
     * Returns the list of {@link NotificationAction} objects for this
     * notification.
     *
     * @return The notification action list.
     */
    public List<NotificationActionView> getActions() {
        return this.notificationData.getActions();
    }

    /**
     * Returns the {@link NotificationData} object of this notification.
     *
     * @return The NotificationData object.
     */
    public NotificationDataView getNotificationData() {
        return this.notificationData;
    }

    public String getPrettyDate() {
        return prettyDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        NotificationView that = (NotificationView) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        String title   = this.notificationData.getTitle();
        String message = this.notificationData.getMessage();
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (level != null ? level.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (timeSent != null ? timeSent.hashCode() : 0);
        result = 31 * result + (wasRead != null ? wasRead.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Notification [id=" + id + ", title=" + this.getTitle() + ", message=" + this.getMessage() + ", type=" + type
                + ", level=" + level + ", userId=" + userId + ", timeSent=" + timeSent + ", wasRead=" + wasRead
                + ", actions=" + this.getActions() + "]";
    }


} // E:O:F:NotificationView.
