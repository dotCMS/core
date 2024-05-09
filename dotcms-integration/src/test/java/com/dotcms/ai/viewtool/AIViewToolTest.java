package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.WireMockTestHelper;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * This class tests the functionality of the AIViewTool class.
 * It uses a mock server to simulate the OpenAI API and checks the responses of the AIViewTool methods.
 *
 * The class follows the Gherkin style for test documentation, with each test method representing a scenario.
 * Each scenario is described in terms of "Given", "When", and "Then" steps.
 *
 * @author vico
 */
public class AIViewToolTest {

    private static final String API_URL = "http://localhost:%d/c";
    private static final String API_IMAGE_URL = "http://localhost:%d/i";
    private static final String API_KEY = "some-api-key-1a2bc3";
    private static final String MODEL = "gpt-3.5-turbo-16k";
    private static final String IMAGE_MODEL = "dall-e-3";
    private static final String IMAGE_SIZE = "1024x1024";
    private static final int PORT = 50505;

    private static AppConfig config;
    private static WireMockServer wireMockServer;

    private User user;
    private AIViewTool aiViewTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = prepareWireMock();
        config = prepareConfig();
    }

    @AfterClass
    public static void tearDown() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void setup() {
        user = new UserDataGen().nextPersisted();
        aiViewTool = prepareAIViewTool(mock(ViewContext.class));
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
        assertChatResponse(response, "Club Atletico Boca Juniors");
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
        assertChatResponse(
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
        // when
        final JSONObject response = aiViewTool.generateImage(prompt);
        // then
        assertImageResponse(response, prompt, "ganymede");
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
        // when
        final JSONObject response = aiViewTool.generateImage(prompt);
        // then
        assertImageResponse(response, prompt.get("prompt").toString(), "dalailama");
    }

    private void assertChatResponse(final JSONObject response, final String containedText) {
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

    private AIViewTool prepareAIViewTool(final ViewContext viewContext) {
        return new AIViewTool(viewContext) {
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

    private static AppConfig prepareConfig() {
        return new AppConfig(
            Map.of(
                AppKeys.API_URL.key,
                Secret.builder()
                    .withType(Type.STRING)
                    .withValue(String.format(API_URL, wireMockServer.port()).toCharArray())
                    .build(),

                AppKeys.API_IMAGE_URL.key,
                Secret.builder()
                    .withType(Type.STRING)
                    .withValue(String.format(API_IMAGE_URL, wireMockServer.port()).toCharArray())
                    .build(),

                AppKeys.API_KEY.key,
                Secret.builder().withType(Type.STRING).withValue(API_KEY.toCharArray()).build(),

                AppKeys.MODEL.key,
                Secret.builder().withType(Type.STRING).withValue(MODEL.toCharArray()).build(),

                AppKeys.IMAGE_MODEL.key,
                Secret.builder().withType(Type.STRING).withValue(IMAGE_MODEL.toCharArray()).build(),

                AppKeys.IMAGE_SIZE.key,
                Secret.builder().withType(Type.SELECT).withValue(IMAGE_SIZE.toCharArray()).build()));
    }

    private static WireMockServer prepareWireMock() {
        final WireMockServer wireMockServer = WireMockTestHelper.wireMockServer(PORT);
        wireMockServer.start();

        return wireMockServer;
    }

}
