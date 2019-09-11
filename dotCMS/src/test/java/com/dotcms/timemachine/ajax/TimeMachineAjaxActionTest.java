package com.dotcms.timemachine.ajax;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.util.Config;
import java.util.Locale;
import javax.servlet.ServletContext;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test for {@link TimeMachineAjaxAction}
 * @author jsanca
 */
public class TimeMachineAjaxActionTest extends UnitTestBase {

    private boolean testGenerateNotification = false;

    @Test()
    public void testGenerateNotification() throws Exception {

        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final WebResource webResource = mock(WebResource.class);

        final TimeMachineAjaxAction machineAjaxAction =
                new TimeMachineAjaxAction(notificationAPI, webResource);
        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("US").build();

        this.initMessages();
        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotification = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                new I18NMessage("notification.timemachine.created.info.title"),
                new I18NMessage("TIMEMACHINE-SNAPSHOT-CREATED"),
                null,
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                "admin@dotcms.com",
                locale
        );

        machineAjaxAction.generateNotification
                (locale,
                        "admin@dotcms.com");


        assertTrue(this.testGenerateNotification);
    }
}
