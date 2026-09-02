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
import java.util.List;
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
    // A required param with no descriptor default — mirrors Content Analytics' "siteAuth".
    private static final String REQUIRED_PARAM = "siteAuth";
    private static final String REQUIRED_ENV_VALUE = "my-site-auth-key";

    private String envVarName() {
        return AppsUtil.envVarName(APP_KEY, HOST_NAME, PARAM_NAME);
    }

    private String requiredEnvVarName() {
        return AppsUtil.envVarName(APP_KEY, HOST_NAME, REQUIRED_PARAM);
    }

    @After
    public void cleanup() {
        Config.setProperty(envVarName(), null);
        Config.setProperty(requiredEnvVarName(), null);
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

    /**
     * Builds an API whose descriptor declares a required, no-default param ({@code siteAuth}) plus an
     * optional one ({@code clientId}). The store reports no blob, but {@code containsKey} returns true
     * so {@code filterSitesForAppKey} short-circuits without hitting {@code APILocator.systemUser()}.
     */
    /**
     * A descriptor declaring a required, no-default param ({@code siteAuth}) plus an optional one
     * ({@code clientId}) — the shape that exercises the missing-value warning.
     */
    private AppDescriptor requiredParamDescriptor() {
        // Required, no-default param — the condition under which the missing-value warning fires.
        final ParamDescriptor requiredParam = mock(ParamDescriptor.class);
        when(requiredParam.isRequired()).thenReturn(true);
        when(requiredParam.getValue()).thenReturn(null);
        when(requiredParam.getType()).thenReturn(Type.STRING);
        when(requiredParam.isHidden()).thenReturn(false);

        // Optional param used to keep AppSecrets non-empty in the missing-value control case.
        final ParamDescriptor optionalParam = mock(ParamDescriptor.class);
        when(optionalParam.isRequired()).thenReturn(false);
        when(optionalParam.getType()).thenReturn(Type.STRING);
        when(optionalParam.isHidden()).thenReturn(false);

        final AppDescriptor appDescriptor = mock(AppDescriptor.class);
        when(appDescriptor.getKey()).thenReturn(APP_KEY);
        when(appDescriptor.getParams())
                .thenReturn(Map.of(REQUIRED_PARAM, requiredParam, PARAM_NAME, optionalParam));
        return appDescriptor;
    }

    /**
     * Builds an API wired to the given descriptor. The store reports no blob, but {@code containsKey}
     * returns true so {@code filterSitesForAppKey} short-circuits without hitting
     * {@code APILocator.systemUser()}.
     */
    private AppsAPIImpl newApiFor(final AppDescriptor appDescriptor) {
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

        // No stored blob, but report the site as configured so computeSecretWarnings proceeds without
        // the env-detection path that would call APILocator.systemUser().
        when(secretsStore.getValue(anyString())).thenReturn(Optional.empty());
        when(secretsStore.containsKey(anyString())).thenReturn(true);
        when(appsCache.getAppDescriptorsMap(any()))
                .thenReturn(Map.of(APP_KEY.toLowerCase(), appDescriptor));

        return new AppsAPIImpl(layoutAPI, hostAPI, secretsStore, appsCache,
                localSystemEventsAPI, appDescriptorHelper, licenseValiditySupplier);
    }

    private Host hostMock() {
        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(HOST_ID);
        when(host.getHostname()).thenReturn(HOST_NAME);
        return host;
    }

    /**
     * Regression for the tier-1 env-locked secret being wrongly reported as missing.
     * <p>
     * A required, no-default param provisioned through the host-specific env var keeps its value in
     * {@code envVarValue} (not {@code value}), so the warning check must honor {@code hasEnvVarValue()}.
     * Before the fix, {@code computeSecretWarnings} flagged {@code siteAuth} as
     * "required, missing value" even though the env var supplied it.
     * <p>
     * Given: no stored blob and the host-specific env var set for the required param.
     * Expected: no warning is produced for that param.
     */
    @Test
    public void test_no_warning_when_required_param_provisioned_via_host_env() throws Exception {
        Config.setProperty(requiredEnvVarName(), REQUIRED_ENV_VALUE);

        final AppDescriptor descriptor = requiredParamDescriptor();
        final AppsAPIImpl api = newApiFor(descriptor);
        final Host host = hostMock();

        final Map<String, List<String>> warnings =
                api.computeSecretWarnings(descriptor, host, adminUser());

        assertFalse("Env-provisioned required param must not be flagged as missing",
                warnings.containsKey(REQUIRED_PARAM));
        assertTrue("Expected no warnings at all for a fully provisioned required param",
                warnings.isEmpty());
    }

    /**
     * Control: the missing-value warning must still fire for a genuinely unprovisioned required param.
     * <p>
     * Given: the optional param is env-backed (so AppSecrets is present) but the required {@code siteAuth}
     * has no env var and no stored value.
     * Expected: a warning is produced for {@code siteAuth} only.
     */
    @Test
    public void test_warning_present_when_required_param_missing() throws Exception {
        // Only the optional param is provisioned; the required one is left unset.
        Config.setProperty(envVarName(), ENV_VALUE);
        Config.setProperty(requiredEnvVarName(), null);

        final AppDescriptor descriptor = requiredParamDescriptor();
        final AppsAPIImpl api = newApiFor(descriptor);
        final Host host = hostMock();

        final Map<String, List<String>> warnings =
                api.computeSecretWarnings(descriptor, host, adminUser());

        assertTrue("A required param with no value must still warn", warnings.containsKey(REQUIRED_PARAM));
        assertFalse("The provisioned optional param must not warn", warnings.containsKey(PARAM_NAME));
    }
}
