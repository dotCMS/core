package com.dotcms.analytics.listener;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContentAnalyticsAppListener}.
 *
 * <p>Constructs the listener via its package-private {@code (HostAPI)} constructor to avoid the
 * {@code APILocator} singleton chain triggered by the no-arg constructor. Static dependencies
 * ({@link Config}, {@link APILocator}, {@link SystemMessageEventUtil}) are mocked with
 * {@link MockedStatic}.
 *
 * <p>The HTTP success path is intentionally not exercised here — that requires building out a
 * full {@code CircuitBreakerUrl} builder-chain mock, which is better suited to an integration
 * test. The failure-path tests below cover the critical "clear password on failure" invariant.
 */
public class ContentAnalyticsAppListenerTest {

    private static final String HOST_ID = "site-123";
    private static final String USER_ID = "admin@dotcms.com";

    private ContentAnalyticsAppListener listenerWith(final HostAPI hostAPI) {
        return new ContentAnalyticsAppListener(hostAPI);
    }

    private AppSecretSavedEvent mockEvent(final String hostId, final Map<String, Secret> secrets) {
        final AppSecrets appSecrets = mock(AppSecrets.class);
        when(appSecrets.getSecrets()).thenReturn(secrets);
        final AppSecretSavedEvent event = mock(AppSecretSavedEvent.class);
        when(event.getHostIdentifier()).thenReturn(hostId);
        when(event.getUserId()).thenReturn(USER_ID);
        when(event.getAppSecrets()).thenReturn(appSecrets);
        return event;
    }

    private Secret stringSecret(final String value) {
        return Secret.builder()
                .withValue(value)
                .withHidden(true)
                .withType(Type.STRING)
                .build();
    }

    @Test
    public void notify_nullEvent_doesNotThrow() throws Exception {
        final ContentAnalyticsAppListener listener = listenerWith(mock(HostAPI.class));
        assertDoesNotThrow(() -> listener.notify(null));
    }

    @Test
    public void notify_missingHostIdentifier_doesNothing() throws Exception {
        final HostAPI hostAPI = mock(HostAPI.class);
        final ContentAnalyticsAppListener listener = listenerWith(hostAPI);
        final AppSecretSavedEvent event = mockEvent("  ", new HashMap<>());
        listener.notify(event);
        verify(hostAPI, never()).find(anyString(), any(User.class), anyBoolean());
    }

    @Test
    public void notify_passwordNotSet_skipsExchange() throws Exception {
        final HostAPI hostAPI = mock(HostAPI.class);
        final ContentAnalyticsAppListener listener = listenerWith(hostAPI);

        final Map<String, Secret> secrets = new HashMap<>();
        secrets.put("bearerToken", stringSecret("existing-token"));
        final AppSecretSavedEvent event = mockEvent(HOST_ID, secrets);

        listener.notify(event);

        // Re-entrancy guard: no host lookup, no exchange, no save.
        verify(hostAPI, never()).find(anyString(), any(User.class), anyBoolean());
    }

    @Test
    public void notify_tenantConfigMissing_clearsPasswordAndNotifies() throws Exception {
        final HostAPI hostAPI = mock(HostAPI.class);
        final AppsAPI appsAPI = mock(AppsAPI.class);
        final SystemMessageEventUtil msgUtil = mock(SystemMessageEventUtil.class);
        final User systemUser = mock(User.class);
        final Host host = mock(Host.class);
        when(hostAPI.find(eq(HOST_ID), any(User.class), anyBoolean())).thenReturn(host);

        try (MockedStatic<Config> configMock = mockStatic(Config.class);
             MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class);
             MockedStatic<SystemMessageEventUtil> sysMsgMock =
                     mockStatic(SystemMessageEventUtil.class)) {

            configMock.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_TENANT"), anyString()))
                    .thenReturn("");
            configMock.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_BASE_URL"), anyString()))
                    .thenReturn("http://event-manager:8080");
            apiLocatorMock.when(APILocator::systemUser).thenReturn(systemUser);
            apiLocatorMock.when(APILocator::getAppsAPI).thenReturn(appsAPI);
            sysMsgMock.when(SystemMessageEventUtil::getInstance).thenReturn(msgUtil);

            final ContentAnalyticsAppListener listener = listenerWith(hostAPI);

            final Map<String, Secret> secrets = new HashMap<>();
            secrets.put("adminUser", stringSecret("admin"));
            secrets.put("adminPassword", stringSecret("admin"));
            secrets.put("siteAuth", stringSecret("auth-key"));
            final AppSecretSavedEvent event = mockEvent(HOST_ID, secrets);

            listener.notify(event);

            // Both credentials cleared via saveSecrets; siteAuth preserved.
            final ArgumentCaptor<AppSecrets> savedCaptor = ArgumentCaptor.forClass(AppSecrets.class);
            verify(appsAPI, times(1)).saveSecrets(savedCaptor.capture(), eq(host), eq(systemUser));
            final Map<String, Secret> saved = savedCaptor.getValue().getSecrets();
            assertEquals(false, saved.containsKey("adminPassword"),
                    "adminPassword must be cleared on failure");
            assertEquals(false, saved.containsKey("adminUser"),
                    "adminUser must be cleared on failure — same lifecycle as the password");
            assertEquals(true, saved.containsKey("siteAuth"),
                    "unrelated secrets must be preserved");

            // User notified.
            verify(msgUtil, times(1)).pushMessage(any(), anyList());
        }
    }

    @Test
    public void notify_adminUserMissing_clearsPasswordAndNotifies() throws Exception {
        // The user typed a password but no admin username. Listener should fail-fast,
        // clear the password (don't retain typed credentials on a failed save), and notify.
        final HostAPI hostAPI = mock(HostAPI.class);
        final AppsAPI appsAPI = mock(AppsAPI.class);
        final SystemMessageEventUtil msgUtil = mock(SystemMessageEventUtil.class);
        final User systemUser = mock(User.class);
        final Host host = mock(Host.class);
        when(hostAPI.find(eq(HOST_ID), any(User.class), anyBoolean())).thenReturn(host);

        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class);
             MockedStatic<SystemMessageEventUtil> sysMsgMock =
                     mockStatic(SystemMessageEventUtil.class)) {
            apiLocatorMock.when(APILocator::systemUser).thenReturn(systemUser);
            apiLocatorMock.when(APILocator::getAppsAPI).thenReturn(appsAPI);
            sysMsgMock.when(SystemMessageEventUtil::getInstance).thenReturn(msgUtil);

            final ContentAnalyticsAppListener listener = listenerWith(hostAPI);

            final Map<String, Secret> secrets = new HashMap<>();
            // No adminUser entry — just the password.
            secrets.put("adminPassword", stringSecret("typed-password"));
            secrets.put("siteAuth", stringSecret("auth-key"));
            final AppSecretSavedEvent event = mockEvent(HOST_ID, secrets);

            listener.notify(event);

            final ArgumentCaptor<AppSecrets> savedCaptor = ArgumentCaptor.forClass(AppSecrets.class);
            verify(appsAPI, times(1)).saveSecrets(savedCaptor.capture(), eq(host), eq(systemUser));
            assertEquals(false, savedCaptor.getValue().getSecrets().containsKey("adminPassword"),
                    "adminPassword must be cleared even when adminUser is missing");
            verify(msgUtil, times(1)).pushMessage(any(), anyList());
        }
    }

    @Test
    public void notify_baseUrlConfigMissing_clearsPasswordAndNotifies() throws Exception {
        final HostAPI hostAPI = mock(HostAPI.class);
        final AppsAPI appsAPI = mock(AppsAPI.class);
        final SystemMessageEventUtil msgUtil = mock(SystemMessageEventUtil.class);
        final User systemUser = mock(User.class);
        final Host host = mock(Host.class);
        when(hostAPI.find(eq(HOST_ID), any(User.class), anyBoolean())).thenReturn(host);

        try (MockedStatic<Config> configMock = mockStatic(Config.class);
             MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class);
             MockedStatic<SystemMessageEventUtil> sysMsgMock =
                     mockStatic(SystemMessageEventUtil.class)) {

            configMock.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_TENANT"), anyString()))
                    .thenReturn("cust-001");
            configMock.when(() -> Config.getStringProperty(eq("DOT_ANALYTICS_BASE_URL"), anyString()))
                    .thenReturn("");
            apiLocatorMock.when(APILocator::systemUser).thenReturn(systemUser);
            apiLocatorMock.when(APILocator::getAppsAPI).thenReturn(appsAPI);
            sysMsgMock.when(SystemMessageEventUtil::getInstance).thenReturn(msgUtil);

            final ContentAnalyticsAppListener listener = listenerWith(hostAPI);

            final Map<String, Secret> secrets = new HashMap<>();
            secrets.put("adminUser", stringSecret("admin"));
            secrets.put("adminPassword", stringSecret("admin"));
            final AppSecretSavedEvent event = mockEvent(HOST_ID, secrets);

            listener.notify(event);

            final ArgumentCaptor<AppSecrets> savedCaptor = ArgumentCaptor.forClass(AppSecrets.class);
            verify(appsAPI, times(1)).saveSecrets(savedCaptor.capture(), eq(host), eq(systemUser));
            assertEquals(false, savedCaptor.getValue().getSecrets().containsKey("adminPassword"));
            assertEquals(false, savedCaptor.getValue().getSecrets().containsKey("adminUser"),
                    "adminUser must be cleared on config-missing failure");
            verify(msgUtil, times(1)).pushMessage(any(), anyList());
        }
    }

    @Test
    public void persistTokenAndClearCredentials_replacesBearerAndDropsPassword() throws Exception {
        final HostAPI hostAPI = mock(HostAPI.class);
        final AppsAPI appsAPI = mock(AppsAPI.class);
        final User systemUser = mock(User.class);
        final Host host = mock(Host.class);
        when(hostAPI.find(eq(HOST_ID), any(User.class), anyBoolean())).thenReturn(host);

        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class)) {
            apiLocatorMock.when(APILocator::systemUser).thenReturn(systemUser);
            apiLocatorMock.when(APILocator::getAppsAPI).thenReturn(appsAPI);

            final ContentAnalyticsAppListener listener = listenerWith(hostAPI);

            // Pre-save state: adminUser + adminPassword set, a stale bearerToken present, and
            // an unrelated secret (siteAuth) that the save flow must preserve verbatim.
            final Map<String, Secret> currentSecrets = new HashMap<>();
            currentSecrets.put("adminUser", stringSecret("admin"));
            currentSecrets.put("adminPassword", stringSecret("typed-by-user"));
            currentSecrets.put("bearerToken", stringSecret("stale-token"));
            currentSecrets.put("siteAuth", stringSecret("auth-key-abc"));

            listener.persistTokenAndClearCredentials(HOST_ID, USER_ID, currentSecrets, "new-token-xyz");

            final ArgumentCaptor<AppSecrets> savedCaptor = ArgumentCaptor.forClass(AppSecrets.class);
            verify(appsAPI, times(1)).saveSecrets(savedCaptor.capture(), eq(host), eq(systemUser));

            final Map<String, Secret> saved = savedCaptor.getValue().getSecrets();
            assertEquals(false, saved.containsKey("adminPassword"),
                    "adminPassword must NOT be retained after token exchange");
            assertEquals(false, saved.containsKey("adminUser"),
                    "adminUser must NOT be retained after token exchange — same lifecycle as the password");
            assertEquals("new-token-xyz", saved.get("bearerToken").getString(),
                    "bearerToken must be replaced with the freshly exchanged token");
            assertEquals("auth-key-abc", saved.get("siteAuth").getString(),
                    "unrelated secrets must round-trip unchanged");
        }
    }

    @Test
    public void persistTokenAndClearCredentials_failure_attemptsCredentialCleanup() throws Exception {
        // Regression: if the token-persist saveSecrets throws (DB transient failure, validation
        // error, etc.), the listener must still attempt to clear the user's adminUser AND
        // adminPassword. Without this, those credentials — written by the user's original save —
        // sit in encrypted storage indefinitely, since a re-save with an empty adminPassword
        // short-circuits on the "password not set" guard in notify().
        final HostAPI hostAPI = mock(HostAPI.class);
        final AppsAPI appsAPI = mock(AppsAPI.class);
        final SystemMessageEventUtil msgUtil = mock(SystemMessageEventUtil.class);
        final User systemUser = mock(User.class);
        final Host host = mock(Host.class);
        when(hostAPI.find(eq(HOST_ID), any(User.class), anyBoolean())).thenReturn(host);

        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class);
             MockedStatic<SystemMessageEventUtil> sysMsgMock =
                     mockStatic(SystemMessageEventUtil.class)) {
            apiLocatorMock.when(APILocator::systemUser).thenReturn(systemUser);
            apiLocatorMock.when(APILocator::getAppsAPI).thenReturn(appsAPI);
            sysMsgMock.when(SystemMessageEventUtil::getInstance).thenReturn(msgUtil);

            // First saveSecrets call (the token-persist) throws; second saveSecrets call (the
            // best-effort cleanup) succeeds. Order matters — Mockito doAnswer with thenAnswer
            // chained on the next call would do the same, but doThrow + doNothing is clearer.
            org.mockito.Mockito.doThrow(new RuntimeException("simulated persist failure"))
                    .doNothing()
                    .when(appsAPI)
                    .saveSecrets(any(AppSecrets.class), eq(host), eq(systemUser));

            final ContentAnalyticsAppListener listener = listenerWith(hostAPI);

            final Map<String, Secret> currentSecrets = new HashMap<>();
            currentSecrets.put("adminUser", stringSecret("admin"));
            currentSecrets.put("adminPassword", stringSecret("typed-by-user"));
            currentSecrets.put("siteAuth", stringSecret("auth-key-abc"));

            listener.persistTokenAndClearCredentials(HOST_ID, USER_ID, currentSecrets, "new-token");

            // Two saveSecrets invocations: the failing token-persist, then the cleanup.
            final ArgumentCaptor<AppSecrets> savedCaptor = ArgumentCaptor.forClass(AppSecrets.class);
            verify(appsAPI, times(2)).saveSecrets(savedCaptor.capture(), eq(host), eq(systemUser));

            final AppSecrets cleanupSave = savedCaptor.getAllValues().get(1);
            assertEquals(false, cleanupSave.getSecrets().containsKey("adminPassword"),
                    "cleanup save must drop adminPassword");
            assertEquals(false, cleanupSave.getSecrets().containsKey("adminUser"),
                    "cleanup save must drop adminUser — same lifecycle as the password");
            assertEquals("auth-key-abc", cleanupSave.getSecrets().get("siteAuth").getString(),
                    "cleanup save must preserve unrelated secrets");
            // User is notified of the original failure.
            verify(msgUtil, org.mockito.Mockito.atLeastOnce()).pushMessage(any(), anyList());
        }
    }

    @Test
    public void key_returnsContentAnalyticsAppKey() throws Exception {
        final ContentAnalyticsAppListener listener = listenerWith(mock(HostAPI.class));
        assertEquals(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY, listener.getKey());
    }
}
