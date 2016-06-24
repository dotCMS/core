package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class NotificationAPIImpl implements NotificationAPI {

	private NotificationFactory notificationFactory;

	public NotificationAPIImpl() {
		notificationFactory = FactoryLocator.getNotificationFactory();
	}

	@Override
	public void info(String message, String userId) {
		try {
			generateNotification(message, NotificationLevel.INFO, userId);
		} catch (DotDataException e) {
			Logger.error(this, "Error generating INFO Notification", e);
		}
	}

	@Override
	public void error(String message, String userId) {
		try {
			generateNotification(message, NotificationLevel.ERROR, userId);
		} catch (DotDataException e) {
			Logger.error(this, "Error generating ERROR Notification", e);
		}
	}

	public void generateNotification(String message, NotificationLevel level, String userId) throws DotDataException {
		Notification n = new Notification(message, level, userId);
		notificationFactory.saveNotification(n);
	}

	public Notification findNotification(String notificationId) throws DotDataException {
		return notificationFactory.findNotification(notificationId); 
	}

	public void deleteNotification(String notificationId) throws DotDataException {
		notificationFactory.deleteNotification(notificationId);
	}

	public void deleteNotifications(String userId) throws DotDataException {
		notificationFactory.deleteNotifications(userId);
	}

	public List<Notification> getNotifications(long offset, long limit) throws DotDataException {
		return notificationFactory.getNotifications(offset, limit);
	}

	public Long getNotificationsCount() throws DotDataException {
		return notificationFactory.getNotificationsCount(null);
	}

	public Long getNotificationsCount(String userId) throws DotDataException {
		return notificationFactory.getNotificationsCount(userId);
	}

	public List<Notification> getAllNotifications(String userId) throws DotDataException {
		return notificationFactory.getAllNotifications(userId);
	}

	public List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException {
		return notificationFactory.getNotifications(userId, offset, limit);
	}

	public Long getNewNotificationsCount(String userId)  throws DotDataException {
		Long count = CacheLocator.getNewNotificationCache().get(userId);

		if(!UtilMethods.isSet(count)) {
			count = notificationFactory.getNewNotificationsCount(userId);
			CacheLocator.getNewNotificationCache().add(userId, count);
		}

		return count;
	}

	public void markNotificationsAsRead(String userId) throws DotDataException {
		notificationFactory.markNotificationsAsRead(userId);
		CacheLocator.getNewNotificationCache().remove(userId);
	}
}
