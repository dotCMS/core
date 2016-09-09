package com.dotmarketing.quartz;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotmarketing.quartz.job.DeleteFieldJobHelper;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test for {@link com.dotmarketing.quartz.job.DeleteFieldJobHelper}
 * @author jsanca
 */
public class DeleteFieldJobHelperTest extends BaseMessageResources {

    private boolean testGenerateNotificationStartDeleting  = false;
    private boolean testGenerateNotificationEndDeleting    = false;
    private boolean testGenerateNotificationUnableDeleting = false;

    @Test
    public void testGenerateNotificationStartDeleting() throws Exception {

        final DeleteFieldJobHelper deleteFieldJobHelper =
                DeleteFieldJobHelper.INSTANCE;
        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);

        this.initMessages();
        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotificationStartDeleting = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                "Delete Field",
                "Deletion of Field velocityVar has been started. Field Inode: iFieldNode1, Structure Inode: iStructureNode1",
                null,
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                "admin@dotcms.com"
                );

        deleteFieldJobHelper.generateNotificationStartDeleting
                (notificationAPI, new Locale.Builder().setLanguage("en").setRegion("US").build(),
                        "admin@dotcms.com", "velocityVar", "iFieldNode1", "iStructureNode1");


        assertTrue(this.testGenerateNotificationStartDeleting);
    }

    @Test
    public void testGenerateNotificationEndDeleting() throws Exception {

        final DeleteFieldJobHelper deleteFieldJobHelper =
                DeleteFieldJobHelper.INSTANCE;
        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);

        this.initMessages();
        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotificationEndDeleting = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                "Delete Field",
                "Field velocityVar was deleted succesfully. Field Inode: iFieldNode1, Structure Inode: iStructureNode1",
                null,
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                "admin@dotcms.com"
        );

        deleteFieldJobHelper.generateNotificationEndDeleting
                (notificationAPI, new Locale.Builder().setLanguage("en").setRegion("US").build(),
                        "admin@dotcms.com", "velocityVar", "iFieldNode1", "iStructureNode1");


        assertTrue(this.testGenerateNotificationEndDeleting);
    }

    @Test
    public void testGenerateNotificationUnableDeleting() throws Exception {

        final DeleteFieldJobHelper deleteFieldJobHelper =
                DeleteFieldJobHelper.INSTANCE;
        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);

        this.initMessages();
        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotificationUnableDeleting = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                "Delete Field",
                "Unable to delete field velocityVar. Field Inode: iFieldNode1, Structure Inode: iStructureNode1",
                null,
                NotificationLevel.ERROR,
                NotificationType.GENERIC,
                "admin@dotcms.com"
        );

        deleteFieldJobHelper.generateNotificationUnableDelete
                (notificationAPI, new Locale.Builder().setLanguage("en").setRegion("US").build(),
                        "admin@dotcms.com", "velocityVar", "iFieldNode1", "iStructureNode1");


        assertTrue(this.testGenerateNotificationUnableDeleting);
    }
}
