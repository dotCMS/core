package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.notifications.bean.Notification;
import com.dotmarketing.exception.DotDataException;

public abstract class NotificationFactory {

	public abstract void saveNotification(Notification notification) throws DotDataException;

	public abstract List<Notification> getAllNotificationsForUser(String userId) throws DotDataException;

	public abstract List<Notification> getNotificationsForUser(String userId, long offset, long limit) throws DotDataException;

	public abstract Long getNewNotificationsCount(String userId)  throws DotDataException;

	public abstract void markNotificationsAsRead(String userId) throws DotDataException;
}
