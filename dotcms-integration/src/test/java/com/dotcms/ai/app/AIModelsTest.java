package com.dotcms.ai.app;

import com.dotcms.ai.AiTest;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.github.tomakehurst.wiremock.WireMockServer;
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
    }

    /**
     * Given a set of models loaded into the AIModels instance
     * When the findModel method is called with various model names and types
     * Then the correct models should be found and returned.
     */
    @Test
    public void test_loadModels_andFindThem() {
        loadModels();

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
        assertTrue(embeddings1.isPresent());
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
    }

    /**
     * Given a set of models loaded into the AIModels instance
     * When the resetModels method is called with a specific host
     * Then the models for that host should be reset and no longer found.
     */
    @Test
    public void test_resetModels() {
        loadModels();
        final Optional<AIModel> aiModel = aiModels.findModel(host.getHostname(), AIModelType.TEXT);

        aiModels.resetModels(host);

        assertNotSame(aiModel.get(), aiModels.findModel(host.getHostname(), AIModelType.TEXT));
        assertTrue(aiModels.findModel(host.getHostname(), "text-model-1").isEmpty());
    }

    /**
     * Given a URL for supported models
     * When the getOrPullSupportedModules method is called
     * Then a list of supported models should be returned.
     */
    @Test
    public void test_getOrPullSupportedModules() {
        final List<String> supported = aiModels.getOrPullSupportedModels();
        assertNotNull(supported);
        assertEquals(32, supported.size());
    }

    /**
     * Given an invalid URL for supported models
     * When the getOrPullSupportedModules method is called
     * Then an empty list of supported models should be returned.
     */
    @Test
    public void test_getOrPullSupportedModules_invalidEndpoint() {
        IPUtils.disabledIpPrivateSubnet(false);

        final List<String> supported = aiModels.getOrPullSupportedModels();
        assertNotNull(supported);
        assertTrue(supported.isEmpty());

        IPUtils.disabledIpPrivateSubnet(true);
    }

    private void loadModels() {
        aiModels.loadModels(
                host.getHostname(),
                List.of(
                        AIModel.builder()
                                .withType(AIModelType.TEXT)
                                .withNames("text-model-1", "text-model-2")
                                .withTokensPerMinute(123)
                                .withApiPerMinute(321)
                                .withMaxTokens(555)
                                .withIsCompletion(true)
                                .build(),
                        AIModel.builder()
                                .withType(AIModelType.IMAGE)
                                .withNames("image-model-3", "image-model-4")
                                .withTokensPerMinute(111)
                                .withApiPerMinute(222)
                                .withMaxTokens(333)
                                .withIsCompletion(false)
                                .build(),
                        AIModel.builder()
                                .withType(AIModelType.EMBEDDINGS)
                                .withNames("embeddings-model-5", "embeddings-model-6")
                                .withTokensPerMinute(999)
                                .withApiPerMinute(888)
                                .withMaxTokens(777)
                                .withIsCompletion(false)
                                .build()));
        aiModels.loadModels(
                otherHost.getHostname(),
                List.of(
                        AIModel.builder()
                                .withType(AIModelType.TEXT)
                                .withNames("text-model-1")
                                .withTokensPerMinute(123)
                                .withApiPerMinute(321)
                                .withMaxTokens(555)
                                .withIsCompletion(true)
                                .build()));
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
