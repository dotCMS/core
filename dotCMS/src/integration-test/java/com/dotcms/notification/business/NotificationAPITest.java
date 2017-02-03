package com.dotcms.notification.business;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import com.dotcms.TestBase;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import org.junit.Test;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.liferay.portal.model.User;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

public class NotificationAPITest extends TestBase  {


	@Test
	public void testSaveDeleteNotification() throws Exception {
		User sysuser=APILocator.getUserAPI().getSystemUser();
		APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());
		Notification n = new Notification("NotificationTest1", NotificationLevel.ERROR, sysuser.getUserId());
		n.setId(UUID.randomUUID().toString());

		try {
			HibernateUtil.startTransaction();
			FactoryLocator.getNotificationFactory().saveNotification(n);
			Notification lastest = APILocator.getNotificationAPI().findNotification(n.getId());
			assertTrue(n.getMessage().equals(lastest.getMessage()));

			APILocator.getNotificationAPI().deleteNotification(lastest.getId());
			assertNull(APILocator.getNotificationAPI().findNotification(lastest.getId()));
			HibernateUtil.commitTransaction();
		} catch (DotDataException e) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(getClass(), "Could not rollback transaction", e);
			}

			throw new Exception("Error in NotificationAPITest.testSaveDeleteNotification", e);
		}
	}

	@Test
	public void testNewNotificationsCount() throws Exception {
		User sysuser=APILocator.getUserAPI().getSystemUser();
		APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());
		
		try {
			HibernateUtil.startTransaction();
			for(int i=0; i<10; i++) {
				Notification n = new Notification("NotificationTest"+i, NotificationLevel.ERROR, sysuser.getUserId());
				n.setId(UUID.randomUUID().toString());
				FactoryLocator.getNotificationFactory().saveNotification(n);
			}
			assertTrue(APILocator.getNotificationAPI().getNewNotificationsCount(sysuser.getUserId())==10);

			APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());

		} catch (DotDataException e) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(getClass(), "Could not rollback transaction", e);
			}

			throw new Exception("Error in NotificationAPITest.testNewNotificationsCount", e);
		}


	}

	@Test
	public void testGetPaginatedNotifications() throws Exception {
		User sysuser=APILocator.getUserAPI().getSystemUser();
		APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());
		
		try {
			HibernateUtil.startTransaction();

			for(int i=0; i<10; i++) {
				Notification n = new Notification("NotificationTest"+i, NotificationLevel.ERROR, sysuser.getUserId());
				n.setId(UUID.randomUUID().toString());
				FactoryLocator.getNotificationFactory().saveNotification(n);
			}

			List<Notification> notifications = APILocator.getNotificationAPI().getNotifications(sysuser.getUserId(), 0, 5);
			assertTrue(notifications.size()==5);

			APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());

		} catch (DotDataException e) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(getClass(), "Could not rollback transaction", e);
			}

			throw new Exception("Error in NotificationAPITest.testGetPaginatedNotifications", e);
		}
	}

}
