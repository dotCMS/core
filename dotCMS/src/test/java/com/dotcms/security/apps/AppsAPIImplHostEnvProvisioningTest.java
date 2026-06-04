package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;

/**
 * Unit test for the tier-1 host-specific env-backed provisioning path wired into
 * {@link AppsAPIImpl#getSecrets(String, boolean, Host, User)}.
 *
 * <p>It verifies that when the secrets store holds no blob for the given host, {@code getSecrets()}
 * still returns the host-specific env-backed value by constructing the
 * {@code DOT_{APP_KEY}_{HOSTNAME}_{APP_VALUE_KEY}} env var name (via {@code AppsUtil.envVarName()})
 * and looking it up (via {@code AppsUtil.hostEnvSecret()}). The store is mocked to return no blob
 * and the environment is stubbed through {@link Config#setProperty(String, Object)}.</p>
 *
 * @author Ouroboros
 */
public class AppsAPIImplHostEnvProvisioningTest {

    private static final String APP_KEY = "dotcms-app";
    private static final String HOST_NAME = "demo.dotcms.com";
    private static final String HOST_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";
    private static final String PARAM_NAME = "clientId";
    private static final String ENV_VALUE = "the-client-id";

    private String envVarName() {
        return AppsUtil.envVarName(APP_KEY, HOST_NAME, PARAM_NAME);
    }

    @After
    public void cleanup() {
        Config.setProperty(envVarName(), null);
    }

    private AppsAPIImpl newApiWithNoStoredBlob() {
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        final SecretsStore secretsStore = mock(SecretsStore.class);
        final AppsCache appsCache = mock(AppsCache.class);
        final LocalSystemEventsAPI localSystemEventsAPI = mock(LocalSystemEventsAPI.class);
        final AppDescriptorHelper appDescriptorHelper = mock(AppDescriptorHelper.class);
        final LicenseValiditySupplier licenseValiditySupplier = new LicenseValiditySupplier() {
            @Override
            public boolean hasValidLicense() {
                return true;
            }
        };

        // No stored secrets blob for any host.
        when(secretsStore.getValue(anyString())).thenReturn(Optional.empty());

        // Registered app descriptor that declares the param we want to provision from env.
        final ParamDescriptor paramDescriptor = mock(ParamDescriptor.class);
        final AppDescriptor appDescriptor = mock(AppDescriptor.class);
        when(appDescriptor.getKey()).thenReturn(APP_KEY);
        when(appDescriptor.getParams()).thenReturn(Map.of(PARAM_NAME, paramDescriptor));
        when(appsCache.getAppDescriptorsMap(any()))
                .thenReturn(Map.of(APP_KEY.toLowerCase(), appDescriptor));

        return new AppsAPIImpl(layoutAPI, hostAPI, secretsStore, appsCache,
                localSystemEventsAPI, appDescriptorHelper, licenseValiditySupplier);
    }

    private User adminUser() {
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(true);
        return user;
    }

    /**
     * Given: no stored blob for the host and a host-specific env var stubbed with a value.
     * Expected: getSecrets() returns AppSecrets carrying the env-backed value with fromEnv=true.
     */
    @Test
    public void test_getSecrets_returns_host_env_value_when_no_stored_blob() throws Exception {
        Config.setProperty(envVarName(), ENV_VALUE);

        final AppsAPIImpl api = newApiWithNoStoredBlob();
        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(HOST_ID);
        when(host.getHostname()).thenReturn(HOST_NAME);

        final Optional<AppSecrets> resolved = api.getSecrets(APP_KEY, false, host, adminUser());

        assertTrue("Expected AppSecrets resolved from host-specific env var", resolved.isPresent());
        final Secret secret = resolved.get().getSecrets().get(PARAM_NAME);
        assertTrue("Expected the param to be present", secret != null);
        assertEquals(ENV_VALUE, secret.getString());
        assertTrue("Expected fromEnv=true for env-sourced value", secret.isFromEnv());
        assertEquals(APP_KEY, resolved.get().getKey());
    }

    /**
     * Given: no stored blob and no env var stubbed for the host-specific name.
     * Expected: getSecrets() returns empty (nothing to provision).
     */
    @Test
    public void test_getSecrets_returns_empty_when_no_blob_and_no_env() throws Exception {
        Config.setProperty(envVarName(), null);

        final AppsAPIImpl api = newApiWithNoStoredBlob();
        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(HOST_ID);
        when(host.getHostname()).thenReturn(HOST_NAME);

        final Optional<AppSecrets> resolved = api.getSecrets(APP_KEY, false, host, adminUser());

        assertFalse("Expected empty when neither stored blob nor env var resolve", resolved.isPresent());
    }
}
