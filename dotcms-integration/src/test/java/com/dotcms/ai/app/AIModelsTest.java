package com.dotcms.ai.app;

import com.dotcms.ai.AiTest;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
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
        List.of(host, otherHost).forEach(h -> Try.of(() -> AiTest.aiAppSecrets(wireMockServer, host)).get());
    }

    /**
     * Given a set of models loaded into the AIModels instance
     * When the findModel method is called with various model names and types
     * Then the correct models should be found and returned.
     */
    @Test
    public void test_loadModels_andFindThem() throws DotDataException, DotSecurityException {
        saveSecrets(
                    host,
                    "text-model-1,text-model-2",
                    "image-model-3,image-model-4",
                    "embeddings-model-5,embeddings-model-6");
        saveSecrets(otherHost, "text-model-1", null, null);

        final String hostId = host.getHostname();

        final Optional<AIModel> notFound = aiModels.findModel(hostId, "some-invalid-model-name");
        assertTrue(notFound.isEmpty());

        final Optional<AIModel> text1 = aiModels.findModel(hostId, "text-model-1");
        final Optional<AIModel> text2 = aiModels.findModel(hostId, "text-model-2");
        assertModels(text1, text2, AIModelType.TEXT);

        final Optional<AIModel> image1 = aiModels.findModel(hostId, "image-model-3");
        final Optional<AIModel> image2 = aiModels.findModel(hostId, "image-model-4");
        assertModels(image1, image2, AIModelType.IMAGE);

        final Optional<AIModel> embeddings1 = aiModels.findModel(hostId, "embeddings-model-5");
        final Optional<AIModel> embeddings2 = aiModels.findModel(hostId, "embeddings-model-6");
        assertModels(embeddings1, embeddings2, AIModelType.EMBEDDINGS);

        assertNotSame(text1.get(), image1.get());
        assertNotSame(text1.get(), embeddings1.get());
        assertNotSame(image1.get(), embeddings1.get());

        final Optional<AIModel> text3 = aiModels.findModel(hostId, AIModelType.TEXT);
        assertSameModels(text3, text1, text2);

        final Optional<AIModel> image3 = aiModels.findModel(hostId, AIModelType.IMAGE);
        assertSameModels(image3, image1, image2);

        final Optional<AIModel> embeddings3 = aiModels.findModel(hostId, AIModelType.EMBEDDINGS);
        assertSameModels(embeddings3, embeddings1, embeddings2);

        final Optional<AIModel> text4 = aiModels.findModel(otherHost.getHostname(), "text-model-1");
        assertTrue(text3.isPresent());
        assertNotSame(text1.get(), text4.get());

        saveSecrets(
                host,
                "text-model-7,text-model-8",
                "image-model-9,image-model-10",
                "embeddings-model-11, embeddings-model-12");

        final Optional<AIModel> text7 = aiModels.findModel(hostId, "text-model-7");
        final Optional<AIModel> text8 = aiModels.findModel(hostId, "text-model-8");
        assertModels(text7, text8, AIModelType.TEXT);

        final Optional<AIModel> image9 = aiModels.findModel(hostId, "image-model-9");
        final Optional<AIModel> image10 = aiModels.findModel(hostId, "image-model-10");
        assertModels(image9, image10, AIModelType.IMAGE);

        final Optional<AIModel> embeddings11 = aiModels.findModel(hostId, "embeddings-model-11");
        assertTrue(embeddings11.isPresent());
        final Optional<AIModel> embeddings12 = aiModels.findModel(hostId, "embeddings-model-12");
        assertModels(embeddings11, embeddings12, AIModelType.EMBEDDINGS);
    }

    /**
     * Given a URL for supported models
     * When the getOrPullSupportedModules method is called
     * Then a list of supported models should be returned.
     */
    @Test
    public void test_getOrPullSupportedModules() throws DotDataException, DotSecurityException {
        AiTest.aiAppSecrets(wireMockServer, APILocator.systemHost());
        AIModels.get().cleanSupportedModelsCache();

        List<String> supported = aiModels.getOrPullSupportedModels();
        assertNotNull(supported);
        assertEquals(32, supported.size());

        supported = aiModels.getOrPullSupportedModels();
        assertNotNull(supported);
        assertEquals(32, supported.size());

        AIModels.get().setAppConfigSupplier(ConfigService.INSTANCE::config);
    }

    /**
     * Given an invalid URL for supported models
     * When the getOrPullSupportedModules method is called
     * Then an empty list of supported models should be returned.
     */
    @Test
    public void test_getOrPullSupportedModules_invalidEndpoint() {
        AIModels.get().cleanSupportedModelsCache();
        IPUtils.disabledIpPrivateSubnet(false);

        final List<String> supported = aiModels.getOrPullSupportedModels();
        assertNotNull(supported);
        assertTrue(supported.isEmpty());

        IPUtils.disabledIpPrivateSubnet(true);
        AIModels.get().setAppConfigSupplier(ConfigService.INSTANCE::config);
    }

    /**
     * Given no API key
     * When the getOrPullSupportedModules method is called
     * Then an empty list of supported models should be returned.
     */
    @Test
    public void test_getOrPullSupportedModules_noApiKey() throws DotDataException, DotSecurityException {
        AiTest.aiAppSecrets(wireMockServer, APILocator.systemHost(), null);

        AIModels.get().cleanSupportedModelsCache();
        final List<String> supported = aiModels.getOrPullSupportedModels();
        assertNotNull(supported);
        assertTrue(supported.isEmpty());
    }

    private void saveSecrets(final Host host,
                             final String textModels,
                             final String imageModels,
                             final String embeddingsModels) throws DotDataException, DotSecurityException {
        AiTest.aiAppSecrets(wireMockServer, host, textModels, imageModels, embeddingsModels);
        DateUtil.sleep(1000);
    }

    private static void assertSameModels(Optional<AIModel> text3, Optional<AIModel> text1, Optional<AIModel> text2) {
        assertTrue(text3.isPresent());
        assertSame(text1.get(), text3.get());
        assertSame(text2.get(), text3.get());
    }

    private static void assertModels(final Optional<AIModel> model1,
                                     final Optional<AIModel> model2,
                                     final AIModelType type) {
        assertTrue(model1.isPresent());
        assertTrue(model2.isPresent());
        assertSame(model1.get(), model2.get());
        assertSame(type, model1.get().getType());
        assertSame(type, model2.get().getType());
    }

}