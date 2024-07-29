package com.dotcms.ai.app;

import com.dotcms.ai.AiTest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.AfterClass;
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

    private static final String HOST = "my-host";
    private static final String OTHER_HOST = "other-host";
    private static final int PORT = 50505;

    private static WireMockServer wireMockServer;

    private final AIModels aiModels = AIModels.get();

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

    /**
     * Given a set of models loaded into the AIModels instance
     * When the findModel method is called with various model names and types
     * Then the correct models should be found and returned.
     */
    @Test
    public void test_loadModels_andFindThem() {
        loadModels();

        final Optional<AIModel> notFound = aiModels.findModel(HOST, "some-invalid-model-name");
        assertTrue(notFound.isEmpty());

        final Optional<AIModel> text1 = aiModels.findModel(HOST, "text-model-1");
        assertTrue(text1.isPresent());
        final Optional<AIModel> text2 = aiModels.findModel(HOST, "text-model-2");
        assertTrue(text2.isPresent());
        assertSame(text1.get(), text2.get());
        assertSame(AIModelType.TEXT, text1.get().getType());
        assertSame(AIModelType.TEXT, text2.get().getType());

        final Optional<AIModel> image1 = aiModels.findModel(HOST, "image-model-3");
        assertTrue(image1.isPresent());
        final Optional<AIModel> image2 = aiModels.findModel(HOST, "image-model-4");
        assertTrue(image2.isPresent());
        assertSame(image1.get(), image2.get());
        assertSame(AIModelType.IMAGE, image1.get().getType());
        assertSame(AIModelType.IMAGE, image2.get().getType());

        final Optional<AIModel> embeddings1 = aiModels.findModel(HOST, "embeddings-model-5");
        assertTrue(embeddings1.isPresent());
        final Optional<AIModel> embeddings2 = aiModels.findModel(HOST, "embeddings-model-6");
        assertTrue(embeddings2.isPresent());
        assertSame(embeddings1.get(), embeddings2.get());
        assertSame(AIModelType.EMBEDDINGS, embeddings1.get().getType());
        assertSame(AIModelType.EMBEDDINGS, embeddings2.get().getType());

        assertNotSame(text1.get(), image1.get());
        assertNotSame(text1.get(), embeddings1.get());
        assertNotSame(image1.get(), embeddings1.get());

        final Optional<AIModel> text3 = aiModels.findModel(HOST, AIModelType.TEXT);
        assertTrue(text3.isPresent());
        assertSame(text1.get(), text3.get());
        assertSame(text2.get(), text3.get());

        final Optional<AIModel> image3 = aiModels.findModel(HOST, AIModelType.IMAGE);
        assertTrue(image3.isPresent());
        assertSame(image1.get(), image3.get());
        assertSame(image2.get(), image3.get());

        final Optional<AIModel> embeddings3 = aiModels.findModel(HOST, AIModelType.EMBEDDINGS);
        assertTrue(embeddings3.isPresent());
        assertSame(embeddings1.get(), embeddings3.get());
        assertSame(embeddings2.get(), embeddings3.get());

        final Optional<AIModel> text4 = aiModels.findModel(OTHER_HOST, "text-model-1");
        assertTrue(text3.isPresent());
        assertNotSame(text1.get(), text4.get());
    }

    /**
     * Given a URL for supported models
     * When the getOrPullSupportedModules method is called
     * Then a list of supported models should be returned.
     */
    @Test
    public void test_getOrPullSupportedModules() {
        aiModels.setOpenAiModelsUrl("http://localhost:" + PORT + "/m");

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
        aiModels.setOpenAiModelsUrl("http://localhost:" + PORT + "/zzz");

        final List<String> supported = aiModels.getOrPullSupportedModels();
        assertNotNull(supported);
        assertTrue(supported.isEmpty());
    }

    private void loadModels() {
        aiModels.loadModels(
                HOST,
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
                OTHER_HOST,
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

}
