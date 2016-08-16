package com.dotcms.notifications.business;

import com.dotcms.api.system.event.*;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.notifications.bean.*;
import com.dotcms.notifications.dto.NotificationDTO;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Concrete implementation of the {@link NotificationAPI} class.
 * 
 * @author Daniel Silva
 * @version 3.0, 3.7
 * @since Feb 3, 2014
 *
 */
public class NotificationAPIImpl implements NotificationAPI {

	public static final String NOTIFICATIONS_THREAD_POOL_SUBMITTER_NAME = "notifications";
	private final NotificationFactory notificationFactory;
	private final SystemEventsAPI systemEventsAPI;
	private final MarshalUtils marshalUtils;
	private final ConversionUtils conversionUtils;
	private final DotConcurrentFactory dotConcurrentFactory;
	private final DotSubmitter dotSubmitter;

	/**
	 * Retrieve the factory class that interacts with the database.
	 */
	public NotificationAPIImpl() {
		this.notificationFactory = FactoryLocator.getNotificationFactory();
		this.systemEventsAPI = APILocator.getSystemEventsAPI();
		this.marshalUtils = MarshalFactory.getInstance().getMarshalUtils();
		this.conversionUtils = ConversionUtils.INSTANCE;
		this.dotConcurrentFactory = DotConcurrentFactory.getInstance();
		this.dotSubmitter = // getting the thread pool for notifications.
				this.dotConcurrentFactory.getSubmitter(NOTIFICATIONS_THREAD_POOL_SUBMITTER_NAME); // by default use the standard configuration, but it can be override via properties config.
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

		this.generateNotification(message, level, NotificationType.GENERIC, userId);
	}

	@Override
	public void generateNotification(String message, NotificationLevel level, NotificationType type, String userId) throws DotDataException {

		this.generateNotification(StringUtils.EMPTY, message, null, level, NotificationType.GENERIC, userId);
	}

	@Override
	public void generateNotification(String title, String message, List<NotificationAction> actions,
									 NotificationLevel level, NotificationType type, String userId) throws DotDataException {

		this.generateNotification(title, message, actions, level, type, userId, null);
	}

	@Override
	public void generateNotification(final String title, final String message, final List<NotificationAction> actions,
									 final NotificationLevel level, final NotificationType type, final String userId, final Locale locale) throws DotDataException {

		// since the notification is not a priory process on the current thread, we decided to execute it async
		this.dotSubmitter.execute(() -> {

				final NotificationData data = new NotificationData(title, message, actions);
				final String msg = this.marshalUtils.marshal(data);
				final NotificationDTO dto = new NotificationDTO(StringUtils.EMPTY, msg, type.name(), level.name(), userId, null,
						Boolean.FALSE);

				try {

					Logger.debug(NotificationAPIImpl.class, "Storing the notification: " + dto);

					this.notificationFactory.saveNotification(dto);
				} catch (DotDataException e) {
					if (Logger.isErrorEnabled(NotificationAPIImpl.class)) {

						Logger.error(NotificationAPIImpl.class, e.getMessage(), e);
					}
				}

				CacheLocator.getNewNotificationCache().remove(userId);
				// Adding notification to System Events table
				final Notification n = new Notification(level, userId, data);
				final Payload payload = new Payload(n, Visibility.USER, userId);

				n.setId(dto.getId());
				n.setTimeSent(new Date());
				n.setPrettyDate(DateUtil.prettyDateSince(n.getTimeSent(), locale));

				final SystemEvent systemEvent = new SystemEvent(SystemEventType.NOTIFICATION, payload);

				try {

					Logger.debug(NotificationAPIImpl.class, "Pushing the event: " + systemEvent);

					this.systemEventsAPI.push(systemEvent);
				} catch (DotDataException e) {
					if (Logger.isErrorEnabled(NotificationAPIImpl.class)) {

						Logger.error(NotificationAPIImpl.class, e.getMessage(), e);
					}
				}
		});
	}

	@Override
	public Notification findNotification(String notificationId) throws DotDataException {
		NotificationDTO dto = notificationFactory.findNotification(notificationId);
		return this.conversionUtils.convert(dto, (NotificationDTO record) -> {
			return convertNotificationDTO(record);
		});
	}

	@Override
	public void deleteNotification(String notificationId) throws DotDataException {
		notificationFactory.deleteNotification(notificationId);
	}


	@Override
	public void deleteNotifications(final String... notificationsId) throws DotDataException {
		notificationFactory.deleteNotification(notificationsId);
	}

	@Override
	public void deleteNotifications(String userId) throws DotDataException {
		notificationFactory.deleteNotifications(userId);
	}

	@Override
	public List<Notification> getNotifications(long offset, long limit) throws DotDataException {
		List<NotificationDTO> dtos = this.notificationFactory.getNotifications(offset, limit);
		return this.conversionUtils.convert(dtos, (NotificationDTO record) -> {
			return convertNotificationDTO(record);
		});
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
		List<NotificationDTO> dtos = this.notificationFactory.getAllNotifications(userId);
		return this.conversionUtils.convert(dtos, (NotificationDTO record) -> {
			return convertNotificationDTO(record);
		});
	}

	@Override
	public List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException {
		List<NotificationDTO> dtos = this.notificationFactory.getNotifications(userId, offset, limit);
		return this.conversionUtils.convert(dtos, (NotificationDTO record) -> {
			return convertNotificationDTO(record);
		});
	}

	@Override
	public Long getNewNotificationsCount(String userId) throws DotDataException {
		Long count = CacheLocator.getNewNotificationCache().get(userId);
		if (!UtilMethods.isSet(count)) {
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

	/**
	 * Converts the physical representation of a Notification (i.e., the
	 * information as stored in the database) to the logical representation.
	 * 
	 * @param record
	 *            - The {@link NotificationDTO} object.
	 * @return The {@link Notification} object.
	 */
	private Notification convertNotificationDTO(NotificationDTO record) {
		final String id = record.getId();
		final String typeStr = record.getType();
		final String levelStr = record.getLevel();
		final NotificationType type = (UtilMethods.isSet(typeStr)) ? NotificationType.valueOf(typeStr)
				: NotificationType.GENERIC;
		final NotificationLevel level = (UtilMethods.isSet(levelStr)) ? NotificationLevel.valueOf(levelStr)
				: NotificationLevel.INFO;
		final String userId = record.getUserId();
		final Date timeSent = record.getTimeSent();
		final boolean wasRead = record.getWasRead();
		NotificationData data;
		try {
			data = this.marshalUtils.unmarshal(record.getMessage(), NotificationData.class);
		} catch (Exception e) {
			// If JSON cannot be parsed, just put the message
			data = new NotificationData("", record.getMessage(), null);
		}
		return new Notification(id, type, level, userId, timeSent, wasRead, data);
	}

}
