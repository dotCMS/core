package com.dotcms.notifications.business;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.dto.NotificationDTO;
import static com.dotcms.util.CollectionsUtils.*;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Concrete implementation of the {@link NotificationFactory} class.
 * 
 * @author Daniel Silva
 * @version 3.0, 3.7
 * @since Feb 3, 2014
 *
 */
public class NotificationFactoryImpl extends NotificationFactory {

	@Override
	public void saveNotification(final NotificationDTO notification) throws DotDataException {
		final DotConnect dc = new DotConnect();
		dc.setSQL("insert into notification (id,message,notification_type,notification_level,user_id,time_sent) "
				+ " values(?,?,?,?,?,?)");
		if (!UtilMethods.isSet(notification.getId())) {
			notification.setId(UUID.randomUUID().toString());
		}
		dc.addParam(notification.getId());
		dc.addParam(notification.getMessage());
		dc.addParam(UtilMethods.isSet(notification.getType()) ? notification.getType() : NotificationType.GENERIC.name());
		dc.addParam(notification.getLevel());
		dc.addParam(notification.getUserId());
		dc.addParam(new Date());

		Connection conn = null;

		try {
			conn = DbConnectionFactory.getDataSource().getConnection();
			conn.setAutoCommit(false);
			dc.loadResult(conn);
			conn.commit();
		} catch (SQLException e) {
			Logger.error(this, "Error creating notification.", e);
			try {
				if(conn!=null) {
					conn.rollback();
				}
			} catch (SQLException q) {
				Logger.error(this, "Error rolling back transaction.", e);
			}
		} finally {
			if(conn!=null) {
				try {
					conn.setAutoCommit(true);
				} catch (SQLException e) {
					Logger.error(this, "Error setting autocommit to true.", e);
				}

				try {
					conn.close();
				} catch (SQLException e) {
					Logger.error(this, "Error closing db connection.", e);
				}
			}
		}


		CacheLocator.getNewNotificationCache().remove(notification.getUserId());
	}

	@Override
	public NotificationDTO findNotification(String notificationId) throws DotDataException {
		final DotConnect dc = new DotConnect();
		dc.setSQL("select * from notification where id = ?");
		dc.addParam(notificationId);
		NotificationDTO notification = null;
		final List<Map<String, Object>> results = dc.loadObjectResults();
		if (results != null && !results.isEmpty()) {
			Map<String, Object> row = results.get(0);
			final String id = (String) row.get("id");
			final String message = (String) row.get("message");
			final String type = (String) row.get("notification_type");
			final String level = (String) row.get("notification_level");
			final String userId = (String) row.get("user_id");
			final Date timeSent = (Date) row.get("time_sent");
			final boolean wasRead = DbConnectionFactory.isDBTrue(row.get("was_read").toString());
			notification = new NotificationDTO(id, message, type, level, userId, timeSent, wasRead);
		}
		return notification;
	}

	@Override
	public void deleteNotification(final String notificationId) throws DotDataException {
		final DotConnect dc = new DotConnect();
		dc.setSQL("delete from notification where id = ?");
		dc.addParam(notificationId);
		dc.loadObjectResults();
	}

	@Override
	public void deleteNotifications(final String userId) throws DotDataException {
		final DotConnect dc = new DotConnect();
		final String userWhere = UtilMethods.isSet(userId) ? " where user_id = ? " : "";
		dc.setSQL("delete from notification " + userWhere);

		if (UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

		dc.loadObjectResults();
	}

	@Override
	public void deleteNotification(final String[] notificationsId) throws DotDataException {

		final DotConnect dc = new DotConnect();
		final List<Params> params = list();

		for (String notificationId : notificationsId) {

			params.add(new Params(notificationId));
		}

		final int [] results = dc.executeBatch("delete from notification where id = ?",
				params);

		if (results.length == notificationsId.length) {

			if (Logger.isDebugEnabled(this.getClass())) {

				Logger.debug(this.getClass(),
						"All the notifications: " + Arrays.asList(notificationsId) +
								" were deleted."
				);
			}
		} else {

			if (Logger.isDebugEnabled(this.getClass())) {

				Logger.debug(this.getClass(),
						"Of the notifications: " + Arrays.asList(notificationsId) +
								" were just deleted: " + results.length
				);
			}

			// todo: should throw an exception reporting the error.
		}
	}

	@Override
	public List<NotificationDTO> getNotifications(final long offset, final long limit) throws DotDataException {
		return getNotifications(null, offset, limit);
	}

	@Override
	public List<NotificationDTO> getAllNotifications(final String userId) throws DotDataException {
		return getNotifications(userId, -1, -1);
	}

	@Override
	public Long getNotificationsCount(final String userId) throws DotDataException {
		DotConnect dc = new DotConnect();
		String userWhere = UtilMethods.isSet(userId) ? "where user_id = ? " : "";
		dc.setSQL("select count(*) as count from notification " + userWhere);

		if (UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

		List<Map<String, Object>> results = dc.loadObjectResults();
		Long count = Long.parseLong(results.get(0).get("count").toString());
		return count;
	}

	@Override
	public List<NotificationDTO> getNotifications(final String userId, final long offset, final long limit)
			throws DotDataException {
		final DotConnect dc = new DotConnect();
		final String userWhere = UtilMethods.isSet(userId) ? " where user_id = ? " : "";
		final String sql = "select * from notification " + userWhere + " order by time_sent desc";
		dc.setSQL((UtilMethods.isSet(offset) && offset > -1 && UtilMethods.isSet(limit) && limit > 0) ? SQLUtil.addLimits(
				sql, offset, limit) : sql);
		if (UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

		final List<Map<String, Object>> results = dc.loadObjectResults();
		final List<NotificationDTO> notifications = new ArrayList<>();
		for (Map<String, Object> row : results) {
			final String id = (String) row.get("id");
			final String message = (String) row.get("message");
			final String type = (String) row.get("notification_type");
			final String level = (String) row.get("notification_level");
			final Date timeSent = (Date) row.get("time_sent");
			final boolean wasRead = DbConnectionFactory.isDBTrue(row.get("was_read").toString());
			final NotificationDTO notification = new NotificationDTO(id, message, type, level, userId, timeSent, wasRead);
			notifications.add(notification);
		}

		return notifications;
	}

	@Override
	public Long getNewNotificationsCount(final String userId) throws DotDataException {
		final DotConnect dc = new DotConnect();
		final String userWhere = UtilMethods.isSet(userId) ? " user_id = ? and " : "";
		dc.setSQL("select count(*) as count from notification where " + userWhere + " was_read = "
				+ DbConnectionFactory.getDBFalse());

		if (UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

		final List<Map<String, Object>> results = dc.loadObjectResults();
		return ((Number) results.get(0).get("count")).longValue();
	}

	@Override
	public void markNotificationsAsRead(final String userId) throws DotDataException {
		final DotConnect dc = new DotConnect();
		if (!UtilMethods.isSet(userId))
			return;

		dc.setSQL("update notification set was_read = " + DbConnectionFactory.getDBTrue() + " where was_read = "
				+ DbConnectionFactory.getDBFalse() + " and user_id = ?");
		dc.addParam(userId);
		dc.loadResult();
	}

}
