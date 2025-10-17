package com.dotcms.ai.app;

import com.dotcms.ai.AiTest;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.security.apps.AppDescriptorHelper;
import com.dotcms.security.apps.AppsAPIImpl;
import com.dotcms.security.apps.SecretsStore;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.InvalidLicenseException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the ConfigService class.
 *
 * <p>
 * This class contains tests to verify the behavior of the ConfigService,
 * including scenarios with valid and invalid licenses, and configurations
 * with and without secrets.
 * </p>
 *
 * <p>
 * The tests ensure that the ConfigService correctly initializes and
 * configures the AppConfig based on the provided Host and license validity.
 * </p>
 *
 * @author vico
 */
public class ConfigServiceTest {

    private Host host;
    private ConfigService configService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void before() {
        host = new SiteDataGen().nextPersisted();
        configService = ConfigService.INSTANCE;
    }

    /**
     * Given a ConfigService with an invalid license
     * When the config method is called with a host
     * Then an InvalidLicenseException should be thrown.
     */
    @Test(expected = InvalidLicenseException.class)
    public void test_invalidLicense() {
        configService = new ConfigService(
                new AppsAPIImpl(
                        APILocator.getLayoutAPI(),
                        APILocator.getHostAPI(), SecretsStore.INSTANCE.get(),
                        CacheLocator.getAppsCache(), APILocator.getLocalSystemEventsAPI(), new AppDescriptorHelper(),
                        new LicenseValiditySupplier() {
                            public boolean hasValidLicense() {
                                return false;
                            }
                        })
        );
        configService.config(host);
    }

    /**
     * Given a host with secrets and a ConfigService
     * When the config method is called with the host
     * Then the models should be operational and the host should be correctly set in the AppConfig.
     */
    @Test
    public void test_config_hostWithSecrets() throws Exception {
        AiTest.aiAppSecrets(host, "text-model-0", "image-model-1", "embeddings-model-2");
        final AiAppConfig appConfig = configService.config(host);

        assertTrue(appConfig.getModel().isOperational());
        assertTrue(appConfig.getImageModel().isOperational());
        assertTrue(appConfig.getEmbeddingsModel().isOperational());
        assertEquals(host.getHostname(), appConfig.getHost());
    }

    /**
     * Given a host without secrets and a ConfigService
     * When the config method is called with the host
     * Then the models should be operational and the host should be set to "System Host" in the AppConfig.
     */
    @Test
    public void test_config_hostWithoutSecrets() throws Exception {
        AiTest.aiAppSecrets(APILocator.systemHost(), "text-model-10", "image-model-11", "embeddings-model-12");
        final AiAppConfig appConfig = configService.config(host);

        assertTrue(appConfig.getModel().isOperational());
        assertTrue(appConfig.getImageModel().isOperational());
        assertTrue(appConfig.getEmbeddingsModel().isOperational());
        assertEquals("System Host", appConfig.getHost());
    }

}
