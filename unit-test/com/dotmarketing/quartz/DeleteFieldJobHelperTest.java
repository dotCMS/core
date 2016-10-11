package com.dotmarketing.quartz;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.quartz.job.DeleteFieldJobHelper;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
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

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotificationStartDeleting = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                new I18NMessage("notification.deletefieldjob.delete.info.title"),
                new I18NMessage(
                        "notification.deletefieldjob.startdelete.info.message", null,
                        "velocityVar", "iFieldNode1", "iStructureNode1"),
                null,
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                "admin@dotcms.com",
                Locale.US
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

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotificationEndDeleting = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                new I18NMessage("notification.deletefieldjob.delete.info.title"),
                new I18NMessage(
                        "notification.deletefieldjob.enddelete.info.message", null,
                        "velocityVar", "iFieldNode1", "iStructureNode1"),
                null,
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                "admin@dotcms.com",
                Locale.US
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

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotificationUnableDeleting = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                new I18NMessage("notification.deletefieldjob.delete.info.title"),
                new I18NMessage(
                        "notification.deletefieldjob.unabledelete.info.message", null,
                        "velocityVar", "iFieldNode1", "iStructureNode1"),
                null,
                NotificationLevel.ERROR,
                NotificationType.GENERIC,
                "admin@dotcms.com",
                Locale.US
        );

        deleteFieldJobHelper.generateNotificationUnableDelete
                (notificationAPI, new Locale.Builder().setLanguage("en").setRegion("US").build(),
                        "admin@dotcms.com", "velocityVar", "iFieldNode1", "iStructureNode1");


        assertTrue(this.testGenerateNotificationUnableDeleting);
    }
}
