package com.dotcms.content.elasticsearch.business;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.RestUtilTest;
import com.dotmarketing.business.DotStateException;
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
 * Test for {@link ESContentletAPIHelper}
 * @author jsanca
 */
public class ESContentletAPIHelperTest extends BaseMessageResources {

    private boolean testGenerateNotificationStartDeleting = false;

    @Test(expected = DotStateException.class)
    public void testGenerateNotificationStartDeleting() throws Exception {

        final ESContentletAPIHelper esContentletAPIHelper =
                ESContentletAPIHelper.INSTANCE;
        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("US").build();

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
                "Contentlet Notification",
                "Contentlet with Inode iFieldNode1 cannot be deleted because it's not archived. Please archive it first before deleting it.",
                null,
                NotificationLevel.ERROR,
                NotificationType.GENERIC,
                "admin@dotcms.com",
                locale
        );

        esContentletAPIHelper.generateNotificationCanNotDelete
                (notificationAPI, locale,
                        "admin@dotcms.com", "iFieldNode1");


        assertTrue(this.testGenerateNotificationStartDeleting);
    }
}
