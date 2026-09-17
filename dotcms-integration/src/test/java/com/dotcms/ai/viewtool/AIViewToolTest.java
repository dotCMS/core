package com.dotcms.ai.viewtool;

import com.dotcms.IntegrationTestBase;
import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.awaitility.Awaitility;
import org.owasp.encoder.Encode;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
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



    // ------------------------------------------------------------------------------------------
    // #37153: escaped by default, raw only through $ai.unsafe
    // ------------------------------------------------------------------------------------------

    /**
     * Scenario: generateText output is escaped by default and raw through $ai.unsafe (AC-001, AC-003)
     * Given the provider answers the probe prompt with a script and an onerror image tag
     * When generateText is called on $ai (String and Map prompt) and on $ai.unsafe
     * Then $ai returns the HTML-encoded markup with no raw markup anywhere, $ai.unsafe returns the literal
     * markup, and getUnsafe() is idempotent
     */
    @Test
    public void test_generateText_escapedByDefault_rawThroughUnsafe() {
        for (final JSONObject escaped : List.of(
                aiViewTool.generateText(AiTest.PROBE_CHAT_PROMPT),
                aiViewTool.generateText(Map.of("prompt", AiTest.PROBE_CHAT_PROMPT)))) {
            assertEquals(AiTest.PROBE_MARKUP_ESCAPED, AiTest.chatContent(escaped));
            AiTest.assertNoRawMarkup(escaped);
        }

        final AIViewTool unsafe = aiViewTool.getUnsafe();
        assertNotSame(aiViewTool, unsafe);
        assertSame(unsafe, unsafe.getUnsafe());
        assertEquals(AiTest.PROBE_MARKUP, AiTest.chatContent(unsafe.generateText(AiTest.PROBE_CHAT_PROMPT)));
    }

    /**
     * Scenario: generateImage output is escaped by default and raw through $ai.unsafe (AC-001, AC-002, AC-003)
     * Given a prompt that carries markup (the image payload's only text is the echoed prompt: the client
     * rebuilds the provider response as data[].url, so no revised_prompt reaches dotCMS)
     * When generateImage is called on $ai and on $ai.unsafe
     * Then $ai returns originalPrompt HTML-encoded, the url intact and no raw markup anywhere;
     * $ai.unsafe returns originalPrompt literally. Same retry as the existing image tests (async temp file).
     */
    @Test
    public void test_generateImage_escapedByDefault_rawThroughUnsafe() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> {
                    final JSONObject escaped = aiViewTool.generateImage(AiTest.PROBE_IMAGE_PROMPT);
                    assertEquals(Encode.forHtml(AiTest.PROBE_IMAGE_PROMPT), escaped.getString("originalPrompt"));
                    assertEquals("http://localhost:50505/s/ganymede", escaped.getString("url"));
                    AiTest.assertNoRawMarkup(escaped);

                    final JSONObject raw = aiViewTool.getUnsafe().generateImage(AiTest.PROBE_IMAGE_PROMPT);
                    assertEquals(AiTest.PROBE_IMAGE_PROMPT, raw.getString("originalPrompt"));
                    return true;
                });
    }

    /**
     * Scenario: a handled generateImage failure returns an escaped error payload (AC-006)
     * Given a prompt no image stub answers
     * When generateImage is called on $ai
     * Then it returns a payload with an error entry and no raw markup, without throwing
     */
    @Test
    public void test_generateImage_errorPayloadIsEscaped() {
        final JSONObject failed = aiViewTool.generateImage("Escaping probe <b>no such image stub</b> " + System.nanoTime());

        assertNotNull(failed);
        assertTrue("expected an error entry in " + failed, failed.containsKey("error"));
        AiTest.assertNoRawMarkup(failed);
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
