package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.datagen.EmbeddingsDTODataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains unit tests for the CompletionsTool class.
 * It tests various functionalities provided by the CompletionsTool class such as getting the configuration,
 * summarizing content, and processing raw prompts.
 * It uses mock objects and a WireMock server to simulate the behavior of external dependencies.
 *
 * @author vico
 */
public class CompletionsToolTest implements AiTest {

    private static AppConfig config;
    private static WireMockServer wireMockServer;

    private Host host;
    private CompletionsTool completionsTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        config = AiTest.prepareCompletionConfig(wireMockServer);
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() {
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(mock(HttpServletRequest.class));
        host = new SiteDataGen().nextPersisted();
        completionsTool = prepareCompletionsTool(viewContext);
    }

    /**
     * Feature: CompletionsTool Configuration
     * Scenario: Get Configuration
     * Given a CompletionsTool instance
     * When the getConfig method is called
     * Then it should return a non-null configuration map
     * And the map should contain the correct default values
     */
    @Test
    public void test_getConfig() {
        final Map<String, String> config = completionsTool.getConfig();

        assertNotNull(config);
        assertEquals(AppKeys.COMPLETION_ROLE_PROMPT.defaultValue, config.get(AppKeys.COMPLETION_ROLE_PROMPT.key));
        assertEquals(AppKeys.COMPLETION_TEXT_PROMPT.defaultValue, config.get(AppKeys.COMPLETION_TEXT_PROMPT.key));
        assertEquals(AppKeys.MODEL.defaultValue, config.get(AppKeys.MODEL.key));
    }

    /**
     * Feature: CompletionsTool Summarization
     * Scenario: Summarize Content
     * Given a CompletionsTool instance
     * And a query "Is AI the future"
     * When the summarize method is called with the query
     * Then it should return a non-null JSONObject
     * And the JSONObject should not contain an "error" key
     */
    @Test
    public void test_summarize() {
        final String query = "Is AI the future";
        EmbeddingsDTODataGen.persistEmbeddings(query, null, "default", 1);

        final JSONObject result = (JSONObject) completionsTool.summarize(query);
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
    }

    /**
     * Feature: CompletionsTool Raw Processing
     * Scenario: Process Raw Prompt
     * Given a CompletionsTool instance
     * And a prompt "What is the speed of light in the vacuum"
     * When the raw method is called with the prompt
     * Then it should return a non-null JSONObject
     * And the JSONObject should not contain an "error" key
     */
    @Test
    public void test_raw() {
        final String query = "What is the speed of light in the vacuum";
        final String prompt = String.format("{\"model\":\"gpt-3.5-turbo-16k\",\"messages\":[{\"role\":\"user\",\"content\":\"%s?\"},{\"role\":\"system\",\"content\":\"You are a helpful assistant with a descriptive writing style.\"}]}", query);

        final JSONObject result = (JSONObject) completionsTool.raw(prompt);
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
    }

    /**
     * Feature: CompletionsTool Raw Processing
     * Scenario: Process Raw JSON
     * Given a CompletionsTool instance
     * And a JSON object with a query "Should I buy a Tesla electric vehicle"
     * When the raw method is called with the JSON object
     * Then it should return a non-null JSONObject
     * And the JSONObject should not contain an "error" key
     */
    @Test
    public void test_raw_json() {
        final String query = "Should I buy a Tesla electric vehicle";
        final JSONObject json = new JSONObject();
        final JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", query + "?"));
        messages.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant with a descriptive writing style"));
        json.put("messages", messages);
        json.put("model", "gpt-3.5-turbo-16k");

        final JSONObject result = (JSONObject) completionsTool.raw(json);
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
    }

    /**
     * Feature: CompletionsTool Raw Processing
     * Scenario: Process Raw Map
     * Given a CompletionsTool instance
     * And a Map with a query "Who was the first president of the United States"
     * When the raw method is called with the Map
     * Then it should return a non-null JSONObject
     * And the JSONObject should not contain an "error" key
     */
    @Test
    public void test_raw_map() {
        final String query = "Who was the first president of the United States";
        final Map<String, Object> map = Map.of("model", "gpt-3.5-turbo-16k", "messages", new JSONArray()
                .put(new JSONObject().put("role", "user").put("content", query + "?"))
                .put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant with a descriptive writing style")));

        final JSONObject result = (JSONObject) completionsTool.raw(map);
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
    }

    private CompletionsTool prepareCompletionsTool(final ViewContext viewContext) {
        return new CompletionsTool(viewContext) {
            @Override
            Host host() {
                return host;
            }

            @Override
            AppConfig config() {
                return config;
            }
        };
    }

}
