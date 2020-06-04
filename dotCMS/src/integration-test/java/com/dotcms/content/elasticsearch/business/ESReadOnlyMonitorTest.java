package com.dotcms.content.elasticsearch.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ESReadOnlyMonitorTest {

    private static ESReadOnlyMonitor esReadOnlyMonitor;
    private SystemMessageEventUtil systemMessageEventUtilMock;
    private RoleAPI roleAPIMock;

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void init () {
        systemMessageEventUtilMock = mock(SystemMessageEventUtil.class);
        roleAPIMock = mock(RoleAPI.class);

        esReadOnlyMonitor = ESReadOnlyMonitor.getInstance(systemMessageEventUtilMock, roleAPIMock);
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(ReindexEntry, String)}
     * When: If the LIVE and WORKING current index are not read only
     * Should: not sent large message
     *
     * @throws DotDataException
     */
    @Test
    public void shouldNotSendLargeMessage() throws DotDataException {
        final ReindexEntry reindexEntry = mock(ReindexEntry.class);
        final String cause = "cause";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        putReadOnly(indiciesInfo.getWorking(), false);
        putReadOnly(indiciesInfo.getLive(), false);

        esReadOnlyMonitor.start(reindexEntry, cause);

        verify(systemMessageEventUtilMock, never()).pushLargeMessage(any(), any());

        assertEquals(false, ESIndexUtil.isReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(ReindexEntry, String)}
     * When: If any of LIVE and WORKING current index are read only
     * Should: sent large message
     *
     * @throws DotDataException
     */
    @Test
    public void shouldSendLargeMessage() throws DotDataException, DotSecurityException, InterruptedException {
        final ReindexEntry reindexEntry = mock(ReindexEntry.class);
        final String cause = "cause";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            putReadOnly(indiciesInfo.getWorking(), true);
            putReadOnly(indiciesInfo.getLive(), false);

            esReadOnlyMonitor.start(reindexEntry, cause);

            Thread.sleep(100);

            checkLargeMessageSent(indiciesInfo, user);
            assertEquals(false, ESIndexUtil.isReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(ReindexEntry, String)}
     * When: If call start again after the first call is finished
     * Should: should sent the message again
     *
     * @throws DotDataException
     */
    @Test
    public void shouldSendLargeMessageTwice() throws DotDataException, DotSecurityException, InterruptedException {
        final ReindexEntry reindexEntry = mock(ReindexEntry.class);
        final String cause = "cause";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            putReadOnly(indiciesInfo.getWorking(), true);
            putReadOnly(indiciesInfo.getLive(), false);

            esReadOnlyMonitor.start(reindexEntry, cause);
            checkLargeMessageSent(indiciesInfo, user);

            esReadOnlyMonitor.start(reindexEntry, cause);
            checkLargeMessageSent(indiciesInfo, user);
            assertEquals(false, ESIndexUtil.isReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    private void checkLargeMessageSent(IndiciesInfo indiciesInfo, User user) throws InterruptedException {
        Thread.sleep(100);

        final SystemMessageBuilder messageReadonly = new SystemMessageBuilder()
                .setMessage("At least one of the Elasticsearch current indices are in read only mode")
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock).pushMessage(
                messageReadonly.create(),
                list(user.getUserId())
        );

        final SystemMessageBuilder messageWriteModeAgain = new SystemMessageBuilder()
                .setMessage("Elasticsearch current indices are in write mode again")
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock).pushMessage(
                messageWriteModeAgain.create(),
                list(user.getUserId())
        );
    }

    private static AcknowledgedResponse putReadOnly(final String indexName, final boolean value) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(indexName);

        final Settings.Builder settingBuilder = Settings.builder()
                .put("index.blocks.read_only_allow_delete", value)
                .put("index.blocks.read_only", value);

        request.settings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient().indices()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }
}
