package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * Guards how {@link AppsAPIImpl#filterSitesForAppKey} behaves when the App secrets store cannot be
 * read (issue #36724).
 *
 * This is the highest-consequence path of that change: {@code EMAWebInterceptor.existsConfiguration}
 * reaches this method on every {@code /api/*} request and the interceptor chain does not guard it, so
 * raising here returns a 500 for the entire API.
 *
 * The reason this class exists is that the fix regressed once already. The store raises
 * {@link SecretsStoreUnreadableException} deep inside itself, but
 * {@code catch (Exception e) { throw new DotRuntimeException(e); }} layers sit between there and here
 * -- {@code SecretCachedKeyStoreImpl.containsKey} is the one on this path -- so what actually arrives
 * is a bare {@link DotRuntimeException} wrapping the real cause. Matching on the thrown type
 * compiled, read as the cleanest option, passed every test, and never fired in production. These
 * cases therefore throw the failure **wrapped**, the way it really arrives.
 *
 * Every case makes {@code containsKey} throw, which deliberately keeps the test out of
 * {@code hasAnySecrets}' env-var branch: that reaches descriptor and config lookups which, with no
 * database present, take {@code DbConnectionFactory}'s {@code System.exit} path
 * ({@code SYSTEM_EXIT_ON_STARTUP_FAILURE} defaults to true) and kill the surefire fork.
 */
public class AppsAPIImplUnreadableStoreTest {

    private static final String APP_KEY = "dotcms-app";
    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    private AppsAPIImpl newApi(final SecretsStore secretsStore) {
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        try {
            // Grant portlet access, so the check in hasAnySecrets passes and the store is reached.
            when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), any(User.class)))
                    .thenReturn(true);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return new AppsAPIImpl(
                layoutAPI,
                mock(HostAPI.class),
                secretsStore,
                mock(AppsCache.class),
                mock(LocalSystemEventsAPI.class),
                mock(AppDescriptorHelper.class),
                new LicenseValiditySupplier() {
                    @Override
                    public boolean hasValidLicense() {
                        return true;
                    }
                });
    }

    private SecretsStore storeThatThrows(final RuntimeException failure) {
        final SecretsStore secretsStore = mock(SecretsStore.class);
        when(secretsStore.containsKey(anyString())).thenThrow(failure);
        return secretsStore;
    }

    private static SecretsStoreUnreadableException unreadable() {
        return new SecretsStoreUnreadableException(
                "Unable to load the App secrets store: the store could not be decrypted",
                new java.io.IOException("Integrity check failed"));
    }

    /**
     * Method to test: {@link AppsAPIImpl#filterSitesForAppKey(String, java.util.Collection, User)}
     * Given Scenario: the store cannot be read, and the failure arrives wrapped in a bare
     * DotRuntimeException by the intermediate cache layer — exactly as it does in production.
     * Expected Result: the site reports no secrets. It must not raise: EMAWebInterceptor calls this
     * on every /api/* request, so raising returns a 500 for the whole API.
     */
    @Test
    public void test_unreadableStoreWrapped_degradesInsteadOfRaising() {
        final Set<String> configured = newApi(storeThatThrows(new DotRuntimeException(unreadable())))
                .filterSitesForAppKey(APP_KEY, List.of(SITE_ID), mock(User.class));

        assertTrue("a wrapped unreadable-store failure must report no configured sites, not raise",
                configured.isEmpty());
    }

    /**
     * Method to test: {@link AppsAPIImpl#filterSitesForAppKey(String, java.util.Collection, User)}
     * Given Scenario: two layers of wrapping, as happens when listKeys re-wraps before containsKey
     * re-wraps again.
     * Expected Result: still degrades. The number of wrapping layers is not this method's business,
     * which is why the guard walks the cause chain instead of matching the thrown type.
     */
    @Test
    public void test_unreadableStoreDoubleWrapped_stillDegrades() {
        final Set<String> configured = newApi(
                storeThatThrows(new DotRuntimeException(new DotRuntimeException(unreadable()))))
                .filterSitesForAppKey(APP_KEY, List.of(SITE_ID), mock(User.class));

        assertTrue("depth of wrapping must not matter", configured.isEmpty());
    }

    /**
     * Method to test: {@link AppsAPIImpl#filterSitesForAppKey(String, java.util.Collection, User)}
     * Given Scenario: an unrelated runtime failure — a database blip or cache error, which dotCMS
     * surfaces as a plain DotRuntimeException with nothing store-related in its cause chain.
     * Expected Result: it propagates. Swallowing it would report "0 configurations" for a transient
     * infrastructure problem and would hide genuine bugs on a very hot path.
     */
    @Test
    public void test_unrelatedRuntimeFailure_propagates() {
        try {
            newApi(storeThatThrows(new DotRuntimeException("the database went away")))
                    .filterSitesForAppKey(APP_KEY, List.of(SITE_ID), mock(User.class));
            fail("an unrelated infrastructure failure must not be reported as 'no secrets'");
        } catch (DotRuntimeException expected) {
            assertEquals("the database went away", expected.getMessage());
        }
    }
}
