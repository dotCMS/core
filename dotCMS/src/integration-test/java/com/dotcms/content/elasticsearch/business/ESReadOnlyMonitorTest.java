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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(DataProviderRunner.class)
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

    @DataProvider
    public static Object[] clusterReadOnlyProperties() {
        return new String[]{"cluster.blocks.read_only", "cluster.blocks.read_only_allow_delete"};
    }

    @DataProvider
    public static Object[] indexReadOnlyProperties() {
        return new String[]{"index.blocks.read_only", "index.blocks.read_only_allow_delete"};
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If the LIVE and WORKING current index are not read only
     * Should: not sent large message
     *
     * @throws DotDataException
     */
    //@Test
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldNotSendLargeMessage(final String propertyName) throws DotDataException, IOException {
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        setReadOnly(indiciesInfo.getWorking(), propertyName, false);
        setReadOnly(indiciesInfo.getLive(), propertyName, false);

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
    //@Test
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldSendLargeMessage(final String propertyName) throws DotDataException, DotSecurityException, InterruptedException, IOException {
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setReadOnly(indiciesInfo.getWorking(), propertyName, true);
            setReadOnly(indiciesInfo.getLive(), propertyName, false);

            esReadOnlyMonitor.start(message);

            Thread.sleep(100);

            checkLargeMessageSent(user);
            assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            setReadOnly(indiciesInfo.getWorking(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start()}
     * When: If the cluster is read only
     * Should: sent message
     *
     * @throws DotDataException
     */
    //@Test
    @UseDataProvider("clusterReadOnlyProperties")
    public void shouldSendLargeMessageIfTheClusterIsInReadOnly(final String propertyName) throws DotDataException, DotSecurityException, InterruptedException, IOException {
        final String message = "message";

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setClusterAsReadOnly(propertyName, true);

            esReadOnlyMonitor.start(message);

            Thread.sleep(100);

            checkClusterLargeMessageSent(user);
            assertEquals(false, ElasticsearchUtil.isClusterInReadOnlyMode());
        } finally {
            setClusterAsReadOnly(propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If the cluster in not read only
     * Should: not sent large message
     *
     * @throws DotDataException
     */
    //@Test
    @UseDataProvider("clusterReadOnlyProperties")
    public void shouldNotSendLargeMessageWhenClusterIsNotReadOnly(final String propertyName) throws DotDataException, IOException {
        setClusterAsReadOnly(propertyName, false);

        esReadOnlyMonitor.start();

        verify(systemMessageEventUtilMock, never()).pushLargeMessage(any(), any());

        assertEquals(false, ElasticsearchUtil.isClusterInReadOnlyMode());
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If the read only if set to true again
     * Should: should set it to read only false again too
     *
     * @throws DotDataException
     */
    @Test
    public void shouldputReadonlyFalseAgain() throws DotDataException, DotSecurityException, IOException, InterruptedException {
        final String propertyName = "index.blocks.read_only";
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setReadOnly(indiciesInfo.getWorking(), propertyName, true);
            setReadOnly(indiciesInfo.getLive(), propertyName, false);

            esReadOnlyMonitor.start(message);
            Thread.sleep(100);

            setReadOnly(indiciesInfo.getWorking(), propertyName, true);

            Thread.sleep(ESReadOnlyMonitor.getInstance().timeToWaitAfterWriteModeSet + TimeUnit.MINUTES.toMillis(1));
            //checkLargeMessageSent(user, 1);
            assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            setReadOnly(indiciesInfo.getWorking(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If the start method is call a second time after finish
     * Should: send the messages twice
     *
     * @throws DotDataException
     */
    //@Test
    public void shouldSendLargeMessageTwice() throws DotDataException, DotSecurityException, IOException, InterruptedException {
        final String propertyName = "index.blocks.read_only";
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setReadOnly(indiciesInfo.getWorking(), propertyName, true);
            setReadOnly(indiciesInfo.getLive(), propertyName, false);

            final long timeToWait = ESReadOnlyMonitor.getInstance().timeToWaitAfterWriteModeSet + TimeUnit.MINUTES.toMillis(1);
            esReadOnlyMonitor.start(message);

            Thread.sleep(timeToWait);

            setReadOnly(indiciesInfo.getWorking(), propertyName, true);
            esReadOnlyMonitor.start(message);

            Thread.sleep(timeToWait);
            checkLargeMessageSent(user, 2);
            assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            setReadOnly(indiciesInfo.getWorking(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ESReadOnlyMonitor#start(String)}
     * When: If call start again before the first call is finished
     * Should: should sent the message just once
     *
     * @throws DotDataException
     */
    //@Test
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldSendLargeMessageJustOnce(final String propertyName) throws DotDataException, DotSecurityException, IOException, InterruptedException {
        final String message = "message";

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final Role adminRole = mock(Role.class);
        when(roleAPIMock.loadCMSAdminRole()).thenReturn(adminRole);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("1");

        when(roleAPIMock.findUsersForRole(adminRole)).thenReturn(list(user));

        try {
            setReadOnly(indiciesInfo.getWorking(), propertyName, true);
            setReadOnly(indiciesInfo.getLive(), propertyName, false);

            esReadOnlyMonitor.start(message);
            esReadOnlyMonitor.start(message);
            Thread.sleep(100);
            checkLargeMessageSent(user, 1);
            assertEquals(false, ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking(), indiciesInfo.getLive()));
        } finally {
            setReadOnly(indiciesInfo.getWorking(), propertyName, false);
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

    private void
    checkClusterLargeMessageSent(final User user) {
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

    private static AcknowledgedResponse setReadOnly(final String indexName, final String propertyName, final boolean value) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(indexName);

        final Settings.Builder settingBuilder = Settings.builder()
                .put(propertyName, value);

        request.settings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient().indices()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }

    private static AcknowledgedResponse setClusterAsReadOnly(final String propertyName, final boolean value) {
        final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

        final Settings.Builder settingBuilder = Settings.builder()
                .put(propertyName, value);

        request.persistentSettings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient()
                        .cluster()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }
}
