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
    public void before() throws Exception {
        host = new SiteDataGen().nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(
                host,
                AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));
        appConfig = ConfigService.INSTANCE.config(host);
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Scenario: Calling AI with a valid providerConfig pointing to WireMock
     * Given a providerConfig with model "gpt-4o-mini" and endpoint on WireMock
     * When the request is sent
     * Then the response should contain a non-null JSON with model name "gpt-4o-mini"
     */
    @Test
    public void test_callToAI_withGpt4oMini_happiestPath() {
        final JSONObjectAIRequest request = textRequest(
                "gpt-4o-mini",
                "What are the major achievements of the Apollo space program?");

        final AIResponse aiResponse = aiProxyClient.callToAI(request);

        assertNotNull(aiResponse);
        assertNotNull(aiResponse.getResponse());
        assertEquals("gpt-4o-mini", new JSONObject(aiResponse.getResponse()).getString(AiKeys.MODEL));
    }

    /**
     * Scenario: Calling AI with a provided output stream
     * Given a valid providerConfig and an output stream
     * When the request is sent
     * Then the response body goes to the stream; AIResponse.getResponse() is null
     */
    @Test
    public void test_callToAI_withGpt4oMini_andProvidedOutput() {
        final JSONObjectAIRequest request = textRequest(
                "gpt-4o-mini",
                "What are the major achievements of the Apollo space program?");

        final AIResponse aiResponse = aiProxyClient.callToAI(
                request,
                new LineReadingOutputStream(new ByteArrayOutputStream()));

        assertNotNull(aiResponse);
        assertNull(aiResponse.getResponse());
    }

    /**
     * Scenario: Network issues
     * Given WireMock is stopped
     * When the request is sent
     * Then a RuntimeException is thrown (LangChain4J wraps the connection error)
     */
    @Test
    public void test_callToAI_withGpt4oMini_andNetworkIssues() {
        final JSONObjectAIRequest request = textRequest(
                "gpt-4o-mini",
                "What are the major achievements of the Apollo space program?");

        wireMockServer.stop();

        try {
            assertThrows(RuntimeException.class, () -> aiProxyClient.callToAI(request));
        } finally {
            wireMockServer = AiTest.prepareWireMock();
        }
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
