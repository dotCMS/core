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

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        AiTest.aiAppSecrets(systemHost, "gpt-4o-mini", "dall-e-3", "text-embedding-ada-002");
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
