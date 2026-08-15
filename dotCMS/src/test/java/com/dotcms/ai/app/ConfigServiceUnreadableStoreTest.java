package com.dotcms.ai.app;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.SecretsStoreUnreadableException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers how {@link ConfigService} resolves dotAI configuration when the App secrets store cannot be
 * read, and — just as importantly — what it must <em>not</em> swallow while doing so.
 *
 * <p>Since issue #36724 an unreadable store raises where it previously wiped itself and returned
 * nothing, so this lookup has to degrade to "the app is unconfigured" on that condition alone.
 *
 * <p>The second half of that contract is the one that broke. An earlier revision of this PR degraded
 * <em>every</em> failure by replacing {@code Try.of(...).get()} with
 * {@code getOrElse(Optional.empty())}, on the mistaken reading that the {@code Try} was inert.
 * {@code Try.get()} rethrows, and that was what carried {@link InvalidLicenseException} out to the
 * caller; swallowing it turned an unlicensed instance into a silent "dotAI is unconfigured". CI
 * caught it in {@code ConfigServiceTest.test_invalidLicense}, an integration test. These unit cases
 * pin both halves at unit speed so the same mistake cannot be reintroduced without a fast failure.
 *
 * <p>The store failure is thrown in its <em>wrapped</em> production shape,
 * {@code DotRuntimeException(SecretsStoreUnreadableException(...))} — the intermediate layers re-wrap
 * it, so a test asserting against the unwrapped type would pass while production still returned 500.
 */
public class ConfigServiceUnreadableStoreTest {

    private static final String HOST_NAME = "demo.dotcms.com";

    private MockedStatic<APILocator> apiLocator;
    private AppsAPI appsAPI;
    private ConfigService configService;
    private Host host;

    /** The shape production actually produces: re-wrapped as a bare DotRuntimeException. */
    private static DotRuntimeException wrappedStoreFailure() {
        return new DotRuntimeException(new SecretsStoreUnreadableException(
                "cannot read dotSecretsStore.p12", new IOException("wrong password")));
    }

    @Before
    public void setUp() {
        appsAPI = mock(AppsAPI.class);

        host = mock(Host.class);
        when(host.getHostname()).thenReturn(HOST_NAME);
        // System host short-circuits the SYSTEM_HOST fallback lookup in config(), keeping each case
        // to a single call against the mocked AppsAPI.
        when(host.isSystemHost()).thenReturn(true);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        apiLocator.when(APILocator::systemUser).thenReturn(mock(User.class));
        apiLocator.when(APILocator::systemHost).thenReturn(host);

        // Constructed only AFTER APILocator is stubbed, and the order matters more than it looks:
        // ConfigService's static INSTANCE field runs on first touch of the class and calls the
        // no-arg constructor, which resolves the real AppsAPI -> AppsAPIImpl -> DataSource. With no
        // database present that reaches DbConnectionFactory, whose SYSTEM_EXIT_ON_STARTUP_FAILURE
        // path (default true) calls System.exit and takes the surefire fork down with it -- the run
        // then reports "Tests run: 0" rather than a test failure. Stubbing getAppsAPI() first keeps
        // the static initializer away from the database.
        configService = new ConfigService(appsAPI);
    }

    @After
    public void tearDown() {
        apiLocator.close();
    }

    private void storeRaises(final Throwable toThrow) throws Exception {
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenThrow(toThrow);
    }

    /**
     * Given an unreadable store, when dotAI resolves its config, then it reports the app as
     * unconfigured rather than propagating.
     */
    @Test
    public void unreadableStore_reportsAppAsUnconfigured() throws Exception {
        storeRaises(wrappedStoreFailure());

        final AppConfig config = configService.config(host);

        assertEquals(HOST_NAME, config.getHost());
        assertNull("No secrets should have been resolved from an unreadable store",
                config.getApiKey());
    }

    /**
     * The regression CI caught. An unlicensed instance must still fail loudly: degrading it would
     * report dotAI as merely unconfigured and hide the licensing problem.
     */
    @Test
    public void invalidLicense_stillPropagates() throws Exception {
        storeRaises(new InvalidLicenseException("valid license required"));

        assertThrows(InvalidLicenseException.class, () -> configService.config(host));
    }

    /**
     * An unrelated infrastructure failure (a database blip surfaces as a plain DotRuntimeException
     * too) must propagate — degrading it would hide a real outage behind "unconfigured".
     */
    @Test
    public void unrelatedRuntimeFailure_isNotMasked() throws Exception {
        storeRaises(new DotRuntimeException("the database went away"));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> configService.config(host));
        assertEquals("the database went away", thrown.getMessage());
    }

    /**
     * Guards the happy path against an over-broad catch: a readable store still yields its secrets.
     */
    @Test
    public void readableStore_returnsSecrets() throws Exception {
        final AppSecrets secrets = new AppSecrets.Builder()
                .withKey(AppKeys.APP_KEY)
                .withHiddenSecret(AppKeys.API_KEY.key, "a-real-key")
                .build();
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.of(secrets));

        assertEquals("a-real-key", configService.config(host).getApiKey());
    }
}
