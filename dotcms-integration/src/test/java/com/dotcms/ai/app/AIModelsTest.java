package com.dotcms.ai.app;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.domain.ModelStatus;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.ai.model.SimpleModel;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
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
     * When the findModel method is called with various model names and types
     * Then the correct models should be found and returned.
     */
    @Test
    public void test_loadModels_andFindThem() throws Exception {
        AiTest.aiAppSecrets(
                    host,
                    "text-model-1,text-model-2",
                    "image-model-3,image-model-4",
                    "embeddings-model-5,embeddings-model-6");
        AiTest.aiAppSecrets(otherHost, "text-model-1", null, null);

        final String hostId = host.getHostname();
        final AppConfig appConfig = ConfigService.INSTANCE.config(host);

        final Optional<AIModel> notFound = aiModels.findModel(appConfig, "some-invalid-model-name", AIModelType.TEXT);
        assertTrue(notFound.isEmpty());

        final Optional<AIModel> text1 = aiModels.findModel(appConfig, "text-model-1", AIModelType.TEXT);
        final Optional<AIModel> text2 = aiModels.findModel(appConfig, "text-model-2", AIModelType.TEXT);
        assertModels(text1, text2, AIModelType.TEXT, true);

        final Optional<AIModel> image1 = aiModels.findModel(appConfig, "image-model-3", AIModelType.IMAGE);
        final Optional<AIModel> image2 = aiModels.findModel(appConfig, "image-model-4", AIModelType.IMAGE);
        assertModels(image1, image2, AIModelType.IMAGE, true);

        final Optional<AIModel> embeddings1 = aiModels.findModel(appConfig, "embeddings-model-5", AIModelType.EMBEDDINGS);
        final Optional<AIModel> embeddings2 = aiModels.findModel(appConfig, "embeddings-model-6", AIModelType.EMBEDDINGS);
        assertModels(embeddings1, embeddings2, AIModelType.EMBEDDINGS, true);

        assertNotSame(text1.get(), image1.get());
        assertNotSame(text1.get(), embeddings1.get());
        assertNotSame(image1.get(), embeddings1.get());

        final Optional<AIModel> text3 = aiModels.findModel(hostId, AIModelType.TEXT);
        assertSameModels(text3, text1, text2);

        final Optional<AIModel> image3 = aiModels.findModel(hostId, AIModelType.IMAGE);
        assertSameModels(image3, image1, image2);

        final Optional<AIModel> embeddings3 = aiModels.findModel(hostId, AIModelType.EMBEDDINGS);
        assertSameModels(embeddings3, embeddings1, embeddings2);

        final AppConfig otherAppConfig = ConfigService.INSTANCE.config(otherHost);
        final Optional<AIModel> text4 = aiModels.findModel(otherAppConfig, "text-model-1", AIModelType.TEXT);
        assertTrue(text3.isPresent());
        assertNotSame(text1.get(), text4.get());

        AiTest.aiAppSecrets(
                host,
                "text-model-7,text-model-8",
                "image-model-9,image-model-10",
                "embeddings-model-11, embeddings-model-12");

        final Optional<AIModel> text7 = aiModels.findModel(otherAppConfig, "text-model-7", AIModelType.TEXT);
        final Optional<AIModel> text8 = aiModels.findModel(otherAppConfig, "text-model-8", AIModelType.TEXT);
        assertNotPresentModels(text7, text8);

        final Optional<AIModel> image9 = aiModels.findModel(otherAppConfig, "image-model-9", AIModelType.IMAGE);
        final Optional<AIModel> image10 = aiModels.findModel(otherAppConfig, "image-model-10", AIModelType.IMAGE);
        assertNotPresentModels(image9, image10);

        final Optional<AIModel> embeddings11 = aiModels.findModel(otherAppConfig, "embeddings-model-11", AIModelType.EMBEDDINGS);
        final Optional<AIModel> embeddings12 = aiModels.findModel(otherAppConfig, "embeddings-model-12", AIModelType.EMBEDDINGS);
        assertNotPresentModels(embeddings11, embeddings12);

        final List<SimpleModel> available = aiModels.getAvailableModels();
        final List<String> availableNames = List.of(
                "gpt-3.5-turbo-16k", "dall-e-3", "text-embedding-ada-002",
                "text-model-1", "text-model-7", "text-model-8",
                "image-model-9", "image-model-10",
                "embeddings-model-11", "embeddings-model-12");
        assertTrue(available.stream().anyMatch(model -> availableNames.contains(model.getName())));
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
