package com.dotcms.ai.app;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.model.SimpleModel;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
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
     * Given a set of models loaded into the AIModels instance
     * When the resolveAIModelOrThrow method is called with various model names and types
     * Then the correct models should be resolved and their operational status verified.
     */
    @Test
    public void test_resolveAIModelOrThrow() throws Exception {
        AiTest.aiAppSecrets(host, "text-model-30", "image-model-31", "embeddings-model-32");

        final AppConfig appConfig = ConfigService.INSTANCE.config(host);
        final AIModel aiModel30 = aiModels.resolveAIModelOrThrow(appConfig, "text-model-30", AIModelType.TEXT);
        final AIModel aiModel31 = aiModels.resolveAIModelOrThrow(appConfig, "image-model-31", AIModelType.IMAGE);
        final AIModel aiModel32 = aiModels.resolveAIModelOrThrow(
                appConfig,
                "embeddings-model-32",
                AIModelType.EMBEDDINGS);

        assertNotNull(aiModel30);
        assertNotNull(aiModel31);
        assertNotNull(aiModel32);
        assertEquals("text-model-30", aiModel30.getModel("text-model-30").getName());
        assertEquals("image-model-31", aiModel31.getModel("image-model-31").getName());
        assertEquals("embeddings-model-32", aiModel32.getModel("embeddings-model-32").getName());

        assertThrows(
                DotAIModelNotFoundException.class,
                () -> aiModels.resolveAIModelOrThrow(appConfig, "text-model-33", AIModelType.TEXT));
        assertThrows(
                DotAIModelNotFoundException.class,
                () -> aiModels.resolveAIModelOrThrow(appConfig, "image-model-34", AIModelType.IMAGE));
        assertThrows(
                DotAIModelNotFoundException.class,
                () -> aiModels.resolveAIModelOrThrow(appConfig, "embeddings-model-35", AIModelType.EMBEDDINGS));
    }

    /**
     * Given a set of models loaded into the AIModels instance
     * When the resolveModelOrThrow method is called with various model names and types
     * Then the correct models should be resolved and their operational status verified.
     */
    @Test
    public void test_resolveModelOrThrow() throws Exception {
        AiTest.aiAppSecrets(host, "text-model-40", "image-model-41", "embeddings-model-42");

        final AppConfig appConfig = ConfigService.INSTANCE.config(host);
        final Tuple2<AIModel, Model> modelTuple40 = aiModels.resolveModelOrThrow(
                appConfig,
                "text-model-40",
                AIModelType.TEXT);
        final Tuple2<AIModel, Model> modelTuple41 = aiModels.resolveModelOrThrow(
                appConfig,
                "image-model-41",
                AIModelType.IMAGE);
        final Tuple2<AIModel, Model> modelTuple42 = aiModels.resolveModelOrThrow(
                appConfig,
                "embeddings-model-42",
                AIModelType.EMBEDDINGS);

        assertNotNull(modelTuple40);
        assertNotNull(modelTuple41);
        assertNotNull(modelTuple42);
        assertEquals("text-model-40", modelTuple40._1.getModel("text-model-40").getName());
        assertEquals("image-model-41", modelTuple41._1.getModel("image-model-41").getName());
        assertEquals("embeddings-model-42", modelTuple42._1.getModel("embeddings-model-42").getName());

        assertThrows(
                DotAIModelNotFoundException.class,
                () -> aiModels.resolveAIModelOrThrow(appConfig, "text-model-43", AIModelType.TEXT));
        assertThrows(
                DotAIModelNotFoundException.class,
                () -> aiModels.resolveAIModelOrThrow(appConfig, "image-model-44", AIModelType.IMAGE));
        assertThrows(
                DotAIModelNotFoundException.class,
                () -> aiModels.resolveAIModelOrThrow(appConfig, "embeddings-model-45", AIModelType.EMBEDDINGS));
    }

    private static void assertSameModels(final Optional<AIModel> text3,
                                         final Optional<AIModel> text1,
                                         final Optional<AIModel> text2) {
        assertTrue(text3.isPresent());
        assertSame(text1.get(), text3.get());
        assertSame(text2.get(), text3.get());
    }

    private static void assertModels(final Optional<AIModel> model1,
                                     final Optional<AIModel> model2,
                                     final AIModelType type,
                                     final boolean assertModelNames) {
        assertTrue(model1.isPresent());
        assertTrue(model2.isPresent());
        assertSame(model1.get(), model2.get());
        assertSame(type, model1.get().getType());
        assertSame(type, model2.get().getType());
        if (assertModelNames) {
            assertTrue(model1.get().getModels().stream().allMatch(model -> model.getStatus() == ModelStatus.ACTIVE));
            assertTrue(model2.get().getModels().stream().allMatch(model -> model.getStatus() == ModelStatus.ACTIVE));
        }
    }

    private static void assertNotPresentModels(final Optional<AIModel> model1, final Optional<AIModel> model2) {
        assertTrue(model1.isEmpty());
        assertTrue(model2.isEmpty());
    }

}
