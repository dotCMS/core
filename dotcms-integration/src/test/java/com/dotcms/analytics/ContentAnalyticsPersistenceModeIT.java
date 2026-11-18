package com.dotcms.analytics;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.Dependent;
import java.util.Map;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Integration coverage for the real {@link AppsAPI} round-trip the Persistence Mode "missing
 * treated as readwrite" rule (FR-002 / FR-002a) depends on. {@code EventAnalyticsProxyResourceTest}
 * already unit-tests the resource-level branching by mocking {@link ContentAnalyticsUtil}
 * entirely; what that mocking can't prove is the assumption underneath it -- that saving Content
 * Analytics app secrets without a {@code persistenceMode} key really does come back absent (not
 * silently defaulted) from the real secrets store, and that the SELECT param actually round-trips
 * through it. This class proves that data-layer assumption for real.
 *
 * @author dotCMS
 * @since 2026
 */
@Dependent
@RunWith(JUnit4WeldRunner.class)
public class ContentAnalyticsPersistenceModeIT extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#getAppSecrets(Host)}
     *
     * Given Scenario: Content Analytics app secrets are saved for a Site with only
     * {@code siteAuth} set -- simulating an instance that had Content Analytics configured
     * before the {@code persistenceMode} field existed (FR-002).
     *
     * Expected Result: The secrets map read back for that Site has no {@code persistenceMode}
     * entry at all -- it is genuinely absent, not silently defaulted to anything by the secrets
     * store -- which is the precondition {@link com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyResource}'s
     * "missing treated as readwrite" branch relies on.
     */
    @Test
    public void existingSecretsWithoutPersistenceMode_comeBackAbsent() throws Exception {
        final Host testSite = new SiteDataGen().nextPersisted(true);
        try {
            final AppsAPI appsAPI = APILocator.getAppsAPI();
            final AppSecrets secrets = new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY)
                    .withSecret("siteAuth", "DOT.48190c8c-42c4-46af-8d1a-0cd5db894797.persistenceModeIT-noKey")
                    .build();
            appsAPI.saveSecrets(secrets, testSite, APILocator.systemUser());

            final Map<String, Secret> readBack = ContentAnalyticsUtil.getAppSecrets(testSite);

            assertFalse("persistenceMode must not be present when it was never saved",
                    readBack.containsKey("persistenceMode"));
            assertNull(readBack.get("persistenceMode"));
        } finally {
            deleteSite(testSite);
        }
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#getAppSecrets(Host)}
     *
     * Given Scenario: Content Analytics is configured for a Site for the first time with
     * {@code persistenceMode} explicitly set to {@code "readwrite"} -- the value the generic
     * Apps UI would submit for a brand-new config, since the YAML marks {@code readwrite} as
     * {@code selected: true} (FR-002a; see
     * {@code specs/37521-content-analytics-mode/contracts/app-config-schema.md}).
     *
     * Expected Result: The `SELECT` param round-trips through the real secrets store correctly
     * -- the value read back is exactly {@code "readwrite"}, proving the data layer (not just
     * the YAML schema, already covered by {@code ContentAnalyticsPersistenceModeSchemaTest})
     * handles this param correctly.
     */
    @Test
    public void freshSetupWithExplicitReadWrite_roundTripsThroughRealSecretsStore() throws Exception {
        final Host testSite = new SiteDataGen().nextPersisted(true);
        try {
            final AppsAPI appsAPI = APILocator.getAppsAPI();
            final AppSecrets secrets = new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY)
                    .withSecret("siteAuth", "DOT.48190c8c-42c4-46af-8d1a-0cd5db894797.persistenceModeIT-fresh")
                    .withSecret("persistenceMode", "readwrite")
                    .build();
            appsAPI.saveSecrets(secrets, testSite, APILocator.systemUser());

            final Map<String, Secret> readBack = ContentAnalyticsUtil.getAppSecrets(testSite);

            final Secret persistenceMode = readBack.get("persistenceMode");
            assertNotNull("persistenceMode must be present when explicitly saved", persistenceMode);
            assertEquals("readwrite", persistenceMode.getString());
        } finally {
            deleteSite(testSite);
        }
    }

    /**
     * Deletes a test Site created for one of the methods above, logging rather than failing the
     * test on cleanup errors -- mirrors the teardown pattern in the sibling
     * {@code AnalyticsValidatorUtilTest}.
     *
     * @param site the Site to unpublish, archive, and delete.
     */
    private void deleteSite(final Host site) {
        final HostAPI hostAPI = APILocator.getHostAPI();
        try {
            hostAPI.unpublish(site, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
            hostAPI.archive(site, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
            hostAPI.delete(site, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
        } catch (final DotDataException | DotSecurityException e) {
            Logger.warn(ContentAnalyticsPersistenceModeIT.class, String.format(
                    "Failed to delete test Site '%s': %s", site.getIdentifier(),
                    ExceptionUtil.getErrorMessage(e)), e);
        }
    }
}
