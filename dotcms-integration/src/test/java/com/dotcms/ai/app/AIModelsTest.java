package com.dotcms.ai.app;

import com.dotcms.ai.AiTest;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the \AIModels\ class. This test class verifies the functionality
 * of methods in \AIModels\ such as loading models, finding models by host and type, and
 * retrieving supported models. It uses \WireMockServer\ to simulate external dependencies
 * and \IntegrationTestInitService\ for initializing the test environment.
 *
 * @author vico
 */
public class AIModelsTest {

    private static WireMockServer wireMockServer;
    private final AIModels aiModels = AIModels.get();
    private Host host;
    private Host otherHost;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        AiTest.aiAppSecrets(APILocator.systemHost());
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() {
        host = new SiteDataGen().nextPersisted();
        otherHost = new SiteDataGen().nextPersisted();
        List.of(host, otherHost).forEach(h -> Try.of(() -> AiTest.aiAppSecrets(h)).get());
    }

    /**
     * Given a set of models loaded into the AIModels instance
     * When the resolveModel method is called with various model names and types
     * Then the correct models should be resolved and their operational status verified.
     */
    @Test
    public void test_resolveModel() throws Exception {
        AiTest.aiAppSecrets(host, "text-model-20", "image-model-21", "embeddings-model-22");
        ConfigService.INSTANCE.config(host);
        AiTest.aiAppSecrets(otherHost, "text-model-23", null, null);
        ConfigService.INSTANCE.config(otherHost);

        assertTrue(aiModels.resolveModel(host.getHostname(), AIModelType.TEXT).isOperational());
        assertTrue(aiModels.resolveModel(host.getHostname(), AIModelType.IMAGE).isOperational());
        assertTrue(aiModels.resolveModel(host.getHostname(), AIModelType.EMBEDDINGS).isOperational());
        assertTrue(aiModels.resolveModel(otherHost.getHostname(), AIModelType.TEXT).isOperational());
        assertFalse(aiModels.resolveModel(otherHost.getHostname(), AIModelType.IMAGE).isOperational());
        assertFalse(aiModels.resolveModel(otherHost.getHostname(), AIModelType.EMBEDDINGS).isOperational());
    }

    /**
     * Given a URL for supported models
     * When the getOrPullSupportedModules method is called
     * Then a list of supported models should be returned.
     */
    @Test
    public void test_getOrPullSupportedModels() {
        AIModels.get().cleanSupportedModelsCache();
        final AppConfig appConfig = ConfigService.INSTANCE.config(host);

        Set<String> supported = aiModels.getOrPullSupportedModels(appConfig);
        assertNotNull(supported);
        assertEquals(38, supported.size());
    }

    /**
     * Given an invalid URL for supported models
     * When the getOrPullSupportedModules method is called
     * Then an exception should be thrown
     */
    @Test
    public void test_getOrPullSupportedModuels_withNetworkError() {
        final AppConfig appConfig = ConfigService.INSTANCE.config(host);
        AIModels.get().cleanSupportedModelsCache();
        IPUtils.disabledIpPrivateSubnet(false);

        assertThrows(DotRuntimeException.class, () ->aiModels.getOrPullSupportedModels(appConfig));
        IPUtils.disabledIpPrivateSubnet(true);
    }

    /**
     * Given no API key
     * When the getOrPullSupportedModules method is called
     * Then an exception should be thrown.
     */
    @Test
    public void test_getOrPullSupportedModels_noApiKey() throws Exception {
        AiTest.aiAppSecrets(host, null);
        final AppConfig appConfig = ConfigService.INSTANCE.config(host);

        AIModels.get().cleanSupportedModelsCache();
        final Set<String> supported = aiModels.getOrPullSupportedModels(appConfig);

        assertTrue(supported.isEmpty());
    }


}
