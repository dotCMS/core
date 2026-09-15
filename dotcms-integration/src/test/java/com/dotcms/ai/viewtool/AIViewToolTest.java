package com.dotcms.ai.viewtool;

import com.dotcms.IntegrationTestBase;
import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.awaitility.Awaitility;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * This class tests the functionality of the AIViewTool class.
 * It uses a mock server to simulate the OpenAI API and checks the responses of the AIViewTool methods.
 * The class follows the Gherkin style for test documentation, with each test method representing a scenario.
 * Each scenario is described in terms of "Given", "When", and "Then" steps.
 *
 * @author vico
 */
public class AIViewToolTest extends IntegrationTestBase {

    private static AppConfig config;
    private static WireMockServer wireMockServer;

    private User user;
    private AIViewTool aiViewTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        final Host systemHost = APILocator.systemHost();
        AiTest.aiAppSecretsWithProviderConfig(systemHost, AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));
        config = ConfigService.INSTANCE.config(systemHost);
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void setup() {
        user = new UserDataGen().nextPersisted();
        aiViewTool = prepareAIViewTool();
        aiViewTool.init(mock(ViewContext.class));
    }

    /**
     * Scenario: Generate text from a string prompt
     * Given a string prompt about Club Atletico Boca Juniors
     * When the generateText method is called with the prompt
     * Then the response should contain a text about Club Atletico Boca Juniors
     */
    @Test
    public void test_generateText_fromStringPrompt() {
        // given
        final String prompt = "Short text about Club Atletico Boca Juniors";
        // when
        final JSONObject response = aiViewTool.generateText(prompt);
        // then
        assertTextResponse(response, "Club Atletico Boca Juniors");
    }

    /**
     * Scenario: Generate text from a map prompt
     * Given a map prompt about Theory of Chaos
     * When the generateText method is called with the prompt
     * Then the response should contain a text about Theory of Chaos
     */
    @Test
    public void test_generateText_fromMapPrompt() {
        // given
        final Map<String, Object> prompt = Map.of("prompt", "Short text about Theory of Chaos");
        // when
        final JSONObject response = aiViewTool.generateText(prompt);
        // then
        assertTextResponse(
                response,
                "The Theory of Chaos is a scientific concept that suggests the universe is inherently unpredictable");
    }

    /**
     * Scenario: Generate image from a string prompt
     * Given a string prompt about Jupiter moon Ganymede
     * When the generateImage method is called with the prompt
     * Then the response should contain an image about Jupiter moon Ganymede
     */
    @Test
    public void test_generateImage_fromStringPrompt() {
        // given
        final String prompt = "Image about Jupiter moon Ganymede";
        // when & then - wait for image generation to complete asynchronously with retry
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)  // Check every 2 seconds
                .pollDelay(1, TimeUnit.SECONDS)     // Wait 1 second before first attempt
                .ignoreExceptions()                 // Continue retrying on exceptions
                .until(() -> {
                    final JSONObject response = aiViewTool.generateImage(prompt);
                    assertImageResponse(response, prompt, "ganymede");
                    return true; // Only return true if all assertions pass
                });
    }

    /**
     * Scenario: Generate image from a map prompt
     * Given a map prompt about Dalai Lama winning a slam dunk contest
     * When the generateImage method is called with the prompt
     * Then the response should contain an image of Dalai Lama winning a slam dunk contest
     */
    @Test
    public void test_generateImage_fromMapPrompt() {
        // given
        final Map<String, Object> prompt = Map.of("prompt", "Image of Dalai Lama winning a slam dunk contest");
        // when & then - wait for image generation to complete asynchronously with retry
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)  // Check every 2 seconds
                .pollDelay(1, TimeUnit.SECONDS)     // Wait 1 second before first attempt
                .ignoreExceptions()                 // Continue retrying on exceptions
                .until(() -> {
                    final JSONObject response = aiViewTool.generateImage(prompt);
                    assertImageResponse(response, prompt.get("prompt").toString(), "dalailama");
                    return true; // Only return true if all assertions pass
                });
    }

    /**
     * Scenario: Generate image from a string prompt when the provider fails (#37154)
     * Given a prompt carrying the image-failure sentinel, so the mocked provider answers HTTP 500
     * When the generateImage method is called with the prompt
     * Then the provider was called with that prompt
     * And the response is the generic error payload: one "error" key, the fixed message, nothing
     *     derived from the exception
     */
    @Test
    public void test_generateImage_string_providerFailure_returnsGenericError() {
        final String prompt = AiTest.FORCE_IMAGE_ERROR + " " + UUID.randomUUID();

        final JSONObject response = aiViewTool.generateImage(prompt);

        assertImageProviderWasCalledWith(prompt);
        assertGenericErrorPayload(response);
    }

    /**
     * Scenario: Generate image from a map prompt when the provider fails (#37154)
     * Given a map prompt carrying the image-failure sentinel
     * When the generateImage method is called with the map
     * Then the provider was called with that prompt
     * And the response is the generic error payload
     */
    @Test
    public void test_generateImage_map_providerFailure_returnsGenericError() {
        final String prompt = AiTest.FORCE_IMAGE_ERROR + " " + UUID.randomUUID();

        final JSONObject response = aiViewTool.generateImage(Map.of("prompt", prompt));

        assertImageProviderWasCalledWith(prompt);
        assertGenericErrorPayload(response);
    }

    /**
     * Scenario: Generate image from a map with no prompt (#37154)
     * Given a map prompt without the "prompt" key
     * When the generateImage method is called with the map
     * Then the image API rejects it before any provider call
     * And the response is the generic error payload, not the API's own message, which today echoes
     *     the request body
     */
    @Test
    public void test_generateImage_map_missingPrompt_returnsGenericError() {
        final JSONObject response = aiViewTool.generateImage(Map.of("size", "1024x1024"));

        assertGenericErrorPayload(response);
    }

    /**
     * Scenario: generateText under a provider failure, rendered by Velocity for a live request
     *           (#37154, AC-005)
     * Given a prompt carrying the chat-failure sentinel, so the mocked provider answers HTTP 500
     * And no HTTP request is bound to the thread, which the platform's method-exception handler
     *     treats as a non-edit-mode render (it falls back to scanning the stack for a method named
     *     like "EditMode"; this test's name deliberately contains none)
     * When a template calls $!ai.generateText(...) and $ai.generateText(...) and is evaluated
     * Then the provider was called with the prompt
     * And the quiet reference renders empty, the loud one renders its own source text,
     * And the output carries no exception text, class name, frame or provider detail.
     *
     * generateText is intentionally not routed through AIViewToolErrorHandler (it rethrows), so
     * this test pins the platform behaviour the spec relies on for that method rather than a
     * change made by #37154.
     */
    @Test
    public void test_generateText_providerFailure_liveRender_showsNoExceptionDetail() throws Exception {
        final String prompt = AiTest.FORCE_CHAT_ERROR + " " + UUID.randomUUID();
        final Context context = new VelocityContext();
        context.put("ai", aiViewTool);
        final String template = "quiet:[$!ai.generateText(\"" + prompt + "\")] loud:[$ai.generateText(\"" + prompt + "\")]";

        final String rendered = VelocityUtil.eval(template, context);

        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/chat/completions"))
                .withRequestBody(containing(prompt)));
        assertTrue("quiet reference must render empty, got: " + rendered, rendered.contains("quiet:[]"));
        assertTrue("loud reference must render its own source text, got: " + rendered,
                rendered.contains("loud:[$ai.generateText(\"" + prompt + "\")]"));
        for (final String marker : new String[] {"Exception", "\tat ", ".java:", "com.dotcms", "langchain4j",
                "forced provider failure"}) {
            assertFalse("rendered page leaks internal detail (" + marker.trim() + "): " + rendered,
                    rendered.contains(marker));
        }
    }

    /**
     * The failure payload must be exactly what {@link AIViewToolErrorHandler} builds: a single
     * "error" key with the fixed message. Comparing against the constant, rather than only checking
     * for the absence of trace markers, is what makes the missing-prompt case fail before the fix,
     * because the API's message there carries no class name yet still echoes request detail.
     */
    private static void assertGenericErrorPayload(final JSONObject response) {
        AiTest.assertSafeErrorPayload(response);
        assertEquals(AIViewToolErrorHandler.GENERIC_ERROR_MESSAGE, response.getString("error"));
    }

    /**
     * Proves the forced failure really came from the provider: the mocked image endpoint must have
     * received a request carrying this test's unique prompt.
     */
    private static void assertImageProviderWasCalledWith(final String prompt) {
        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/images/generations"))
                .withRequestBody(containing(prompt)));
    }

    private void assertTextResponse(final JSONObject response, final String containedText) {
        assertNotNull(response);
        assertTrue(response.containsKey("choices"));
        assertFalse(response.getJSONArray("choices").isEmpty());
        assertTrue(response.getJSONArray("choices").getJSONObject(0).containsKey("message"));
        assertTrue(response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .containsKey("content"));
        assertTrue(response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .contains(containedText));
    }

    private void assertImageResponse(final JSONObject response, final String prompt, final String path) {
        assertNotNull(response);
        assertEquals(prompt, response.getString("originalPrompt"));
        assertEquals("http://localhost:50505/s/" + path, response.getString("url"));
        assertTrue(response.containsKey("tempFileName"));
        assertTrue(wasFileSaved(response));
    }

    private boolean wasFileSaved(final JSONObject response) {
        return new File(response.getString("tempFile")).exists();
    }

    private AIViewTool prepareAIViewTool() {
        return new AIViewTool() {
            @Override
            User user() {
                return user;
            }

            @Override
            AppConfig config() {
                return config;
            }
        };
    }

}
