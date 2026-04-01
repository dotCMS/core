package com.dotcms.ai.client;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.domain.AIResponse;
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
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Integration tests for the AIProxyClient class.
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
     * Scenario: Calling AI with gpt-4o-mini via providerConfig
     * Given a providerConfig pointing to a WireMock server with model "gpt-4o-mini"
     * When the request is sent to the AI service
     * Then the response should contain the model name "gpt-4o-mini"
     */
    @Test
    public void test_callToAI_withGpt4oMini_happiestPath() throws Exception {
        final String model = "gpt-4o-mini";
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, model));
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
     * Scenario: Calling AI with gpt-4o-mini and a provided output stream
     * Given a providerConfig with model "gpt-4o-mini" and an output stream
     * When the request is sent to the AI service
     * Then the response object should have a null body (written to stream instead)
     */
    @Test
    public void test_callToAI_withGpt4oMini_andProvidedOutput() throws Exception {
        final String model = "gpt-4o-mini";
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, model));
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
     * Scenario: Calling AI with gpt-4o-mini when the AI service is unavailable
     * Given a providerConfig with model "gpt-4o-mini"
     * And the AI service is unavailable due to network issues
     * When the request is sent to the AI service
     * Then a RuntimeException should be thrown
     */
    @Test
    public void test_callToAI_withGpt4oMini_andNetworkIssues() throws Exception {
        final String model = "gpt-4o-mini";
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, model));
        appConfig = ConfigService.INSTANCE.config(host);
        final JSONObjectAIRequest request = textRequest(
                model,
                "What are the major achievements of the Apollo space program?");

        wireMockServer.stop();

        assertThrows(RuntimeException.class, () -> aiProxyClient.callToAI(request));

        wireMockServer = AiTest.prepareWireMock();
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
