package com.dotmarketing.common.reindex;

import com.dotcms.UnitTestBase;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.api.system.event.PayloadVerifierFactory;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.RoleVerifier;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ReindexThread}
 * @author jsanca
 */
public class ReindexThreadUnitTest extends UnitTestBase {

    private boolean testGenerateNotification = false;
    private PayloadVerifier originalRoleVerifier;

    @Test()
    public void testGenerateNotification() throws Exception {

        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final ReindexQueueAPI jAPI = mock(ReindexQueueAPI.class);
        final ContentletIndexAPI indexApi = mock(ContentletIndexAPI.class);
        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("US").build();

        String cmsAdminRoleId = UUID.randomUUID().toString();

        //Getting the original version of the Verifier in order to restore it after the test
        PayloadVerifierFactory payloadVerifierFactory = PayloadVerifierFactory.getInstance();
        Payload payload = new Payload(Visibility.ROLE, cmsAdminRoleId);
        this.originalRoleVerifier = payloadVerifierFactory.getVerifier(payload);

        //Mocking the Notification Visibility
        PayloadVerifier roleVerifier = new RoleVerifier(roleAPI);
        payloadVerifierFactory.register(Visibility.ROLE, roleVerifier);

        final ReindexThread reindexThread = new ReindexThread(jAPI, notificationAPI, userAPI, roleAPI, indexApi);
        final String identToIndex = "index1";
        final String msg = "Could not re-index record with the Identifier '"
                + identToIndex
                + "'. The record is in a bad state or can be associated to orphaned records. You can try running the Fix Assets Inconsistencies tool and re-start the reindex.";

        //Mock the system user
        final User user = new User();
        user.setLocale(locale);
        user.setUserId("admin@dotcms.com");
        when(userAPI.getSystemUser()).thenReturn(user);

        //Mock the CMS Admin Role
        final Role cmsAdminRole = new Role();
        cmsAdminRole.setId(cmsAdminRoleId);
        cmsAdminRole.setName("CMS Administrator");
        cmsAdminRole.setRoleKey("CMS Administrator");
        when(roleAPI.loadCMSAdminRole()).thenReturn(cmsAdminRole);

        this.initMessages();
        Config.CONTEXT = context;

        when(context.getInitParameter(WebKeys.COMPANY_ID)).thenReturn(RestUtilTest.DEFAULT_COMPANY);

        doAnswer(new Answer<Void>() { // if this method is called, should fail

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                testGenerateNotification = true;
                return null;
            }
        }).when(notificationAPI).generateNotification(
                new I18NMessage("notification.reindex.error.title"),
                new I18NMessage("notification.reindexing.error.processrecord", msg, identToIndex),
                null,
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                Visibility.ROLE,
                cmsAdminRoleId,
                user.getUserId(),
                locale
        );

        //Execute the notification call
        reindexThread.sendNotification
                ("notification.reindexing.error.processrecord",
                        new Object[] {identToIndex}, msg, false);

        //Validate
        assertTrue(this.testGenerateNotification);
    }

    @After
    public void restore() {

        //Restore the original version of the Verifier
        PayloadVerifierFactory payloadVerifierFactory = PayloadVerifierFactory.getInstance();
        payloadVerifierFactory.register(Visibility.ROLE, this.originalRoleVerifier);

    }
    
    
    

}