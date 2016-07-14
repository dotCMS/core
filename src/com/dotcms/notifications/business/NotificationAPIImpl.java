package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Concrete implementation of the {@link NotificationAPI} class.
 * 
 * @author Daniel Silva
 * @version 3.0
 * @since Feb 3, 2014
 *
 */
public class NotificationAPIImpl implements NotificationAPI {

	private final NotificationFactory notificationFactory;
	private final SystemEventsAPI systemEventsAPI;

	/**
	 * Retrieve the factory class that interacts with the database.
	 */
	public NotificationAPIImpl() {
		this.notificationFactory = FactoryLocator.getNotificationFactory();
		this.systemEventsAPI = APILocator.getSystemEventsAPI();
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

	@Override
	public void generateNotification(String message, NotificationLevel level, String userId) throws DotDataException {

		final Notification notification = new Notification(message, level, userId);
		// Adding notification to System Events table
		this.systemEventsAPI.pushNotification(notification);
		this.notificationFactory.saveNotification(notification);
	}

	@Override
	public Notification findNotification(String notificationId) throws DotDataException {
		return notificationFactory.findNotification(notificationId); 
	}

	@Override
	public void deleteNotification(String notificationId) throws DotDataException {
		notificationFactory.deleteNotification(notificationId);
	}

	@Override
	public void deleteNotifications(String userId) throws DotDataException {
		notificationFactory.deleteNotifications(userId);
	}

	@Override
	public List<Notification> getNotifications(long offset, long limit) throws DotDataException {
		return notificationFactory.getNotifications(offset, limit);
	}

	@Override
	public Long getNotificationsCount() throws DotDataException {
		return notificationFactory.getNotificationsCount(null);
	}

	@Override
	public Long getNotificationsCount(String userId) throws DotDataException {
		return notificationFactory.getNotificationsCount(userId);
	}

	@Override
	public List<Notification> getAllNotifications(String userId) throws DotDataException {
		return notificationFactory.getAllNotifications(userId);
	}

	@Override
	public List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException {
		return notificationFactory.getNotifications(userId, offset, limit);
	}

	@Override
	public Long getNewNotificationsCount(String userId)  throws DotDataException {
		Long count = CacheLocator.getNewNotificationCache().get(userId);

		if(!UtilMethods.isSet(count)) {
			count = notificationFactory.getNewNotificationsCount(userId);
			CacheLocator.getNewNotificationCache().add(userId, count);
		}

		return count;
	}

	@Override
	public void markNotificationsAsRead(String userId) throws DotDataException {
		notificationFactory.markNotificationsAsRead(userId);
		CacheLocator.getNewNotificationCache().remove(userId);
	}

}
