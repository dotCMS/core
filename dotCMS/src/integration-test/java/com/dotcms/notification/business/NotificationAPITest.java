package com.dotcms.notification.business;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.dto.NotificationDTO;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * 
 * @author Daniel Silva
 * @version 3.0, 3.7
 * @since Feb 5, 2014
 *
 */
public class NotificationAPITest extends TestBase  {
	
	@BeforeClass
	public static void prepare() throws Exception{
    	//Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}

	//This one still fails
	@Test
	public void testSaveDeleteNotification() throws Exception {
		User sysuser=APILocator.getUserAPI().getSystemUser();
		APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());
		//Notification n = new Notification("NotificationTest1", NotificationLevel.ERROR, sysuser.getUserId());
		NotificationDTO notificationDTO = new NotificationDTO(UUID.randomUUID().toString(), "Notification message",
				NotificationType.GENERIC.name(), NotificationLevel.ERROR.name(), sysuser.getUserId(), new Date(),
				Boolean.FALSE);
		//n.setId(UUID.randomUUID().toString());

		try {
			HibernateUtil.startTransaction();
			FactoryLocator.getNotificationFactory().saveNotification(notificationDTO);
			Notification lastest = APILocator.getNotificationAPI().findNotification(notificationDTO.getId());
			assertTrue(notificationDTO.getMessage().equals(lastest.getMessage()));

			APILocator.getNotificationAPI().deleteNotification(lastest.getUserId(), lastest.getId());
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

        //Cleaning up the notifications in order to prepare them for the test
		APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());
		
		try {
			HibernateUtil.startTransaction();
			for(int i=0; i<10; i++) {
				//Creating the new notification
				NotificationDTO notificationDTO = new NotificationDTO(UUID.randomUUID().toString(), "Notification message #" + i,
						NotificationType.GENERIC.name(), NotificationLevel.ERROR.name(), sysuser.getUserId(), new Date(),
						Boolean.FALSE);
				//Saving it
				FactoryLocator.getNotificationFactory().saveNotification(notificationDTO);
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

        //Cleaning up the notifications in order to prepare them for the test
		APILocator.getNotificationAPI().deleteNotifications(sysuser.getUserId());
		
		try {
			HibernateUtil.startTransaction();

			for(int i=0; i<10; i++) {
                //Creating the new notification
				NotificationDTO notificationDTO = new NotificationDTO(UUID.randomUUID().toString(), "Notification message #" + i,
						NotificationType.GENERIC.name(), NotificationLevel.ERROR.name(), sysuser.getUserId(), new Date(),
						Boolean.FALSE);
                //Saving it
				FactoryLocator.getNotificationFactory().saveNotification(notificationDTO);
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
