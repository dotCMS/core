package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.rest.api.v1.apps.view.SecretView.SecretViewSerializer;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import java.util.Optional;
import org.junit.Test;

/**
 * Guards that env-backed (fromEnv) Secrets are never promoted into the persisted stored blob.
 *
 * <p>When the UI submits the unchanged hidden mask for a param whose current value is env-backed,
 * {@link AppsAPIImpl#maintainHiddenValues(AppSecrets, Optional)} must NOT copy the env value into the
 * secrets-to-save (otherwise the env value would be serialized and become a permanent stored secret,
 * defeating read-time env resolution).</p>
 *
 * @author Ouroboros
 */
public class AppsAPIImplEnvWriteGuardTest {

    private static final String APP_KEY = "dotcms-app";
    private static final String PARAM = "apiKey";

    private AppsAPIImpl newApi() {
        return new AppsAPIImpl(
                mock(LayoutAPI.class),
                mock(HostAPI.class),
                mock(SecretsStore.class),
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

    /**
     * Incoming: hidden param submitted as the unchanged mask. Existing: env-backed Secret.
     * Expected: the param is omitted from the result (not promoted into the stored blob).
     */
    @Test
    public void test_maintainHiddenValues_does_not_promote_env_backed_value() throws Exception {
        final AppsAPIImpl api = newApi();

        final Secret incoming = Secret.builder()
                .withType(Type.STRING)
                .withHidden(true)
                .withValue(SecretViewSerializer.HIDDEN_SECRET_MASK)
                .build();
        final AppSecrets toSave = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, incoming).build();

        final Secret envExisting = Secret.builder()
                .withType(Type.STRING)
                .withHidden(true)
                .withEnvValue("from-env")
                .withFromEnv(true)
                .build();
        final AppSecrets existing = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, envExisting).build();

        final AppSecrets result = api.maintainHiddenValues(toSave, Optional.of(existing));

        assertFalse("Env-backed value must not be promoted into the stored blob",
                result.getSecrets().containsKey(PARAM));
    }

    /**
     * Sanity check: when the existing value is a normal stored secret (not env-backed), the unchanged
     * mask DOES retain the existing stored value (the historical behavior is preserved).
     */
    @Test
    public void test_maintainHiddenValues_retains_stored_value() throws Exception {
        final AppsAPIImpl api = newApi();

        final Secret incoming = Secret.builder()
                .withType(Type.STRING)
                .withHidden(true)
                .withValue(SecretViewSerializer.HIDDEN_SECRET_MASK)
                .build();
        final AppSecrets toSave = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, incoming).build();

        final Secret stored = Secret.builder()
                .withType(Type.STRING)
                .withHidden(true)
                .withValue("stored-value")
                .build();
        final AppSecrets existing = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, stored).build();

        final AppSecrets result = api.maintainHiddenValues(toSave, Optional.of(existing));

        assertTrue("Stored value must be retained for unchanged hidden mask",
                result.getSecrets().containsKey(PARAM));
        assertEquals("stored-value", result.getSecrets().get(PARAM).getString());
    }
}
