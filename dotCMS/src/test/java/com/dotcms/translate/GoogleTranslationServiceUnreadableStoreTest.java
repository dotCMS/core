package com.dotcms.translate;

import com.dotcms.rendering.velocity.viewtools.JSONTool;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.SecretsStoreUnreadableException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static com.dotcms.translate.GoogleTranslationService.API_KEY_VAR;
import static com.dotcms.translate.GoogleTranslationService.GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers {@code GoogleTranslationService.getFallbackAPIKey()} when the App secrets store cannot be
 * read.
 *
 * <p>That method exists precisely to fall back to the configured
 * {@code GOOGLE_TRANSLATE_SERVICE_API_KEY} property, but its catch named only
 * {@code DotDataException | DotSecurityException}. Since issue #36724 an unreadable store raises --
 * where it previously wiped itself and returned nothing -- and the raise walked straight past that
 * catch, so the fallback never happened.
 *
 * <p>The failure is thrown in its <em>wrapped</em> production shape,
 * {@code DotRuntimeException(SecretsStoreUnreadableException(...))}: the layers between the store and
 * here re-wrap it, so a test asserting on the unwrapped type would pass while production still broke.
 *
 * <p>Reached through {@code setServiceParameters} with a blank API key, which is the only path to the
 * private fallback method.
 */
public class GoogleTranslationServiceUnreadableStoreTest {

    private static final String CONFIGURED_KEY = "configured-fallback-key";
    private static final String HOST_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<Config> config;
    private AppsAPI appsAPI;
    private GoogleTranslationService service;

    /** The shape production actually produces: re-wrapped as a bare DotRuntimeException. */
    private static DotRuntimeException wrappedStoreFailure() {
        return new DotRuntimeException(new SecretsStoreUnreadableException(
                "cannot read dotSecretsStore.p12", new IOException("wrong password")));
    }

    /** A blank key is what sends setServiceParameters into the fallback lookup. */
    private static List<ServiceParameter> blankApiKeyParam() {
        return List.of(new ServiceParameter(API_KEY_VAR, "Service API Key", StringPool.BLANK));
    }

    @Before
    public void setUp() throws Exception {
        appsAPI = mock(AppsAPI.class);

        // find() must be stubbed to a real (mock) Host, not left to return null. getFallbackAPIKey
        // wraps it in Try.of(...).getOrElse(systemHost), and a null RESULT is a Success -- getOrElse
        // only fires on failure -- so the host stays null, and Mockito's any(Host.class) does not
        // match null. The getSecrets stub below would then never apply, Mockito would hand back its
        // default Optional.empty(), and every case would take the "no secrets" branch: the
        // unreadable-store test would pass without the exception ever being thrown.
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.find(anyString(), any(User.class), anyBoolean())).thenReturn(mock(Host.class));

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        apiLocator.when(APILocator::systemUser).thenReturn(mock(User.class));
        apiLocator.when(APILocator::systemHost).thenReturn(mock(Host.class));

        config = mockStatic(Config.class);
        config.when(() -> Config.getStringProperty(
                        eq(GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY), anyString()))
                .thenReturn(CONFIGURED_KEY);
        config.when(() -> Config.getStringProperty(
                        eq("GOOGLE_TRANSLATE_SERVICE_BASE_URL"), anyString()))
                .thenReturn(GoogleTranslationService.BASE_URL);

        service = new GoogleTranslationService(
                StringPool.BLANK, mock(JSONTool.class), mock(ApiProvider.class));
    }

    @After
    public void tearDown() {
        config.close();
        apiLocator.close();
    }

    /**
     * Given an unreadable store, when the service resolves its API key, then it falls back to the
     * configured property instead of propagating.
     */
    @Test
    public void unreadableStore_fallsBackToConfiguredKey() throws Exception {
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenThrow(wrappedStoreFailure());

        service.setServiceParameters(blankApiKeyParam(), HOST_ID);

        assertEquals(CONFIGURED_KEY, service.getApiKey());
    }

    /**
     * An unrelated infrastructure failure must still propagate -- silently falling back would hide a
     * real outage behind a working-looking configuration.
     */
    @Test
    public void unrelatedRuntimeFailure_isNotMasked() throws Exception {
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenThrow(new DotRuntimeException("the database went away"));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> service.setServiceParameters(blankApiKeyParam(), HOST_ID));
        assertEquals("the database went away", thrown.getMessage());
    }

    /**
     * Guards the happy path against an over-broad catch: a readable store's secret still wins over
     * the configured property.
     */
    @Test
    public void readableStore_prefersTheStoredSecret() throws Exception {
        final AppSecrets secrets = new AppSecrets.Builder()
                .withKey(GoogleTranslationService.GOOGLE_TRANSLATE_APP_CONFIG_KEY)
                .withHiddenSecret(API_KEY_VAR, "stored-key")
                .build();
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.of(secrets));

        service.setServiceParameters(blankApiKeyParam(), HOST_ID);

        assertEquals("stored-key", service.getApiKey());
    }
}
