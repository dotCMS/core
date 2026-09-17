package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.datagen.EmbeddingsDTODataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
public class CompletionsToolTest {

    private static AppConfig appConfig;
    private static User user;
    private static WireMockServer wireMockServer;
    private static Host host;

    /** Title carried by the probe embedding rows; must come back untouched (contentlet field). */
    private static final String PROBE_TITLE = AiTest.PROBE_MARKUP + " title";

    private CompletionsTool completionsTool;
    private CompletionsTool unsafeCompletionsTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        host = new SiteDataGen().nextPersisted();
        wireMockServer = AiTest.prepareWireMock();
        AiTest.aiAppSecretsWithProviderConfig(APILocator.systemHost(), AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));
        appConfig = ConfigService.INSTANCE.config(host);
        user = new UserDataGen().nextPersisted();
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

        completionsTool = prepareCompletionsTool(viewContext);
        unsafeCompletionsTool = prepareCompletionsTool(viewContext, false);
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
        assertAll(result);
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
        final String prompt = String.format("{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"%s?\"},{\"role\":\"system\",\"content\":\"You are a helpful assistant with a descriptive writing style.\"}]}", query);

        final JSONObject result = (JSONObject) completionsTool.raw(prompt);
        assertResult(result);
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
        json.put("model", "gpt-4o-mini");

        final JSONObject result = (JSONObject) completionsTool.raw(json);
        assertResult(result);
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
        final Map<String, Object> map = Map.of("model", "gpt-4o-mini", "messages", new JSONArray()
                .put(new JSONObject().put("role", "user").put("content", query + "?"))
                .put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant with a descriptive writing style")));

        final JSONObject result = (JSONObject) completionsTool.raw(map);
        assertResult(result);
    }


    // ------------------------------------------------------------------------------------------
    // #37153: escaped by default, raw only through the unsafe construction
    // ------------------------------------------------------------------------------------------

    /**
     * Feature: Safe-by-default completions output (#37153, AC-001 / AC-002)
     * Scenario: Summarize with a provider reply that contains active markup
     * Given an embedding row whose extractedText equals the prompt and contains markup, and whose title contains markup
     * And the provider replies with a script and an onerror image tag
     * When summarize is called on the default tool
     * Then the model text, the query echo and the match's extractedText are HTML-escaped
     * And the result title is returned exactly as stored
     */
    @Test
    public void test_summarize_escapesProviderOutputByDefault() {
        final String text = AiTest.PROBE_CHAT_PROMPT + " summarize default " + AiTest.PROBE_MARKUP;
        seedProbeEmbeddings(text);

        final JSONObject result = (JSONObject) completionsTool.summarize(text);

        assertResult(result);
        assertEquals(AiTest.PROBE_MARKUP_ESCAPED, AiTest.summarizeContent(result));
        AiTest.assertNoRawMarkup(result.getJSONObject("openAiResponse"));
        assertEquals(Encode.forHtml(text), result.getString("query"));
        final JSONObject probeResult = AiTest.findResultByTitle(result, PROBE_TITLE);
        assertEquals(PROBE_TITLE, probeResult.getString("title"));
        assertEquals(Encode.forHtml(text),
                probeResult.getJSONArray("matches").getJSONObject(0).getString("extractedText"));
    }

    /**
     * Feature: Explicit opt-in returns today's payload (#37153, AC-003)
     * Scenario: Summarize on the unsafe construction
     * Given the same seeded row and provider reply
     * When summarize is called on a tool built with escaping off
     * Then the payload is what the CompletionsAPI returns, markup intact, apart from the per-call timing value
     */
    @Test
    public void test_summarize_unsafeReturnsRawPayload() {
        final String text = AiTest.PROBE_CHAT_PROMPT + " summarize unsafe " + AiTest.PROBE_MARKUP;
        seedProbeEmbeddings(text);

        final JSONObject unsafeResult = (JSONObject) unsafeCompletionsTool.summarize(text);

        assertResult(unsafeResult);
        assertEquals(AiTest.PROBE_MARKUP, AiTest.summarizeContent(unsafeResult));
        assertEquals(text, unsafeResult.getString("query"));
        assertEquals(text, AiTest.findResultByTitle(unsafeResult, PROBE_TITLE)
                .getJSONArray("matches").getJSONObject(0).getString("extractedText"));

        final JSONObject apiResult = APILocator.getDotAIAPI().getCompletionsAPI(appConfig).summarize(summarizeForm(text));
        AiTest.assertEquivalentIgnoring(apiResult, unsafeResult, Set.of("timeToEmbeddings"));
    }

    /**
     * Feature: Escaping changes text only (#37153, AC-004)
     * Scenario: Compare default and unsafe summarize payloads
     * When both tools summarize the same prompt
     * Then keys, nesting, array lengths and non-string values are identical
     * And string values differ only under openAiResponse, at query and at matches[].extractedText, by exactly Encode.forHtml
     */
    @Test
    public void test_summarize_defaultAndUnsafeHaveSameShape() {
        final String text = AiTest.PROBE_CHAT_PROMPT + " summarize shape " + AiTest.PROBE_MARKUP;
        seedProbeEmbeddings(text);

        final JSONObject unsafeResult = (JSONObject) unsafeCompletionsTool.summarize(text);
        final JSONObject defaultResult = (JSONObject) completionsTool.summarize(text);

        assertResult(unsafeResult);
        assertResult(defaultResult);
        AiTest.assertEscapedOnlyAt(unsafeResult, defaultResult, AiTest.searchShapedEscapedPaths(), Set.of("timeToEmbeddings"));
    }

    /**
     * Feature: Safe-by-default raw completions (#37153, AC-001)
     * Scenario: raw(String), raw(JSONObject) and raw(Map) with a provider reply containing markup
     * When each overload is called on the default tool
     * Then the whole provider payload is escaped: the model text equals the encoded markup and no string contains raw markup
     */
    @Test
    public void test_raw_escapesWholePayloadByDefault() {
        final String prompt = rawPrompt(AiTest.PROBE_CHAT_PROMPT);

        final JSONObject fromString = (JSONObject) completionsTool.raw(prompt);
        final JSONObject fromJson = (JSONObject) completionsTool.raw(new JSONObject(prompt));
        final JSONObject fromMap = (JSONObject) completionsTool.raw(rawPromptMap(AiTest.PROBE_CHAT_PROMPT));

        for (final JSONObject result : List.of(fromString, fromJson, fromMap)) {
            assertResult(result);
            assertEquals(AiTest.PROBE_MARKUP_ESCAPED, AiTest.chatContent(result));
            AiTest.assertNoRawMarkup(result);
        }
    }

    /**
     * Feature: Explicit opt-in for raw completions (#37153, AC-003 / AC-004)
     * Scenario: the three raw overloads on the unsafe construction
     * When each overload is called with escaping off
     * Then the model text is the literal markup, raw(JSONObject) is byte-for-byte the CompletionsAPI result,
     * and the default payload differs from it only by Encode.forHtml on string leaves
     */
    @Test
    public void test_raw_unsafeReturnsRawPayload() {
        final String prompt = rawPrompt(AiTest.PROBE_CHAT_PROMPT);

        final JSONObject fromString = (JSONObject) unsafeCompletionsTool.raw(prompt);
        final JSONObject fromJson = (JSONObject) unsafeCompletionsTool.raw(new JSONObject(prompt));
        final JSONObject fromMap = (JSONObject) unsafeCompletionsTool.raw(rawPromptMap(AiTest.PROBE_CHAT_PROMPT));

        for (final JSONObject result : List.of(fromString, fromJson, fromMap)) {
            assertResult(result);
            assertEquals(AiTest.PROBE_MARKUP, AiTest.chatContent(result));
        }
        final JSONObject apiResult = APILocator.getDotAIAPI().getCompletionsAPI(appConfig)
                .raw(new JSONObject(prompt), UtilMethods.extractUserIdOrNull(user));
        assertEquals(apiResult.toString(), fromJson.toString());

        final JSONObject defaultFromJson = (JSONObject) completionsTool.raw(new JSONObject(prompt));
        AiTest.assertEscapedOnlyAt(fromJson, defaultFromJson, path -> true);
    }

    /**
     * Feature: Error payloads are escaped and never throw (#37153, AC-006)
     * Scenario: provider failure, malformed prompt, and a summarize with no index hits
     * When the default tool is called
     * Then each call returns a payload with an error entry, no string value contains raw markup, and nothing is thrown
     */
    @Test
    public void test_errorPayload_isEscapedAndDoesNotThrow() {
        assertErrorPayloadEscaped(completionsTool.raw(rawPrompt(AiTest.PROBE_FAILURE_PROMPT)));
        assertErrorPayloadEscaped(completionsTool.raw("this is not json <b>at all</b>"));

        final Object noHits = completionsTool.summarize(
                "Escaping probe nohits <b>x</b>", "no-such-index-" + System.nanoTime());
        assertErrorPayloadEscaped(noHits);
        assertFalse(((Map<?, ?>) noHits).containsKey("openAiResponse"));
    }

    /**
     * Feature: The API layer is untouched by the template-side escaping (#37153, AC-006 / AC-008)
     * Scenario: a default summarize call followed by a direct CompletionsAPI call with the same form
     * Then the API still returns the literal markup in the model text, the query and the extractedText,
     * so the REST resources that return these objects are unaffected
     */
    @Test
    public void test_defaultCall_leavesApiLayerUnescaped() {
        final String text = AiTest.PROBE_CHAT_PROMPT + " summarize api " + AiTest.PROBE_MARKUP;
        seedProbeEmbeddings(text);

        final JSONObject escaped = (JSONObject) completionsTool.summarize(text);
        assertEquals(AiTest.PROBE_MARKUP_ESCAPED, AiTest.summarizeContent(escaped));

        final JSONObject apiResult = APILocator.getDotAIAPI().getCompletionsAPI(appConfig).summarize(summarizeForm(text));
        assertEquals(AiTest.PROBE_MARKUP, AiTest.summarizeContent(apiResult));
        assertEquals(text, apiResult.getString("query"));
        assertEquals(text, AiTest.findResultByTitle(apiResult, PROBE_TITLE)
                .getJSONArray("matches").getJSONObject(0).getString("extractedText"));
    }

    // ------------------------------------------------------------------------------ helpers

    /**
     * Persists one embedding row whose extractedText is exactly {@code text} (so the query reuses its
     * vector and the search finds it) and whose title carries markup that must come back untouched.
     */
    private static void seedProbeEmbeddings(final String text) {
        // explicit inode: the data-gen default derives it from the current millisecond, and two rows
        // sharing an inode are merged into one result whose matches[0] may then belong to another test
        final String inode = "probe-" + UUIDGenerator.generateUuid();
        new EmbeddingsDTODataGen().generate(inode, "default", text).withTitle(PROBE_TITLE).nextPersisted();
    }

    private static CompletionsForm summarizeForm(final String text) {
        return new CompletionsForm.Builder().indexName("default").prompt(text).user(user).build();
    }

    private static String rawPrompt(final String userMessage) {
        return String.format(
                "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                userMessage);
    }

    private static Map<String, Object> rawPromptMap(final String userMessage) {
        return Map.of("model", "gpt-4o-mini", "messages", new JSONArray()
                .put(new JSONObject().put("role", "user").put("content", userMessage)));
    }

    private static void assertErrorPayloadEscaped(final Object payload) {
        assertNotNull(payload);
        assertTrue("expected a Map payload but got " + payload.getClass(), payload instanceof Map);
        assertTrue("expected an error entry in " + payload, ((Map<?, ?>) payload).containsKey("error"));
        AiTest.assertNoRawMarkup(payload);
    }

    private CompletionsTool prepareCompletionsTool(final ViewContext viewContext, final boolean escapeOutput) {
        return new CompletionsTool(viewContext, escapeOutput) {
            @Override
            Host host() {
                return host;
            }

            @Override
            AppConfig config() {
                return appConfig;
            }

            @Override
            User user() {
                return user;
            }
        };
    }

    private CompletionsTool prepareCompletionsTool(final ViewContext viewContext) {
        return new CompletionsTool(viewContext) {
            @Override
            Host host() {
                return host;
            }

            @Override
            AppConfig config() {
                return appConfig;
            }

            @Override
            User user() {
                return user;
            }
        };
    }

    private static void assertResult(final JSONObject result) {
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
    }

    private static void assertResponse(final JSONObject result) {
        assertNotNull(result.getString("openAiResponse"));
        final JSONObject openAiResponse = result.getJSONObject("openAiResponse");
        assertFalse(openAiResponse.getJSONArray("choices").isEmpty());
        final JSONObject choice = openAiResponse.getJSONArray("choices").getJSONObject(0);
        assertTrue(choice.containsKey("message"));
        final JSONObject message = choice.getJSONObject("message");
        assertTrue(StringUtils.isNotBlank(message.getString("content")));
    }

    private static void assertAll(final JSONObject result) {
        assertResult(result);
        assertResponse(result);
    }

}
