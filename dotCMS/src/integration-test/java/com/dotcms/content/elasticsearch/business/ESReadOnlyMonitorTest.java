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
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
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
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If the LIVE and WORKING current index are not read only
     * Should: not sent large message
     *
     * @throws DotDataException
     */
    @Test
    public void shouldNotSendLargeMessage() throws DotDataException, IOException {
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        setReadOnly(indiciesInfo.getWorking(), false);
        setReadOnly(indiciesInfo.getLive(), false);

        esReadOnlyMonitor.start(message);

        verify(systemMessageEventUtilMock, never()).pushLargeMessage(any(), any());

        assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If any of LIVE and WORKING current index are read only
     * Should: sent large message
     *
     * @throws DotDataException
     */
    @Test
    public void shouldSendLargeMessage() throws DotDataException, DotSecurityException, InterruptedException, IOException {
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setReadOnly(indiciesInfo.getWorking(), true);
            setReadOnly(indiciesInfo.getLive(), false);

            esReadOnlyMonitor.start(message);

            Thread.sleep(100);

            checkLargeMessageSent(user);
            assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            setReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start()}
     * When: If the cluster is read only
     * Should: sent message
     *
     * @throws DotDataException
     */
    @Test
    public void shouldSendLargeMessageIfTheClusterIsInReadOnly() throws DotDataException, DotSecurityException, InterruptedException, IOException {
        final String message = "message";

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setClusterAsReadOnly(true);

            esReadOnlyMonitor.start(message);

            Thread.sleep(100);

            checkClusterLargeMessageSent(user);
            assertEquals(false, ElasticsearchUtil.isClusterInReadOnlyMode());
        } finally {
            setClusterAsReadOnly(false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If the cluster in not read only
     * Should: not sent large message
     *
     * @throws DotDataException
     */
    @Test
    public void shouldNotSendLargeMessageWhenClusterIsNotReadOnly() throws DotDataException, IOException {
        setClusterAsReadOnly(false);

        esReadOnlyMonitor.start();

        verify(systemMessageEventUtilMock, never()).pushLargeMessage(any(), any());

        assertEquals(false, ElasticsearchUtil.isClusterInReadOnlyMode());
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If call start again after the first call is finished
     * Should: should sent the message again
     *
     * @throws DotDataException
     */
    @Test
    public void shouldSendLargeMessageTwice() throws DotDataException, DotSecurityException, IOException, InterruptedException {
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setReadOnly(indiciesInfo.getWorking(), true);
            setReadOnly(indiciesInfo.getLive(), false);

            esReadOnlyMonitor.start(message);
            Thread.sleep(100);

            setReadOnly(indiciesInfo.getWorking(), true);

            esReadOnlyMonitor.start(message);
            Thread.sleep(100);
            checkLargeMessageSent(user, 2);
            assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            setReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If call start again before the first call is finished
     * Should: should sent the message just once
     *
     * @throws DotDataException
     */
    @Test
    public void shouldSendLargeMessageJustOnce() throws DotDataException, DotSecurityException, IOException, InterruptedException {
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setReadOnly(indiciesInfo.getWorking(), true);
            setReadOnly(indiciesInfo.getLive(), false);

            esReadOnlyMonitor.start(message);
            esReadOnlyMonitor.start(message);
            Thread.sleep(100);
            checkLargeMessageSent(user, 1);
            assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            setReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    private void checkLargeMessageSent(final User user) {
        checkLargeMessageSent(user, 1);
    }

    private void checkLargeMessageSent(final User user, final int times) {
        final SystemMessageBuilder messageReadonly = new SystemMessageBuilder()
                .setMessage("Either \"Live\" or \"Working\" indices are in read-only mode")
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock, times(times)).pushMessage(
                messageReadonly.create(),
                list(user.getUserId())
        );

        final SystemMessageBuilder messageWriteModeAgain = new SystemMessageBuilder()
                .setMessage("\"Live\" and \"Working\" indices are in write-mode again")
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock, times(times)).pushMessage(
                messageWriteModeAgain.create(),
                list(user.getUserId())
        );
    }

    private void checkClusterLargeMessageSent(final User user) {
        final SystemMessageBuilder messageReadonly = new SystemMessageBuilder()
                .setMessage("Elasticsearch cluster is in read-only mode")
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock).pushMessage(
                messageReadonly.create(),
                list(user.getUserId())
        );

        final SystemMessageBuilder messageWriteModeAgain = new SystemMessageBuilder()
                .setMessage("Elasticsearch cluster is in write-mode again")
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock).pushMessage(
                messageWriteModeAgain.create(),
                list(user.getUserId())
        );
    }

    private static AcknowledgedResponse setReadOnly(final String indexName, final boolean value) {
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

    private static AcknowledgedResponse setClusterAsReadOnly(final boolean value) {
        final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

        final Settings.Builder settingBuilder = Settings.builder()
                .put("cluster.blocks.read_only", value);

        request.persistentSettings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient()
                        .cluster()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }
}
