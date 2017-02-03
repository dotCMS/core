package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotmarketing.exception.DotDataException;

public interface NotificationAPI {

	void info(String message, String userId);

	void error(String message, String userId);

	void generateNotification(String message, NotificationLevel level, String userId) throws DotDataException;

	Notification findNotification(String notificationId) throws DotDataException;

	void deleteNotification(String notificationId) throws DotDataException;

	void deleteNotifications(String userId) throws DotDataException;

	List<Notification> getNotifications(long offset, long limit) throws DotDataException;

	Long getNotificationsCount(String userId) throws DotDataException;

	Long getNotificationsCount() throws DotDataException;

	List<Notification> getAllNotifications(String userId) throws DotDataException;

	List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException;

	Long getNewNotificationsCount(String userId)  throws DotDataException;

	void markNotificationsAsRead(String userId) throws DotDataException;
}
