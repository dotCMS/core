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

    /**
     * Incoming: hidden param submitted as the mask, with NO existing config at all (e.g. a tier-3
     * System Host env param not surfaced by the fallbackOnSystemHost=false read).
     * Expected: the literal mask is never persisted.
     */
    @Test
    public void test_maintainHiddenValues_never_persists_mask_when_no_existing() throws Exception {
        final AppsAPIImpl api = newApi();

        final Secret incoming = Secret.builder()
                .withType(Type.STRING)
                .withHidden(true)
                .withValue(SecretViewSerializer.HIDDEN_SECRET_MASK)
                .build();
        final AppSecrets toSave = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, incoming).build();

        final AppSecrets result = api.maintainHiddenValues(toSave, Optional.empty());

        assertFalse("The literal mask must never be persisted, even with no existing value",
                result.getSecrets().containsKey(PARAM));
    }

    /**
     * Incoming: non-hidden value identical to an env-backed existing value (unchanged form re-submit).
     * Expected: not snapshotted into the stored blob (stays env-resolved).
     */
    @Test
    public void test_maintainHiddenValues_drops_unchanged_env_value() throws Exception {
        final AppsAPIImpl api = newApi();

        final Secret incoming = Secret.builder()
                .withType(Type.STRING)
                .withValue("from-env")
                .build();
        final AppSecrets toSave = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, incoming).build();

        final Secret envExisting = Secret.builder()
                .withType(Type.STRING)
                .withValue("from-env")
                .withFromEnv(true)
                .build();
        final AppSecrets existing = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, envExisting).build();

        final AppSecrets result = api.maintainHiddenValues(toSave, Optional.of(existing));

        assertFalse("Unchanged env value must not be snapshotted into the stored blob",
                result.getSecrets().containsKey(PARAM));
    }

    /**
     * Incoming: non-hidden value DIFFERENT from an env-backed existing value (admin changed it).
     * Expected: persisted (a host-specific stored value legitimately wins per specificity).
     */
    @Test
    public void test_maintainHiddenValues_persists_changed_env_value() throws Exception {
        final AppsAPIImpl api = newApi();

        final Secret incoming = Secret.builder()
                .withType(Type.STRING)
                .withValue("admin-set")
                .build();
        final AppSecrets toSave = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, incoming).build();

        final Secret envExisting = Secret.builder()
                .withType(Type.STRING)
                .withValue("from-env")
                .withFromEnv(true)
                .build();
        final AppSecrets existing = AppSecrets.builder().withKey(APP_KEY)
                .withSecret(PARAM, envExisting).build();

        final AppSecrets result = api.maintainHiddenValues(toSave, Optional.of(existing));

        assertTrue("A changed value must be persisted as a host-specific stored secret",
                result.getSecrets().containsKey(PARAM));
        assertEquals("admin-set", result.getSecrets().get(PARAM).getString());
    }
}
