package com.dotcms.ai.client;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AIModels;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.domain.AIResponse;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.domain.ModelStatus;
import com.dotcms.ai.exception.DotAIAllModelsExhaustedException;
import com.dotcms.ai.exception.DotAIClientConnectException;
import com.dotcms.ai.util.LineReadingOutputStream;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the AIProxyClient class.
 *
 * @author vico
 */
public class AIProxyClientTest {

    private static WireMockServer wireMockServer;
    private static User user;
    private Host host;
    private AppConfig appConfig;
    private final AIProxyClient aiProxyClient = AIProxyClient.get();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        final Host systemHost = APILocator.systemHost();
        AiTest.aiAppSecrets(systemHost);
        ConfigService.INSTANCE.config(systemHost);
        user = new UserDataGen().nextPersisted();
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() {
        host = new SiteDataGen().nextPersisted();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Scenario: Calling AI with a valid model
     * Given a valid model "gpt-4o-mini"
     * When the request is sent to the AI service
     * Then the response should contain the model name "gpt-4o-mini"
     */
    @Test
    public void test_callToAI_happiestPath() throws Exception {
        final String model = "gpt-4o-mini";
        AiTest.aiAppSecrets(host, model, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                model,
                "What are the major achievements of the Apollo space program?");

        final AIResponse aiResponse = aiProxyClient.callToAI(request);

        assertNotNull(aiResponse);
        assertNotNull(aiResponse.getResponse());
        assertEquals("gpt-4o-mini", new JSONObject(aiResponse.getResponse()).getString(AiKeys.MODEL));
    }

    /**
     * Scenario: Calling AI with multiple models
     * Given multiple models including "gpt-4o-mini"
     * When the request is sent to the AI service
     * Then the response should contain the model name "gpt-4o-mini"
     */
    @Test
    public void test_callToAI_happyPath_withMultipleModels() throws Exception {
        final String model = "gpt-4o-mini";
        AiTest.aiAppSecrets(
                host,
                String.format("%s,some-made-up-model-1", model),
                "dall-e-3",
                "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                model,
                "What are the major achievements of the Apollo space program?");

        final AIResponse aiResponse = aiProxyClient.callToAI(request);

        assertNotNull(aiResponse);
        assertNotNull(aiResponse.getResponse());
        assertEquals("gpt-4o-mini", new JSONObject(aiResponse.getResponse()).getString(AiKeys.MODEL));
    }

    /**
     * Scenario: Calling AI with an invalid model
     * Given an invalid model "some-made-up-model-10"
     * When the request is sent to the AI service
     * Then a DotAIAllModelsExhaustedException should be thrown
     */
    @Test
    public void test_callToAI_withInvalidModel() throws Exception {
        final String invalidModel = "some-made-up-model-10";
        AiTest.aiAppSecrets(host, invalidModel, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                invalidModel,
                "What are the major achievements of the Apollo space program?");

        assertThrows(DotAIAllModelsExhaustedException.class, () -> aiProxyClient.callToAI(request));
        final Tuple2<AIModel, Model> modelTuple = appConfig.resolveModelOrThrow(invalidModel, AIModelType.TEXT);
        assertSame(ModelStatus.INVALID, modelTuple._2.getStatus());
        assertEquals(-1, modelTuple._1.getCurrentModelIndex());
        assertTrue(AIModels.get()
                .getAvailableModels()
                .stream()
                .noneMatch(model -> model.getName().equals(invalidModel)));
        assertThrows(DotAIAllModelsExhaustedException.class, () -> aiProxyClient.callToAI(request));
    }

    /**
     * Scenario: Calling AI with a decommissioned model
     * Given a decommissioned model "some-decommissioned-model-20"
     * When the request is sent to the AI service
     * Then a DotAIAllModelsExhaustedException should be thrown
     */
    @Test
    public void test_callToAI_withDecommissionedModel() throws Exception {
        final String decommissionedModel = "some-decommissioned-model-20";
        AiTest.aiAppSecrets(host, decommissionedModel, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                decommissionedModel,
                "What are the major achievements of the Apollo space program?");

        assertThrows(DotAIAllModelsExhaustedException.class, () -> aiProxyClient.callToAI(request));
        final Tuple2<AIModel, Model> modelTuple = appConfig.resolveModelOrThrow(decommissionedModel, AIModelType.TEXT);
        assertSame(ModelStatus.DECOMMISSIONED, modelTuple._2.getStatus());
        assertEquals(-1, modelTuple._1.getCurrentModelIndex());
        assertTrue(AIModels.get()
                .getAvailableModels()
                .stream()
                .noneMatch(model -> model.getName().equals(decommissionedModel)));
        assertThrows(DotAIAllModelsExhaustedException.class, () -> aiProxyClient.callToAI(request));
    }

    /**
     * Scenario: Calling AI with multiple models including invalid, decommissioned, and valid models
     * Given models "some-made-up-model-30", "some-decommissioned-model-31", and "gpt-4o-mini"
     * When the request is sent to the AI service
     * Then the response should contain the model name "gpt-4o-mini"
     */
    @Test
    public void test_callToAI_withMultipleModels_invalidAndDecommissionedAndValid() throws Exception {
        final String invalidModel = "some-made-up-model-30";
        final String decommissionedModel = "some-decommissioned-model-31";
        final String validModel = "gpt-4o-mini";
        AiTest.aiAppSecrets(
                host,
                String.format("%s,%s,%s", invalidModel, decommissionedModel, validModel),
                "dall-e-3",
                "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(invalidModel, "What are the major achievements of the Apollo space program?");

        final AIResponse aiResponse = aiProxyClient.callToAI(request);

        assertNotNull(aiResponse);
        assertNotNull(aiResponse.getResponse());
        assertSame(ModelStatus.INVALID, appConfig.resolveModelOrThrow(invalidModel, AIModelType.TEXT)._2.getStatus());
        assertSame(
                ModelStatus.DECOMMISSIONED,
                appConfig.resolveModelOrThrow(decommissionedModel, AIModelType.TEXT)._2.getStatus());
        final Tuple2<AIModel, Model> modelTuple = appConfig.resolveModelOrThrow(validModel, AIModelType.TEXT);
        assertSame(ModelStatus.ACTIVE, modelTuple._2.getStatus());
        assertEquals(2, modelTuple._1.getCurrentModelIndex());
        assertTrue(AIModels.get()
                .getAvailableModels()
                .stream()
                .noneMatch(model -> List.of(invalidModel, decommissionedModel).contains(model.getName())));
        assertTrue(AIModels.get()
                .getAvailableModels()
                .stream()
                .anyMatch(model -> model.getName().equals(validModel)));
        assertEquals("gpt-4o-mini", new JSONObject(aiResponse.getResponse()).getString(AiKeys.MODEL));
    }

    /**
     * Scenario: Calling AI with a valid model and provided output stream
     * Given a valid model "gpt-4o-mini" and a provided output stream
     * When the request is sent to the AI service
     * Then the response should be written to the output stream
     */
    @Test
    public void test_callToAI_withProvidedOutput() throws Exception {
        final String model = "gpt-4o-mini";
        AiTest.aiAppSecrets(host, model, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                model,
                "What are the major achievements of the Apollo space program?");

        final AIResponse aiResponse = aiProxyClient.callToAI(
                request,
                new LineReadingOutputStream(new ByteArrayOutputStream()));
        assertNotNull(aiResponse);
        assertNull(aiResponse.getResponse());
    }

    /**
     * Scenario: Calling AI with an invalid model and provided output stream
     * Given an invalid model "some-made-up-model-40" and a provided output stream
     * When the request is sent to the AI service
     * Then a DotAIAllModelsExhaustedException should be thrown
     */
    @Test
    public void test_callToAI_withInvalidModel_withProvidedOutput() throws Exception {
        final String invalidModel = "some-made-up-model-40";
        AiTest.aiAppSecrets(host, invalidModel, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                invalidModel,
                "What are the major achievements of the Apollo space program?");

        assertThrows(DotAIAllModelsExhaustedException.class, () -> aiProxyClient.callToAI(request));
        final Tuple2<AIModel, Model> modelTuple = appConfig.resolveModelOrThrow(invalidModel, AIModelType.TEXT);
        assertSame(ModelStatus.INVALID, modelTuple._2.getStatus());
        assertEquals(-1, modelTuple._1.getCurrentModelIndex());
        assertTrue(AIModels.get()
                .getAvailableModels()
                .stream()
                .noneMatch(model -> model.getName().equals(invalidModel)));
    }

    /**
     * Scenario: Calling AI with network issues
     * Given a valid model "gpt-4o-mini"
     * And the AI service is unavailable due to network issues
     * When the request is sent to the AI service
     * Then a DotAIClientConnectException should be thrown
     * And the model should remain operational after the network is restored
     */
    @Test
    public void test_callToAI_withNetworkIssues() throws Exception {
        final String model = "gpt-4o-mini";
        AiTest.aiAppSecrets(host, model, "dall-e-3", "text-embedding-ada-002");
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                model,
                "What are the major achievements of the Apollo space program?");

        wireMockServer.stop();

        assertThrows(DotAIClientConnectException.class, () -> aiProxyClient.callToAI(request));

        wireMockServer = AiTest.prepareWireMock();

        final Tuple2<AIModel, Model> modelTuple = appConfig.resolveModelOrThrow(model, AIModelType.TEXT);
        assertTrue(modelTuple._2.isOperational());
    }

    private JSONObjectAIRequest textRequest(final String model, final String prompt) {
        final JSONObject payload = new JSONObject();
        final JSONArray messages = new JSONArray();

        final String systemPrompt = UtilMethods.isSet(appConfig.getRolePrompt()) ? appConfig.getRolePrompt() : null;
        if (UtilMethods.isSet(systemPrompt)) {
            messages.add(Map.of(AiKeys.ROLE, AiKeys.SYSTEM, AiKeys.CONTENT, systemPrompt));
        }
        messages.add(Map.of(AiKeys.ROLE, AiKeys.USER, AiKeys.CONTENT, prompt));

        payload.put(AiKeys.MODEL, model);
        payload.put(AiKeys.TEMPERATURE, appConfig.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE));
        payload.put(AiKeys.MESSAGES, messages);

        return JSONObjectAIRequest.quickText(appConfig, payload, user.getUserId());
    }

}
