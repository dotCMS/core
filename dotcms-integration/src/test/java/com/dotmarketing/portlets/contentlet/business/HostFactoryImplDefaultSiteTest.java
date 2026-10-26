package com.dotmarketing.portlets.contentlet.business;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that the default site is found no matter how {@code persist.contentlet.as.json} is set.
 * <p>
 * Several integration tests switch that flag off for a while. If the default site cannot be found
 * while it is off, {@link HostAPIImpl} creates a second default site called {@code noDefault-<millis>}
 * and caches it, and later tests that resolve the current site (for example the Shorty servlet tests)
 * get that site instead of the real one.
 */
public class HostFactoryImplDefaultSiteTest {

    /**
     * Starts the integration test environment.
     *
     * @throws Exception if the environment cannot be started
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link com.dotmarketing.business.HostAPI#findDefaultHost(User, boolean)}
     * Given Scenario: The default site cache is empty and {@code persist.contentlet.as.json} is off
     * Expected Result: The existing default site is returned and no {@code noDefault-} site is created
     */
    @Test
    public void findDefaultHost_withJsonPersistenceOff_returnsTheExistingDefaultSite() throws Exception {

        final User systemUser = APILocator.systemUser();
        final Host defaultSite = APILocator.getHostAPI().findDefaultHost(systemUser, false);
        final boolean previousValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);

        Host foundSite = null;
        try {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
            CacheLocator.getHostCache().clearCache();

            foundSite = APILocator.getHostAPI().findDefaultHost(systemUser, false);

            assertFalse("A second default site was created: " + foundSite.getHostname(),
                    foundSite.getHostname().startsWith("noDefault-"));
            assertEquals(defaultSite.getIdentifier(), foundSite.getIdentifier());
        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, previousValue);
            if (null != foundSite && !defaultSite.getIdentifier().equals(foundSite.getIdentifier())) {
                // Saving the second default site took the default flag off the real one
                CacheLocator.getHostCache().clearCache();
                final Host previousDefault = APILocator.getHostAPI().find(
                        defaultSite.getIdentifier(), systemUser, false);
                if (!previousDefault.isDefault()) {
                    APILocator.getHostAPI().makeDefault(previousDefault, systemUser, false);
                }
                final Host secondDefault = APILocator.getHostAPI().find(
                        foundSite.getIdentifier(), systemUser, false);
                APILocator.getHostAPI().archive(secondDefault, systemUser, false);
                APILocator.getHostAPI().delete(secondDefault, systemUser, false);
            }
            CacheLocator.getHostCache().clearCache();
        }
    }
}
