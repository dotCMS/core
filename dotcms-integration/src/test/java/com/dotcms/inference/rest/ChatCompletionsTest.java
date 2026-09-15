package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.FunctionCallView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.FunctionDefView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.MessageView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.ToolCallView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.ToolView;
import com.dotcms.inference.rest.view.ChatCompletionView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Specifies the tool-calling round trip through
 * {@link ChatCompletionsResource#completions(HttpServletRequest, HttpServletResponse, String, ChatCompletionRequestView)}.
 *
 * <p>The round trip is the whole point of the tool contract and it cannot be verified one half at
 * a time: a {@code tool_calls} answer is only useful if the id it carries is accepted back on the
 * next turn, and an id that is accepted back is only safe if an uncorrelated one is refused.
 * The four tests here cover exactly that arc.</p>
 *
 * <ul>
 *     <li>FR-004 — tools declared in the request produce {@code tool_calls} in the response, with
 *     an id, a function name and its arguments.</li>
 *     <li>FR-005 — a follow-up turn replaying the assistant tool call plus a {@code role:"tool"}
 *     result carrying that id is accepted and answered.</li>
 *     <li>A {@code role:"tool"} turn whose {@code tool_call_id} correlates with nothing is a
 *     client error, refused before the provider is ever contacted.</li>
 *     <li>FR-024 — {@code model} is required; there is no implicit default.</li>
 * </ul>
 *
 * <p>The provider is a WireMock server standing in for an OpenAI-compatible endpoint, wired in
 * through the same dotAI app secrets the rest of the AI integration tests use, so the exchange
 * travels the real client path rather than a stubbed one.</p>
 */
public class ChatCompletionsTest {

    /** The chat model the site is configured with, and the only one these tests ask for. */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    /** Path an OpenAI-compatible provider serves completions on. */
    private static final String COMPLETIONS_PATH = "/chat/completions";

    /** The tool the request declares and the stubbed provider asks to run. */
    private static final String TOOL_NAME = "get_weather";

    /** The id the stubbed provider mints for its tool call, and that the follow-up replays. */
    private static final String TOOL_CALL_ID = "call_dot_weather_1";

    /** An id no assistant turn in the conversation ever produced. */
    private static final String UNCORRELATED_TOOL_CALL_ID = "call_never_issued";

    private static final String ERROR_TYPE_INVALID_REQUEST = "invalid_request_error";

    /** What the provider answers when the conversation has not yet carried a tool result. */
    private static final String PROVIDER_TOOL_CALL_RESPONSE = """
            {
              "id": "chatcmpl-tool-1",
              "object": "chat.completion",
              "created": 1789000000,
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": null,
                    "tool_calls": [
                      {
                        "id": "call_dot_weather_1",
                        "type": "function",
                        "function": {
                          "name": "get_weather",
                          "arguments": "{\\"city\\":\\"Bogota\\"}"
                        }
                      }
                    ]
                  },
                  "finish_reason": "tool_calls"
                }
              ],
              "usage": {"prompt_tokens": 82, "completion_tokens": 17, "total_tokens": 99}
            }
            """;

    /** What the provider answers once the tool result is part of the conversation. */
    private static final String PROVIDER_FINAL_ANSWER_RESPONSE = """
            {
              "id": "chatcmpl-tool-2",
              "object": "chat.completion",
              "created": 1789000001,
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "It is currently 19 degrees Celsius in Bogota."
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {"prompt_tokens": 120, "completion_tokens": 12, "total_tokens": 132}
            }
            """;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static WireMockServer wireMockServer;
    private static User user;
    private static String bearerToken;

    private Host host;
    private final ChatCompletionsResource resource = new ChatCompletionsResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProvider();

        // A bare UserDataGen user has no roles at all, so it is neither a backend nor a
        // frontend user and FR-016 rejects it; it also cannot read the site these tests pass
        // as an explicit override, which FR-019 checks. Two roles are needed, not one: the check
        // in WebResource.checkRolePermissions is doesUserHaveRole(user, "DOTCMS_BACK_END_USER")
        // by key and does not walk inheritance, so being an admin does not imply it. Admin is
        // what grants read on the site. Role-specific behaviour is US3's tests, not these.
        user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(
                user.getUserId(),
                Date.from(Instant.now().plus(Duration.ofDays(1))),
                APILocator.systemUser().getUserId(),
                "127.0.0.1");
        bearerToken = "Bearer " + APILocator.getApiTokenAPI().getJWT(apiToken, user);
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() throws Exception {
        host = new SiteDataGen().nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a request declaring a {@code get_weather} tool, against a provider that answers with a
     * tool call
     * When the completion is requested
     * Then the answer finishes for {@code tool_calls} and carries one tool call with an id, the
     * declared function name and its arguments
     */
    @Test
    public void test_completions_withDeclaredTool_returnsToolCall() throws Exception {
        final ChatCompletionRequestView requestView = new ChatCompletionRequestView(
                CHAT_MODEL,
                List.of(
                        systemMessage("You are a helpful assistant."),
                        userMessage("What is the weather in Bogota?")),
                List.of(weatherTool()),
                TextNode.valueOf("auto"),
                null, null, null, null, null, null, null, null);

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), requestView);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);

        final ChatCompletionView view = (ChatCompletionView) response.getEntity();
        assertNotNull(view.choices());
        assertEquals(1, view.choices().size());

        final ChatCompletionView.ChoiceView choice = view.choices().get(0);
        assertEquals("tool_calls", choice.finishReason());
        assertNotNull(choice.message());
        assertNotNull(choice.message().toolCalls());
        assertEquals(1, choice.message().toolCalls().size());

        final ChatCompletionView.ToolCallView toolCall = choice.message().toolCalls().get(0);
        assertEquals(TOOL_CALL_ID, toolCall.id());
        assertNotNull(toolCall.function());
        assertEquals(TOOL_NAME, toolCall.function().name());
        assertNotNull(toolCall.function().arguments());
        assertFalse(toolCall.function().arguments().isBlank());
        assertTrue(toolCall.function().arguments().contains("Bogota"));
    }

    /**
     * Given a conversation replaying the assistant tool-call turn and a {@code role:"tool"} turn
     * carrying that same {@code tool_call_id}
     * When the completion is requested
     * Then the turn is accepted and the provider's answer comes back as a finished completion
     */
    @Test
    public void test_completions_withCorrelatedToolResult_returnsAnswer() throws Exception {
        final ChatCompletionRequestView requestView = new ChatCompletionRequestView(
                CHAT_MODEL,
                List.of(
                        userMessage("What is the weather in Bogota?"),
                        assistantToolCallMessage(TOOL_CALL_ID),
                        toolResultMessage(TOOL_CALL_ID, "{\"tempC\":19}")),
                List.of(weatherTool()),
                TextNode.valueOf("auto"),
                null, null, null, null, null, null, null, null);

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), requestView);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);

        final ChatCompletionView view = (ChatCompletionView) response.getEntity();
        assertNotNull(view.choices());
        assertEquals(1, view.choices().size());

        final ChatCompletionView.ChoiceView choice = view.choices().get(0);
        assertEquals("stop", choice.finishReason());
        assertNotNull(choice.message());
        assertNotNull(choice.message().content());
        assertFalse(choice.message().content().isBlank());

        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH))
                .withRequestBody(containing(TOOL_CALL_ID)));
    }

    /**
     * Given a {@code role:"tool"} turn whose {@code tool_call_id} matches no preceding assistant
     * tool call
     * When the completion is requested
     * Then it is refused as a client error and the provider is never contacted
     */
    @Test
    public void test_completions_withUncorrelatedToolResult_isRejected() throws Exception {
        final ChatCompletionRequestView requestView = new ChatCompletionRequestView(
                CHAT_MODEL,
                List.of(
                        userMessage("What is the weather in Bogota?"),
                        toolResultMessage(UNCORRELATED_TOOL_CALL_ID, "{\"tempC\":19}")),
                List.of(weatherTool()),
                TextNode.valueOf("auto"),
                null, null, null, null, null, null, null, null);

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), requestView);

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView errorView = (InferenceErrorView) response.getEntity();
        assertNotNull(errorView.error());
        assertEquals(ERROR_TYPE_INVALID_REQUEST, errorView.error().type());
        assertNotNull(errorView.error().message());
        assertFalse(errorView.error().message().isBlank());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Given a request that omits {@code model}
     * When the completion is requested
     * Then it is refused as a client error naming {@code model}, with no implicit default applied
     */
    @Test
    public void test_completions_withoutModel_isRejected() {
        final ChatCompletionRequestView requestView = new ChatCompletionRequestView(
                null,
                List.of(userMessage("What is the weather in Bogota?")),
                null, null, null, null, null, null, null, null, null, null);

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), requestView);

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView errorView = (InferenceErrorView) response.getEntity();
        assertNotNull(errorView.error());
        assertEquals(ERROR_TYPE_INVALID_REQUEST, errorView.error().type());
        assertEquals("model", errorView.error().param());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Stubs the OpenAI-compatible provider. The tool-result stub takes precedence — a conversation
     * carrying a tool result also carries the tool declaration, so ordering by priority is what
     * separates the two turns.
     */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .atPriority(1)
                .withRequestBody(containing(TOOL_CALL_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_FINAL_ANSWER_RESPONSE)));

        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .atPriority(2)
                .withRequestBody(containing(TOOL_NAME))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_TOOL_CALL_RESPONSE)));
    }

    /**
     * @return a request authenticated with the test user's bearer token, as the family requires
     */
    private static HttpServletRequest mockRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/inference/v1/chat/completions");
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/inference/v1/chat/completions"));
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        return request;
    }

    private static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }

    private static MessageView systemMessage(final String content) {
        return new MessageView("system", content, null, null, null);
    }

    private static MessageView userMessage(final String content) {
        return new MessageView("user", content, null, null, null);
    }

    /**
     * @param toolCallId the id the assistant turn asked the client to echo back
     * @return the assistant turn replaying a tool call, exactly as a client would resend it
     */
    private static MessageView assistantToolCallMessage(final String toolCallId) {
        return new MessageView(
                "assistant",
                null,
                List.of(new ToolCallView(
                        toolCallId,
                        "function",
                        new FunctionCallView(TOOL_NAME, "{\"city\":\"Bogota\"}"))),
                null,
                null);
    }

    /**
     * @param toolCallId the call this result answers
     * @param content    the tool's output, as JSON text
     * @return the {@code role:"tool"} turn carrying the result
     */
    private static MessageView toolResultMessage(final String toolCallId, final String content) {
        return new MessageView("tool", content, null, toolCallId, TOOL_NAME);
    }

    /**
     * @return the {@code get_weather} tool declaration, with a JSON Schema for its arguments
     */
    private static ToolView weatherTool() throws Exception {
        final JsonNode parameters = OBJECT_MAPPER.readTree("""
                {
                  "type": "object",
                  "properties": {"city": {"type": "string"}},
                  "required": ["city"]
                }
                """);
        return new ToolView(
                "function",
                new FunctionDefView(TOOL_NAME, "Current weather for a city", parameters));
    }
}
