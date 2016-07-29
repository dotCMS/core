package com.dotmarketing.common.reindex;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.timemachine.ajax.TimeMachineAjaxAction;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ReindexThread}
 * @author jsanca
 */
public class ReindexThreadTest extends BaseMessageResources {

    private boolean testGenerateNotification = false;

    @Test()
    public void testGenerateNotification() throws Exception {

        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final DistributedJournalAPI<String> jAPI = mock(DistributedJournalAPI.class);

        final ReindexThread reindexThread =
                new ReindexThread(jAPI, notificationAPI);

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
                "Reindex Notification",
                "Could not re-index record with the Identifier \"index1\". It is in a bad state or is associated to orphaned records. You can try running the Fix Assets Inconsistencies tool and restart the re-index.",
                null,
                NotificationLevel.ERROR,
                NotificationType.GENERIC,
                "admin@dotcms.com"
        );

        final String identToIndex = "index1";
        final String msg = "Could not re-index record with the Identifier '"
                + identToIndex
                + "'. The record is in a bad state or can be associated to orphaned records. You can try running the Fix Assets Inconsistencies tool and re-start the reindex.";
        final User user = new User();
        user.setLocale(new Locale.Builder().setLanguage("en").setRegion("US").build());
        user.setUserId("admin@dotcms.com");

        reindexThread.sendNotification
                ("notification.reindexing.error.processrecord",
                        new Object[] { identToIndex }, msg, user);


        assertTrue(this.testGenerateNotification);
    }
}
