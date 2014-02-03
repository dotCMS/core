package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.notifications.bean.Notification;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

public class NotificationAPIImpl implements NotificationAPI {

	private NotificationFactory notificationFactory;

	public NotificationAPIImpl() {
		notificationFactory = FactoryLocator.getNotificationFactory();
	}

	public void saveNotification(Notification notification) throws DotDataException {
		notificationFactory.saveNotification(notification);
	}

	public List<Notification> getAllNotificationsForUser(String userId) throws DotDataException {
		return notificationFactory.getAllNotificationsForUser(userId);
	}

	public List<Notification> getNotificationsForUser(String userId, long offset, long limit) throws DotDataException {
		return notificationFactory.getNotificationsForUser(userId, offset, limit);
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
