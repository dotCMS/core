package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.notifications.bean.Notification;
import com.dotmarketing.exception.DotDataException;

public interface NotificationAPI {

	void saveNotification(Notification notification) throws DotDataException;

	List<Notification> getAllNotificationsForUser(String userId) throws DotDataException;

	List<Notification> getNotificationsForUser(String userId, long offset, long limit) throws DotDataException;

	Long getNewNotificationsCount(String userId)  throws DotDataException;

	void markNotificationsAsRead(String userId) throws DotDataException;
}
