package com.dotcms.timemachine.ajax;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
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
 * Test for {@link TimeMachineAjaxAction}
 * @author jsanca
 */
public class TimeMachineAjaxActionTest extends BaseMessageResources {

    private boolean testGenerateNotification = false;

    @Test()
    public void testGenerateNotification() throws Exception {

        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final TimeMachineAjaxAction machineAjaxAction =
                new TimeMachineAjaxAction(notificationAPI);

        this.initMessages();
        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotification = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                "Reindex failed",
                "Time Machine Snapshot created.",
                null,
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                "admin@dotcms.com"
        );

        machineAjaxAction.generateNotification
                (new Locale.Builder().setLanguage("en").setRegion("US").build(),
                        "admin@dotcms.com");


        assertTrue(this.testGenerateNotification);
    }
}
