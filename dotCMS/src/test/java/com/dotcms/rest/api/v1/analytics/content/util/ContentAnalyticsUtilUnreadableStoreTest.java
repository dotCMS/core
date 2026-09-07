package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.SecretsStoreUnreadableException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers how {@link ContentAnalyticsUtil} behaves when the App secrets store cannot be read.
 *
 * <p>Context: before issue #36724 an unreadable store silently recreated itself and returned no
 * secrets, so these lookups degraded by accident. Since the fix it raises instead, and these methods
 * have to degrade on purpose -- {@code getBearerTokenFromAppSecrets} feeds
 * {@code EventAnalyticsProxyHelper.buildAuthHeader()}, which is invoked outside {@code proxy()}'s try
 * block, so anything escaping here becomes a 500 on every analytics proxy and ingest request.
 *
 * <p>The exception never arrives as a {@link SecretsStoreUnreadableException}: several
 * {@code catch (Exception e) { throw new DotRuntimeException(e); }} layers sit between the store and
 * this class, so what actually reaches the catch is a bare {@link DotRuntimeException} with the real
 * cause underneath. These tests therefore throw the <em>wrapped</em> shape, which is what production
 * produces -- asserting against an unwrapped exception would pass while the production path still
 * returned 500. The existing {@code SiteAuthValidatorTest} and {@code EventAnalyticsProxyHelperTest}
 * both {@code mockStatic(ContentAnalyticsUtil.class)}, so neither could observe this class's own
 * exception handling; that is the gap these tests close.
 */
public class ContentAnalyticsUtilUnreadableStoreTest {

    private MockedStatic<APILocator> apiLocator;
    private AppsAPI appsAPI;
    private Host site;

    @BeforeEach
    public void setUp() {
        appsAPI = mock(AppsAPI.class);
        site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-id-1");

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        apiLocator.when(APILocator::systemUser).thenReturn(mock(User.class));
    }

    @AfterEach
    public void tearDown() {
        apiLocator.close();
    }

    private void storeRaises(final Throwable toThrow) throws Exception {
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenThrow(toThrow);
    }

    /**
     * The production shape: an unreadable store re-wrapped by the intermediate layers.
     * Expected result: an empty map, not a propagated exception.
     */
    @Test
    public void getAppSecrets_unreadableStoreWrappedByIntermediateLayers_degradesToEmptyMap()
            throws Exception {
        storeRaises(new DotRuntimeException(new SecretsStoreUnreadableException(
                "cannot read dotSecretsStore.p12", new IOException("wrong password"))));

        final Map<String, com.dotcms.security.apps.Secret> secrets =
                ContentAnalyticsUtil.getAppSecrets(site);

        assertTrue(secrets.isEmpty(), "An unreadable store must degrade to no secrets, not raise");
    }

    /**
     * An unrelated infrastructure failure (a database blip surfaces as a plain
     * DotRuntimeException too). Expected result: it propagates -- degrading it into "no secrets
     * configured" would hide real outages behind an unconfigured-app response.
     */
    @Test
    public void getAppSecrets_unrelatedRuntimeFailure_isNotMasked() throws Exception {
        storeRaises(new DotRuntimeException("the database went away"));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> ContentAnalyticsUtil.getAppSecrets(site));
        assertEquals("the database went away", thrown.getMessage());
    }

    /**
     * The path that produced the 500: {@code buildAuthHeader()} asks for the bearer token before the
     * proxy's try block opens. Expected result: an empty token, letting the caller fall back.
     */
    @Test
    public void getBearerTokenFromAppSecrets_unreadableStore_degradesToEmpty() throws Exception {
        storeRaises(new DotRuntimeException(new SecretsStoreUnreadableException(
                "cannot read dotSecretsStore.p12", new IOException("wrong password"))));

        final Optional<String> token = ContentAnalyticsUtil.getBearerTokenFromAppSecrets(site);

        assertTrue(token.isEmpty(), "An unreadable store must yield no token, not raise");
    }

    @Test
    public void getSiteKeyFromAppSecrets_unreadableStore_degradesToEmpty() throws Exception {
        storeRaises(new DotRuntimeException(new SecretsStoreUnreadableException(
                "cannot read dotSecretsStore.p12", new IOException("wrong password"))));

        final Optional<String> siteKey = ContentAnalyticsUtil.getSiteKeyFromAppSecrets(site);

        assertTrue(siteKey.isEmpty(), "An unreadable store must yield no site key, not raise");
    }

    @Test
    public void getSiteKeyFromAppSecrets_unrelatedRuntimeFailure_isNotMasked() throws Exception {
        storeRaises(new DotRuntimeException("the database went away"));

        assertThrows(DotRuntimeException.class,
                () -> ContentAnalyticsUtil.getSiteKeyFromAppSecrets(site));
    }

    /**
     * Guards the happy path against over-broad catching: a readable store still returns its secrets.
     */
    @Test
    public void getAppSecrets_readableStore_returnsSecrets() throws Exception {
        final AppSecrets appSecrets = new AppSecrets.Builder()
                .withKey("dotAnalytics")
                .withHiddenSecret("bearerToken", "a-token")
                .build();
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.of(appSecrets));

        assertTrue(ContentAnalyticsUtil.getAppSecrets(site).containsKey("bearerToken"));
    }
}
