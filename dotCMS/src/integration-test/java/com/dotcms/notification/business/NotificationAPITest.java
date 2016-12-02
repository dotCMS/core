package com.dotcms.notification.business;

import com.dotcms.TestBase;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.dto.NotificationDTO;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

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

	@Test
	public void testSaveDeleteNotification() throws Exception {

		//Get the system user for testing
		User systemUser = APILocator.getUserAPI().getSystemUser();

		//Cleaning up the notifications in order to prepare them for the test
		APILocator.getNotificationAPI().deleteNotifications(systemUser.getUserId());

		//Create a new notification for this system user
		NotificationDTO notificationDTO = new NotificationDTO(StringUtils.EMPTY, "Notification message",
				NotificationType.GENERIC.name(), NotificationLevel.ERROR.name(), systemUser.getUserId(), new Date(),
				Boolean.FALSE);

		try {
			HibernateUtil.startTransaction();

			//Save the notification
			FactoryLocator.getNotificationFactory().saveNotification(notificationDTO);

			//Make sure is there
			Notification latest = APILocator.getNotificationAPI().findNotification(systemUser.getUserId(), notificationDTO.getGroupId());
			assertTrue(notificationDTO.getMessage().equals(latest.getMessage().getKey()));

			//Delete it
			APILocator.getNotificationAPI().deleteNotification(latest.getUserId(), latest.getGroupId());

			HibernateUtil.commitTransaction();

			//Make sure it was deleted
			assertNull(APILocator.getNotificationAPI().findNotification(systemUser.getUserId(), latest.getGroupId()));
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

		//Get the system user for testing
		User systemUser = APILocator.getUserAPI().getSystemUser();

        //Cleaning up the notifications in order to prepare them for the test
		APILocator.getNotificationAPI().deleteNotifications(systemUser.getUserId());
		
		try {
			HibernateUtil.startTransaction();

			for(int i=0; i<10; i++) {
				//Creating the new notification
				NotificationDTO notificationDTO = new NotificationDTO(StringUtils.EMPTY, "Notification message #" + i,
						NotificationType.GENERIC.name(), NotificationLevel.ERROR.name(), systemUser.getUserId(), new Date(),
						Boolean.FALSE);
				//Saving it
				FactoryLocator.getNotificationFactory().saveNotification(notificationDTO);
			}
			//Make sure the count is returning the proper value
			assertTrue(APILocator.getNotificationAPI().getNewNotificationsCount(systemUser.getUserId()) == 10);

			//Delete all the notifications for this user
			APILocator.getNotificationAPI().deleteNotifications(systemUser.getUserId());

			//Make sure the count is returning the proper value
			assertTrue(APILocator.getNotificationAPI().getNewNotificationsCount(systemUser.getUserId()) == 0);

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

		//Get the system user for testing
		User systemUser = APILocator.getUserAPI().getSystemUser();

        //Cleaning up the notifications in order to prepare them for the test
		APILocator.getNotificationAPI().deleteNotifications(systemUser.getUserId());
		
		try {
			HibernateUtil.startTransaction();

			for(int i=0; i<10; i++) {
                //Creating the new notification
				NotificationDTO notificationDTO = new NotificationDTO(StringUtils.EMPTY, "Notification message #" + i,
						NotificationType.GENERIC.name(), NotificationLevel.ERROR.name(), systemUser.getUserId(), new Date(),
						Boolean.FALSE);
                //Saving it
				FactoryLocator.getNotificationFactory().saveNotification(notificationDTO);
			}

			//Make sure the pagination is working properly
			List<Notification> notifications = APILocator.getNotificationAPI().getNotifications(systemUser.getUserId(), 0, 5);
			assertTrue(notifications.size()==5);

			//Delete all the notifications for this user
			APILocator.getNotificationAPI().deleteNotifications(systemUser.getUserId());

		} catch (DotDataException e) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(getClass(), "Could not rollback transaction", e);
			}

			throw new Exception("Error in NotificationAPITest.testGetPaginatedNotifications", e);
		}
	}

	/**
	 * The idea of this method is to test the {@link com.dotcms.notifications.business.NotificationFactory#saveNotificationsForUsers(NotificationDTO, Collection)}
	 * and the concept of the groups ids.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSaveNotificationsForUsers() throws Exception {

		//Get the system user for testing
		User systemUser = APILocator.getUserAPI().getSystemUser();

		//Create some test users
		String time = String.valueOf(new Date().getTime());
		String user1Email = time + "@test.com";
		User newUser1 = APILocator.getUserAPI().createUser(user1Email, user1Email);
		newUser1.setFirstName("Test");
		newUser1.setLastName("User");
		APILocator.getUserAPI().save(newUser1, systemUser, false);

		time = String.valueOf(new Date().getTime());
		String user2Email = time + "@test.com";
		User newUser2 = APILocator.getUserAPI().createUser(user2Email, user2Email);
		newUser2.setFirstName("Test");
		newUser2.setLastName("User");
		APILocator.getUserAPI().save(newUser2, systemUser, false);

		Collection<User> testUsers = new ArrayList<>();
		testUsers.add(systemUser);
		testUsers.add(newUser1);
		testUsers.add(newUser2);

		//Create a new notification for this system user
		NotificationDTO notificationDTO = new NotificationDTO(StringUtils.EMPTY, "Notification test message",
				NotificationType.GENERIC.name(), NotificationLevel.ERROR.name(), systemUser.getUserId(), new Date(),
				Boolean.FALSE);

		try {
			HibernateUtil.startTransaction();

			//Save the notification
			FactoryLocator.getNotificationFactory().saveNotificationsForUsers(notificationDTO, testUsers);

			//Make sure is there for all the users
			Notification foundNotification;
			for ( User user : testUsers ) {

				foundNotification = APILocator.getNotificationAPI().findNotification(user.getUserId(), notificationDTO.getGroupId());

				//Some validations
				assertNotNull(foundNotification);
				assertTrue(notificationDTO.getGroupId().equals(foundNotification.getGroupId()));
				assertTrue(foundNotification.getUserId().equals(user.getUserId()));
			}

			//Make sure all the users have unread notifications
			for ( User user : testUsers ) {
				assertTrue(APILocator.getNotificationAPI().getNewNotificationsCount(user.getUserId()) == 1);
			}

			//Mark as read the notifications for just one user
			APILocator.getNotificationAPI().markNotificationsAsRead(newUser1.getUserId());
			assertTrue(APILocator.getNotificationAPI().getNewNotificationsCount(systemUser.getUserId()) == 1);
			assertTrue(APILocator.getNotificationAPI().getNewNotificationsCount(newUser1.getUserId()) == 0);//Should not have unread notifications
			assertTrue(APILocator.getNotificationAPI().getNewNotificationsCount(newUser2.getUserId()) == 1);

			//Delete the notification for just one user
			APILocator.getNotificationAPI().deleteNotification(newUser2.getUserId(), notificationDTO.getGroupId());
			assertTrue(APILocator.getNotificationAPI().getNotificationsCount(systemUser.getUserId()) == 1);
			assertTrue(APILocator.getNotificationAPI().getNotificationsCount(newUser1.getUserId()) == 1);
			assertTrue(APILocator.getNotificationAPI().getNotificationsCount(newUser2.getUserId()) == 0);

			//And finally delete all for clean up
			APILocator.getNotificationAPI().deleteNotifications(systemUser.getUserId());
			APILocator.getNotificationAPI().deleteNotifications(newUser1.getUserId());
			APILocator.getNotificationAPI().deleteNotifications(newUser2.getUserId());

			HibernateUtil.commitTransaction();

			//Make sure everything is clean
			assertTrue(APILocator.getNotificationAPI().getNotificationsCount(systemUser.getUserId()) == 0);
			assertTrue(APILocator.getNotificationAPI().getNotificationsCount(newUser1.getUserId()) == 0);
			assertTrue(APILocator.getNotificationAPI().getNotificationsCount(newUser2.getUserId()) == 0);
		} catch (DotDataException e) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(getClass(), "Could not rollback transaction", e);
			}

			throw new Exception("Error in NotificationAPITest.testSaveDeleteNotification", e);
		}
	}

}
