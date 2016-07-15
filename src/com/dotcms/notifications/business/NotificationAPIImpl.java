package com.dotcms.notifications.business;

import java.util.Date;
import java.util.List;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationData;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.dto.NotificationDTO;
import com.dotcms.util.ConversionUtils;
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
 * @version 3.0, 3.7
 * @since Feb 3, 2014
 *
 */
public class NotificationAPIImpl implements NotificationAPI {

	private final NotificationFactory notificationFactory;
	private final SystemEventsAPI systemEventsAPI;
	private final MarshalUtils marshalUtils;
	private final ConversionUtils conversionUtils;

	/**
	 * Retrieve the factory class that interacts with the database.
	 */
	public NotificationAPIImpl() {
		this.notificationFactory = FactoryLocator.getNotificationFactory();
		this.systemEventsAPI = APILocator.getSystemEventsAPI();
		this.marshalUtils = MarshalFactory.getInstance().getMarshalUtils();
		this.conversionUtils = ConversionUtils.INSTANCE;
	}

	@Override
	public void info(final String message, final String userId) {
		try {
			generateNotification(message, NotificationLevel.INFO, userId);
		} catch (DotDataException e) {
			Logger.error(this, "Error generating INFO Notification", e);
		}
	}

	@Override
	public void error(final String message, final String userId) {
		try {
			generateNotification(message, NotificationLevel.ERROR, userId);
		} catch (DotDataException e) {
			Logger.error(this, "Error generating ERROR Notification", e);
		}
	}

	@Override
	public void generateNotification(final String message, final NotificationLevel level, final String userId) throws DotDataException {
		// Adding notification to System Events table
		final NotificationData notificationData = new NotificationData("", message, null);
		final Notification notification = new Notification(level, userId, notificationData);
		this.systemEventsAPI.pushNotification(notification);
		// Adding notification to original Notification table
		final String messageBody = this.marshalUtils.marshal(notificationData);
		NotificationType type = (UtilMethods.isSet(notification.getType())) ? notification.getType()
				: NotificationType.GENERIC;
		final NotificationDTO notificationDTO = new NotificationDTO(notification.getId(), messageBody, type.name(),
				notification.getLevel().name(), userId, notification.getTimeSent(), notification.getWasRead());
		this.notificationFactory.saveNotification(notificationDTO);
	}

	@Override
	public Notification findNotification(final String notificationId) throws DotDataException {
		final NotificationDTO notificationDTO = this.notificationFactory.findNotification(notificationId);
		return convertNotification(notificationDTO); 
	}

	@Override
	public void deleteNotification(final String notificationId) throws DotDataException {
		this.notificationFactory.deleteNotification(notificationId);
	}

	@Override
	public void deleteNotifications(final String userId) throws DotDataException {
		this.notificationFactory.deleteNotifications(userId);
	}

	@Override
	public List<Notification> getNotifications(final long offset, final long limit) throws DotDataException {
		final List<NotificationDTO> notificationList = this.notificationFactory.getNotifications(offset, limit);
		return this.conversionUtils.convert(notificationList, (NotificationDTO notification) -> {
			return convertNotification(notification);
		});
	}

	@Override
	public Long getNotificationsCount() throws DotDataException {
		return this.notificationFactory.getNotificationsCount(null);
	}

	@Override
	public Long getNotificationsCount(final String userId) throws DotDataException {
		return this.notificationFactory.getNotificationsCount(userId);
	}

	@Override
	public List<Notification> getAllNotifications(final String userId) throws DotDataException {
		final List<NotificationDTO> notificationList = this.notificationFactory.getAllNotifications(userId);
		return this.conversionUtils.convert(notificationList, (NotificationDTO notification) -> {
			return convertNotification(notification);
		});
	}

	@Override
	public List<Notification> getNotifications(final String userId, final long offset, final long limit) throws DotDataException {
		final List<NotificationDTO> notificationList = this.notificationFactory.getNotifications(userId, offset, limit);
		return this.conversionUtils.convert(notificationList, (NotificationDTO notification) -> {
			return convertNotification(notification);
		});
	}

	@Override
	public Long getNewNotificationsCount(final String userId)  throws DotDataException {
		Long count = CacheLocator.getNewNotificationCache().get(userId);

		if(!UtilMethods.isSet(count)) {
			count = this.notificationFactory.getNewNotificationsCount(userId);
			CacheLocator.getNewNotificationCache().add(userId, count);
		}

		return count;
	}

	@Override
	public void markNotificationsAsRead(final String userId) throws DotDataException {
		this.notificationFactory.markNotificationsAsRead(userId);
		CacheLocator.getNewNotificationCache().remove(userId);
	}

	/**
	 * 
	 * @param record
	 * @return
	 */
	private Notification convertNotification(final NotificationDTO record) {
		final String id = record.getId();
		final String message = record.getMessage();
		final NotificationType type = NotificationType.valueOf(record.getType());
		final NotificationLevel level = NotificationLevel.valueOf(record.getLevel());
		final String userId = record.getUserId();
		final Date timeSent = record.getTimeSent();
		final boolean wasRead = record.getWasRead();
		NotificationData data;
		try {
			data = this.marshalUtils.unmarshal(message, NotificationData.class);
		} catch (Exception e) {
			// "message" field is not a valid JSON, so just store it as String
			data = new NotificationData("", message, null);
		}
		return new Notification(id, type, level, userId, timeSent, wasRead, data);
	}

}
