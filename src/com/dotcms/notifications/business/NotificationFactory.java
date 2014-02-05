package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.notifications.bean.Notification;
import com.dotmarketing.exception.DotDataException;

public abstract class NotificationFactory {

	public abstract void saveNotification(Notification notification) throws DotDataException;

	public abstract Notification findNotification(String notificationId) throws DotDataException;

	public abstract void deleteNotification(String notificationId) throws DotDataException;

	public abstract void deleteNotifications(String userId) throws DotDataException;

	public abstract List<Notification> getNotifications(long offset, long limit) throws DotDataException;

	public abstract List<Notification> getAllNotifications(String userId) throws DotDataException;

	public abstract Long getNotificationsCount(String userId) throws DotDataException;

	public abstract List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException;

	public abstract Long getNewNotificationsCount(String userId)  throws DotDataException;

	public abstract void markNotificationsAsRead(String userId) throws DotDataException;
}
