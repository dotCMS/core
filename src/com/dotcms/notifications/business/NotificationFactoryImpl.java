package com.dotcms.notifications.business;

import java.util.ArrayList;
import java.util.Calendar;
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

	public void saveNotification(Notification notification) throws DotDataException {
	    DotConnect dc = new DotConnect();
		dc.setSQL("insert into notification (id,message,notification_type,notification_level,user_id,time_sent) "
		        + " values(?,?,?,?,?,?)");

		if(!UtilMethods.isSet(notification.getId())) {
			notification.setId(UUID.randomUUID().toString());
		}

		dc.addParam(notification.getId());
		dc.addParam(notification.getMessage());
		dc.addParam(UtilMethods.isSet(notification.getType())?notification.getType().name():NotificationType.GENERIC.name());
		dc.addParam(notification.getLevel().name());
		dc.addParam(notification.getUserId());
		dc.addParam(new Date());
		dc.loadResult();
		CacheLocator.getNewNotificationCache().remove(notification.getUserId());
	}

	public Notification findNotification(String notificationId) throws DotDataException {
	    DotConnect dc = new DotConnect();
		dc.setSQL("select * from notification where id = ?");
		dc.addParam(notificationId);

		Notification n = null;
		List<Map<String, Object>> results = dc.loadObjectResults();

		if(results!=null && !results.isEmpty()) {
			Map<String, Object> row = results.get(0);
			n = new Notification();
			n.setId((String)row.get("id"));
			n.setMessage((String)row.get("message"));
			n.setType(NotificationType.valueOf((String)row.get("notification_type")));
			n.setLevel(NotificationLevel.valueOf((String)row.get("notification_level")));
			n.setUserId((String)row.get("user_id"));
			n.setTimeSent((Date)row.get("time_sent"));
			n.setWasRead((DbConnectionFactory.isDBTrue(row.get("was_read").toString())));
		}

		return n;
	}

	public void deleteNotification(String notificationId) throws DotDataException {
	    DotConnect dc = new DotConnect();
		dc.setSQL("delete from notification where id = ?");
		dc.addParam(notificationId);
		dc.loadObjectResults();
	}

	public void deleteNotifications(String userId) throws DotDataException {
	    DotConnect dc = new DotConnect();
		String userWhere = UtilMethods.isSet(userId)?" where user_id = ? ":"";
		dc.setSQL("delete from notification " + userWhere);

		if(UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

		dc.loadObjectResults();
	}

	public List<Notification> getNotifications(long offset, long limit) throws DotDataException {
		return getNotifications(null, offset, limit);
	}

	public List<Notification> getAllNotifications(String userId) throws DotDataException {
		return getNotifications(userId, -1, -1);
	}

	public Long getNotificationsCount(String userId) throws DotDataException {
	    DotConnect dc = new DotConnect();
		String userWhere = UtilMethods.isSet(userId)?"where user_id = ? ":"";
		dc.setSQL("select count(*) as count from notification " + userWhere);

		if(UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

		List<Map<String, Object>> results = dc.loadObjectResults();
		Long count = Long.parseLong(results.get(0).get("count").toString());
		return count;
	}

	public List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException {
	    DotConnect dc = new DotConnect();
		String userWhere = UtilMethods.isSet(userId)?" where user_id = ? ":"";
		String sql = "select * from notification " + userWhere + " order by time_sent desc";
		dc.setSQL( (UtilMethods.isSet(offset)&&offset>-1 && UtilMethods.isSet(limit) && limit>0)
				? SQLUtil.addLimits(sql, offset, limit)
						: sql);

		if(UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

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
	    DotConnect dc = new DotConnect();
		String userWhere = UtilMethods.isSet(userId)?" user_id = ? and ":"";
		dc.setSQL("select count(*) as count from notification where " + userWhere + " was_read = " + DbConnectionFactory.getDBFalse());

		if(UtilMethods.isSet(userId)) {
			dc.addParam(userId);
		}

		List<Map<String, Object>> results = dc.loadObjectResults();
		Long count = Long.parseLong(results.get(0).get("count").toString());
		return count;
	}

	public void markNotificationsAsRead(String userId) throws DotDataException {
	    DotConnect dc = new DotConnect();
		if(!UtilMethods.isSet(userId)) return;

		dc.setSQL("update notification set was_read = "+ DbConnectionFactory.getDBTrue()
				+ " where was_read = "+ DbConnectionFactory.getDBFalse()+" and user_id = ?");
		dc.addParam(userId);
		dc.loadResult();
	}
}
