package com.dotcms.notifications.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

public class NotificationFactoryImpl extends NotificationFactory {

	final DotConnect dc;
	private String TIMESTAMPSQL = "NOW()";

	public NotificationFactoryImpl() {
		 dc = new DotConnect();

		 if (DbConnectionFactory.isMsSql()) {
	            TIMESTAMPSQL = "GETDATE()";
	        } else if (DbConnectionFactory.isOracle()) {
	            TIMESTAMPSQL = "CAST(SYSTIMESTAMP AS TIMESTAMP)";
	        }
	}

	public void saveNotification(Notification notification) throws DotDataException {
		dc.setSQL("insert into notification values(?,?,?,?,?,"+TIMESTAMPSQL+")");

		dc.addParam(UUID.randomUUID().toString());
		dc.addParam(notification.getMessage());
		dc.addParam(UtilMethods.isSet(notification.getType())?notification.getType().name():NotificationType.GENERIC.name());
		dc.addParam(notification.getLevel().name());
		dc.addParam(notification.getUserId());
		dc.loadResult();
		CacheLocator.getNewNotificationCache().remove(notification.getUserId());
	}

	public List<Notification> getAllNotificationsForUser(String userId) throws DotDataException {
		return getNotificationsForUser(userId, -1, -1);
	}

	public List<Notification> getNotificationsForUser(String userId, long offset, long limit) throws DotDataException {


		dc.setSQL(SQLUtil.addLimits("select * from notification where user_id = ?", offset, limit));
		dc.addParam(userId);

		List<Map<String, Object>> results = dc.loadObjectResults();
		List<Notification> notifications = new ArrayList<Notification>();

		for (Map<String, Object> row : results) {
			Notification n = new Notification();
			n.setId((String)row.get("id"));
			n.setMessage((String)row.get("message"));
			n.setType(NotificationType.valueOf((String)row.get("notification_type")));
			n.setLevel(NotificationLevel.valueOf((String)row.get("notification_level")));
			n.setUserId((String)row.get("user_id"));
			n.setTimeSent((Date)row.get("time_sent"));
			n.setWasRead((DbConnectionFactory.isDBTrue(row.get("was_read").toString())));
			notifications.add(n);
		}

		return notifications;
	}

	public Long getNewNotificationsCount(String userId)  throws DotDataException {
		dc.setSQL("select count(*) as count from notification where user_id = ? and was_read = " + DbConnectionFactory.getDBFalse());
		dc.addParam(userId);
		List<Map<String, Object>> results = dc.loadObjectResults();
		Long count = (Long) results.get(0).get("count");
		return count;
	}

	public void markNotificationsAsRead(String userId) throws DotDataException {
		dc.setSQL("update notification set was_read = "+ DbConnectionFactory.getDBTrue()
				+ " where was_read = "+ DbConnectionFactory.getDBFalse()+" and user_id = ?");
		dc.addParam(userId);
		dc.loadResult();

	}
}
