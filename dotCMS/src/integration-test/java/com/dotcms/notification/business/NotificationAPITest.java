package com.dotcms.notification.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.business.NotificationAPIImpl;
import com.dotcms.notifications.business.NotificationFactory;
import com.dotcms.notifications.dto.NotificationDTO;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 *
 * @author Daniel Silva
 * @version 3.0, 3.7
 * @since Feb 5, 2014
 *
 */
public class NotificationAPITest extends IntegrationTestBase  {

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

        final NotificationAPI notificationAPI = APILocator.getNotificationAPI();
        final NotificationFactory notificationFactory = FactoryLocator.getNotificationFactory();

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
            notificationFactory.saveNotificationsForUsers(notificationDTO, testUsers);

            //Make sure is there for all the users
            Notification foundNotification;
            for ( User user : testUsers ) {

                foundNotification = notificationAPI.findNotification(user.getUserId(), notificationDTO.getGroupId());

                //Some validations
                assertNotNull(foundNotification);
                assertTrue(notificationDTO.getGroupId().equals(foundNotification.getGroupId()));
                assertTrue(foundNotification.getUserId().equals(user.getUserId()));
            }

            //Make sure all the users have unread notifications
            for ( User user : testUsers ) {
                assertTrue(notificationAPI.getNewNotificationsCount(user.getUserId()) == 1);
            }

            //Mark as read the notifications for just one user
            notificationAPI.markNotificationsAsRead(newUser1.getUserId());
            assertTrue(notificationAPI.getNewNotificationsCount(systemUser.getUserId()) == 1);
            assertTrue(notificationAPI.getNewNotificationsCount(newUser1.getUserId()) == 0);//Should not have unread notifications
            assertTrue(notificationAPI.getNewNotificationsCount(newUser2.getUserId()) == 1);

            //Delete the notification for just one user
            notificationAPI.deleteNotification(newUser2.getUserId(), notificationDTO.getGroupId());
            assertTrue(notificationAPI.getNotificationsCount(systemUser.getUserId()) == 1);
            assertTrue(notificationAPI.getNotificationsCount(newUser1.getUserId()) == 1);
            assertTrue(notificationAPI.getNotificationsCount(newUser2.getUserId()) == 0);

            //And finally delete all for clean up
            notificationAPI.deleteNotifications(systemUser.getUserId());
            notificationAPI.deleteNotifications(newUser1.getUserId());
            notificationAPI.deleteNotifications(newUser2.getUserId());

            HibernateUtil.commitTransaction();

            //Make sure everything is clean
            assertTrue(notificationAPI.getNotificationsCount(systemUser.getUserId()) == 0);
            assertTrue(notificationAPI.getNotificationsCount(newUser1.getUserId()) == 0);
            assertTrue(notificationAPI.getNotificationsCount(newUser2.getUserId()) == 0);
        } catch (DotDataException e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(getClass(), "Could not rollback transaction", e);
            }

            throw new Exception("Error in NotificationAPITest.testSaveDeleteNotification", e);
        }
    }

    /**
     * The idea of this method is to test the
     * {@link com.dotcms.notifications.business.NotificationAPI#generateNotification(String, String, List, NotificationLevel, NotificationType, Visibility, String, String, Locale)}
     * method using the {@link Visibility#ROLE}
     *
     * @throws Exception
     */
    @Test
    public void generateNotificationForRole() throws Exception {

        final NotificationAPI notificationAPI = APILocator.getNotificationAPI();
        final RoleAPI roleAPI = APILocator.getRoleAPI();

        //Get the system user for testing
        User systemUser = APILocator.getUserAPI().getSystemUser();

        try {
            HibernateUtil.startTransaction();

            //Create a role for testing
            Role newRole = new Role();
            String newRoleName = "role-" + String.valueOf(new Date().getTime());
            newRole.setName(newRoleName);
            newRole.setRoleKey(newRoleName);
            newRole.setEditLayouts(true);
            newRole.setEditPermissions(true);
            newRole.setEditUsers(true);
            newRole.setSystem(false);
            newRole = roleAPI.save(newRole);

            //Create some test users
            String time = String.valueOf(new Date().getTime());
            String user1Email = time + "@test.com";
            User newUser1 = APILocator.getUserAPI().createUser(user1Email, user1Email);
            newUser1.setFirstName("Test1");
            newUser1.setLastName("User");
            APILocator.getUserAPI().save(newUser1, systemUser, false);
            roleAPI.addRoleToUser(newRole, newUser1);

            time = String.valueOf(new Date().getTime());
            String user2Email = time + "@test.com";
            User newUser2 = APILocator.getUserAPI().createUser(user2Email, user2Email);
            newUser2.setFirstName("Test2");
            newUser2.setLastName("User");
            APILocator.getUserAPI().save(newUser2, systemUser, false);
            roleAPI.addRoleToUser(newRole, newUser2);

            time = String.valueOf(new Date().getTime());
            String user3Email = time + "@test.com";
            User newUser3 = APILocator.getUserAPI().createUser(user3Email, user3Email);
            newUser3.setFirstName("Test3");
            newUser3.setLastName("User");
            APILocator.getUserAPI().save(newUser3, systemUser, false);
            roleAPI.addRoleToUser(newRole, newUser3);

            Collection<User> testUsers = new ArrayList<>();
            testUsers.add(newUser1);
            testUsers.add(newUser2);
            testUsers.add(newUser3);

            //Validate the user relation with the roles
            Collection<User> foundUsers = roleAPI.findUsersForRole(newRole.getId());
            assertNotNull(foundUsers);
            assertTrue(foundUsers.size() == 3);

            HibernateUtil.commitTransaction();

            //Generate a notification for the new created test role
            notificationAPI.generateNotification(
                "Test notification title",
                "Test notification message",
                null, // no actions
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                Visibility.ROLE,
                newRole.getId(),
                systemUser.getUserId(),
                systemUser.getLocale()
            );

            //Sleep some time in order to make sure the thread had time to start
            Thread.sleep(2000);

            //Using the DotSubmitter in order to know when the generation of the notification finished
            DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().
                getSubmitter(NotificationAPIImpl.NOTIFICATIONS_THREAD_POOL_SUBMITTER_NAME);
            while ( dotSubmitter.getActiveCount() > 0 ) {
                //Do nothing...., just wait the notification thread to finish it execution
                Thread.sleep(2000);
            }

            //Make sure the notification are there for all the users
            for ( User user : testUsers ) {
                assertTrue(notificationAPI.getNotificationsCount(user.getUserId()) == 1);
            }

            //Make sure all the users have unread notifications
            for ( User user : testUsers ) {
                assertTrue(notificationAPI.getNewNotificationsCount(user.getUserId()) == 1);
            }

            String groupId = null;
            //Make all the notifications share the same group id
            for ( User user : testUsers ) {

                //First get all the notifications for this user --> Should have only one
                List<Notification> foundNotifications = notificationAPI.getAllNotifications(user.getUserId());
                assertNotNull(foundNotifications);
                assertTrue(foundNotifications.size() == 1);

                Notification foundNotification = foundNotifications.get(0);
                if ( groupId == null ) {
                    groupId = foundNotification.getGroupId();//First notification we read
                } else {
                    assertEquals(groupId, foundNotification.getGroupId());
                }
            }

            //Mark as read the notifications for just one user
            notificationAPI.markNotificationsAsRead(newUser1.getUserId());
            assertTrue(notificationAPI.getNewNotificationsCount(newUser1.getUserId()) == 0);//Should not have unread notifications
            assertTrue(notificationAPI.getNewNotificationsCount(newUser2.getUserId()) == 1);
            assertTrue(notificationAPI.getNewNotificationsCount(newUser3.getUserId()) == 1);

            //Delete the notification for just one user
            notificationAPI.deleteNotification(newUser2.getUserId(), groupId);
            assertTrue(notificationAPI.getNotificationsCount(newUser1.getUserId()) == 1);
            assertTrue(notificationAPI.getNotificationsCount(newUser2.getUserId()) == 0);
            assertTrue(notificationAPI.getNotificationsCount(newUser3.getUserId()) == 1);

            //And finally delete all for clean up
            notificationAPI.deleteNotifications(newUser1.getUserId());
            notificationAPI.deleteNotifications(newUser2.getUserId());
            notificationAPI.deleteNotifications(newUser3.getUserId());

            //Make sure everything is clean
            assertTrue(notificationAPI.getNotificationsCount(newUser1.getUserId()) == 0);
            assertTrue(notificationAPI.getNotificationsCount(newUser2.getUserId()) == 0);
            assertTrue(notificationAPI.getNotificationsCount(newUser3.getUserId()) == 0);
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