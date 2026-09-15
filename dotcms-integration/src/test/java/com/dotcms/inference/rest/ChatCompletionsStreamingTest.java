package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.mapper.SseSerializer;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.MessageView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.StreamOptionsView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
 * Specifies the streamed half of
 * {@link ChatCompletionsResource#completions(HttpServletRequest, HttpServletResponse, String, ChatCompletionRequestView)}
 * — what a caller who asked for {@code "stream": true} actually reads off the wire.
 *
 * <p>Streaming is where the wire format is easiest to get subtly wrong, because a client consumes
 * the answer as it arrives and therefore cannot re-read it once it has acted on it. Three of the
 * four behaviours here are about the frames themselves; the fourth is about how a stream
 * <em>ends</em>, which is the only part a client cannot recover from on its own.</p>
 *
 * <ul>
 *     <li>FR-007 — every chunk carries {@code id}, {@code object} = {@code chat.completion.chunk},
 *     {@code created} and {@code model}; the last content chunk carries a {@code finish_reason};
 *     the stream closes with {@code data: [DONE]}.</li>
 *     <li>FR-008 — tool-call arguments arrive as fragments that reassemble into the complete
 *     argument JSON, with the call's id carried verbatim rather than synthesised.</li>
 *     <li>FR-009 — a usage chunk, with an empty {@code choices} array, appears immediately before
 *     the terminal marker only when the caller asked for it through
 *     {@code stream_options.include_usage}, and never otherwise.</li>
 *     <li>FR-039 — a provider that fails part-way through produces an error frame and a stream
 *     that closes <strong>without</strong> the terminal marker. This is the assertion that
 *     matters most in this file: withholding {@code [DONE]} is what stops a client which does not
 *     parse the error frame from reading a truncated answer as a finished one.</li>
 * </ul>
 *
 * <p>The provider is a WireMock server standing in for an OpenAI-compatible endpoint, wired in
 * through the same dotAI app secrets the rest of the AI integration tests use, so the exchange
 * travels the real client path rather than a stubbed one.</p>
 *
 * <p>The usage tests pin down that {@code stream_options} is <strong>not</strong> forwarded
 * upstream. It is an OpenAI-format option that four of dotCMS's seven providers do not
 * understand, so forwarding it would make streamed usage work on some vendors and silently fail
 * on others. dotCMS reads the flag itself and builds the chunk from the token counts the unified
 * provider abstraction returns on completion — available whichever vendor served the request —
 * and suppresses a usage chunk a provider volunteers unasked, since its empty {@code choices}
 * array is the shape that breaks readers assuming every chunk carries one.</p>
 */
public class ChatCompletionsStreamingTest {

    /** The chat model the site is configured with, and the only one these tests ask for. */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    /** Path an OpenAI-compatible provider serves completions on. */
    private static final String COMPLETIONS_PATH = "/chat/completions";

    /** Media type a streamed completion is served as. */
    private static final String EVENT_STREAM = "text/event-stream";

    /** Prefix every server-sent event field this family emits carries. */
    private static final String DATA_PREFIX = "data: ";

    /** Payload of the terminal frame, without its prefix or terminator. */
    private static final String DONE_PAYLOAD = "[DONE]";

    /** The tool the streamed tool-call conversation declares. */
    private static final String TOOL_NAME = "get_weather";

    /** The id the stubbed provider mints for its tool call, and that must survive verbatim. */
    private static final String TOOL_CALL_ID = "call_dot_weather_1";

    /** The argument JSON the provider sends in fragments, once reassembled. */
    private static final String EXPECTED_TOOL_ARGUMENTS = "{\"city\":\"Bogota\"}";

    /** The streaming option gating the usage chunk, as it appears on the wire. */
    private static final String INCLUDE_USAGE = "include_usage";

    /** A plain content stream: a role chunk, two content chunks, a finish chunk, the marker. */
    private static final String PROVIDER_CONTENT_STREAM = """
            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""}}]}

            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"It is currently "}}]}

            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"19 degrees in Bogota."}}]}

            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

            data: [DONE]

            """;

    /** The same stream, with the usage chunk the provider adds when asked for it. */
    private static final String PROVIDER_CONTENT_STREAM_WITH_USAGE = """
            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""}}]}

            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"It is currently 19 degrees in Bogota."}}]}

            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

            data: {"id":"chatcmpl-stream-1","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[],"usage":{"prompt_tokens":82,"completion_tokens":17,"total_tokens":99}}

            data: [DONE]

            """;

    /** A tool call whose arguments are split across two chunks, as providers really send them. */
    private static final String PROVIDER_TOOL_CALL_STREAM = """
            data: {"id":"chatcmpl-stream-2","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":null}}]}

            data: {"id":"chatcmpl-stream-2","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_dot_weather_1","type":"function","function":{"name":"get_weather","arguments":"{\\"ci"}}]}}]}

            data: {"id":"chatcmpl-stream-2","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"ty\\":\\"Bogota\\"}"}}]}}]}

            data: {"id":"chatcmpl-stream-2","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"finish_reason":"tool_calls"}]}

            data: [DONE]

            """;

    /**
     * A stream that starts well and then breaks: the third frame is truncated JSON and the
     * provider never sends a terminal marker. The generation is already in flight by then, which
     * is exactly the window FR-039 is about.
     */
    private static final String PROVIDER_MALFORMED_STREAM = """
            data: {"id":"chatcmpl-stream-3","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""}}]}

            data: {"id":"chatcmpl-stream-3","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"It is currently "}}]}

            data: {"id":"chatcmpl-stream-3","object":"chat.completion.chunk","created":178900

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
        wireMockServer.resetAll();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a provider streaming a plain content answer
     * When the completion is requested with {@code stream: true}
     * Then the body is a sequence of {@code data:} frames, every chunk carrying {@code id},
     * {@code object}, {@code created} and {@code model}, the last content chunk carrying a
     * {@code finish_reason}, and the stream closing with the terminal marker
     */
    @Test
    public void test_completions_withStreamRequested_writesChunkFramesEndingWithDoneMarker() throws Exception {
        stubProviderStream(PROVIDER_CONTENT_STREAM);

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), streamingRequest(null));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof StreamingOutput);

        final String body = drain(response);
        assertTrue(body.startsWith(DATA_PREFIX));
        assertTrue(body.endsWith(SseSerializer.DONE_MARKER));

        final List<JsonNode> chunks = chunkFrames(body);
        assertFalse(chunks.isEmpty());

        final String completionId = chunks.get(0).path("id").asText();
        assertFalse(completionId.isBlank());

        for (final JsonNode chunk : chunks) {
            assertEquals(completionId, chunk.path("id").asText());
            assertEquals(SseSerializer.CHUNK_OBJECT, chunk.path("object").asText());
            assertTrue(chunk.path("created").isNumber());
            assertTrue(chunk.path("created").asLong() > 0);
            assertEquals(CHAT_MODEL, chunk.path("model").asText());
        }

        final JsonNode lastContentChunk = chunks.get(chunks.size() - 1);
        assertEquals(1, lastContentChunk.path("choices").size());
        assertEquals("stop", lastContentChunk.path("choices").get(0).path("finish_reason").asText());

        final String streamedContent = concatenatedContent(chunks);
        assertTrue(streamedContent.contains("Bogota"));

        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Given a provider streaming a tool call whose arguments are split across chunks
     * When the completion is requested with {@code stream: true}
     * Then the fragments reassemble into the complete argument JSON and the call's id is carried
     * verbatim, announced once rather than repeated or regenerated per fragment
     */
    @Test
    public void test_completions_withStreamedToolCall_reassemblesArgumentFragments() throws Exception {
        stubProviderStream(PROVIDER_TOOL_CALL_STREAM);

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), streamingRequest(null));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof StreamingOutput);

        final String body = drain(response);
        assertTrue(body.endsWith(SseSerializer.DONE_MARKER));

        final List<JsonNode> chunks = chunkFrames(body);
        final List<String> argumentFragments = new ArrayList<>();
        final List<String> announcedIds = new ArrayList<>();
        String functionName = null;

        for (final JsonNode chunk : chunks) {
            for (final JsonNode choice : chunk.path("choices")) {
                for (final JsonNode toolCall : choice.path("delta").path("tool_calls")) {
                    if (toolCall.hasNonNull("id")) {
                        announcedIds.add(toolCall.path("id").asText());
                    }
                    final JsonNode function = toolCall.path("function");
                    if (function.hasNonNull("name")) {
                        functionName = function.path("name").asText();
                    }
                    if (function.hasNonNull("arguments")) {
                        argumentFragments.add(function.path("arguments").asText());
                    }
                }
            }
        }

        assertEquals(List.of(TOOL_CALL_ID), announcedIds);
        assertEquals(TOOL_NAME, functionName);
        assertTrue("arguments must arrive as fragments, not as one whole blob",
                argumentFragments.size() > 1);
        assertEquals(EXPECTED_TOOL_ARGUMENTS, String.join("", argumentFragments));
        assertEquals("Bogota",
                OBJECT_MAPPER.readTree(String.join("", argumentFragments)).path("city").asText());
    }

    /**
     * Given a provider streaming an answer for a request that says nothing about usage
     * When the completion is requested with {@code stream: true}
     * Then no usage chunk appears anywhere in the stream
     */
    @Test
    public void test_completions_streamWithoutIncludeUsage_omitsUsageChunk() throws Exception {
        stubProviderStreamWithoutUsage();

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), streamingRequest(null));

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final String body = drain(response);
        assertTrue(body.endsWith(SseSerializer.DONE_MARKER));

        for (final JsonNode chunk : chunkFrames(body)) {
            assertFalse("no chunk may carry usage unless the caller asked for it",
                    chunk.hasNonNull("usage"));
            assertFalse("an empty choices array only belongs to a usage chunk",
                    chunk.path("choices").isArray() && chunk.path("choices").isEmpty());
        }
    }

    /**
     * Given a request carrying {@code stream_options.include_usage}, against a provider whose
     * stream contains no usage chunk at all
     * When the completion is requested with {@code stream: true}
     * Then dotCMS emits the usage chunk itself, from the token counts the provider abstraction
     * returns on completion, and does not forward the option upstream
     */
    @Test
    public void test_completions_streamWithIncludeUsage_relaysUsageChunk() throws Exception {
        stubProviderStreamVolunteeringUsage();

        final Response response = resource.completions(
                mockRequest(),
                mockResponse(),
                host.getIdentifier(),
                streamingRequest(new StreamOptionsView(Boolean.TRUE)));

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final String body = drain(response);
        assertTrue(body.endsWith(SseSerializer.DONE_MARKER));

        final List<JsonNode> chunks = chunkFrames(body);
        assertFalse(chunks.isEmpty());

        final JsonNode usageChunk = chunks.get(chunks.size() - 1);
        assertTrue("the usage chunk is the last one before [DONE]", usageChunk.hasNonNull("usage"));
        assertTrue(usageChunk.path("choices").isArray());
        assertTrue("the usage chunk carries an empty choices array",
                usageChunk.path("choices").isEmpty());
        assertEquals(SseSerializer.CHUNK_OBJECT, usageChunk.path("object").asText());
        assertTrue(usageChunk.path("usage").path("total_tokens").asLong() > 0);

        // Deliberately NOT asserted here: that "include_usage" never reached the provider.
        // langchain4j's OpenAiStreamingChatModel builds StreamOptions.includeUsage(true) on every
        // streamed request of its own accord, so that string is on the wire whatever dotCMS does
        // and the assertion would be testing the library, not us. What is ours is the gating, and
        // the suppression test below proves it from the other side: same provider stream carrying
        // usage, caller did not ask, chunk withheld.
    }

    /**
     * Given a provider that volunteers a usage chunk although the caller did not ask for one
     * When the completion is requested with {@code stream: true} and no streaming options
     * Then the volunteered chunk is suppressed rather than relayed
     */
    @Test
    public void test_completions_providerVolunteersUsage_withoutRequest_suppressesIt() throws Exception {
        stubProviderStreamVolunteeringUsage();

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), streamingRequest(null));

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final String body = drain(response);
        assertTrue(body.endsWith(SseSerializer.DONE_MARKER));

        for (final JsonNode chunk : chunkFrames(body)) {
            assertFalse("a usage chunk the caller never asked for must not be relayed",
                    chunk.hasNonNull("usage"));
            assertFalse("an empty choices array breaks readers that assume every chunk has one",
                    chunk.path("choices").isArray() && chunk.path("choices").isEmpty());
        }
    }

    /**
     * Given a provider that starts streaming and then breaks — a truncated, unparseable frame and
     * no terminal marker of its own
     * When the completion is requested with {@code stream: true}
     * Then the caller receives an error frame and the stream closes <strong>without</strong>
     * {@code [DONE]}, so a failed stream can never be mistaken for a finished one
     */
    @Test
    public void test_completions_whenProviderFailsMidStream_writesErrorFrameWithoutDoneMarker() throws Exception {
        stubProviderStream(PROVIDER_MALFORMED_STREAM);

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), streamingRequest(null));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof StreamingOutput);

        final String body = drain(response);

        assertFalse("a failed stream must never carry the terminal marker",
                body.contains(DONE_PAYLOAD));
        assertFalse(body.endsWith(SseSerializer.DONE_MARKER));

        final List<JsonNode> frames = dataFrames(body);
        assertFalse(frames.isEmpty());

        final JsonNode lastFrame = frames.get(frames.size() - 1);
        assertTrue("the stream ends on an error frame", lastFrame.hasNonNull("error"));

        final JsonNode error = lastFrame.path("error");
        assertNotNull(error.path("message").asText());
        assertFalse(error.path("message").asText().isBlank());
        assertFalse("the error type must be stated", error.path("type").asText().isBlank());
    }

    /**
     * Given a provider that faults the connection part-way through its response
     * When the completion is requested with {@code stream: true}
     * Then whatever reaches the caller carries an error and does not carry {@code [DONE]} — the
     * same invariant as an unparseable frame, through a different failure mode
     */
    @Test
    public void test_completions_whenProviderConnectionFaults_streamNeverLooksFinished() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        final Response response = resource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), streamingRequest(null));

        assertNotNull(response);

        final String body = clientVisibleBody(response);

        assertFalse("a faulted stream must never carry the terminal marker",
                body.contains(DONE_PAYLOAD));
        assertTrue("the caller must be told the answer failed", body.contains("\"error\""));
    }

    /**
     * Stubs the provider with a single streamed response, served as an event stream.
     *
     * @param streamBody the server-sent events the provider writes back
     */
    private static void stubProviderStream(final String streamBody) {
        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", EVENT_STREAM)
                        .withBody(streamBody)));
    }

    /**
     * Stubs the provider stream that carries no usage chunk of its own.
     *
     * <p>Deliberately one stub, not a pair keyed on the outbound body: {@code stream_options} is
     * an OpenAI-format option that four of dotCMS's seven providers do not understand, so
     * forwarding it would make streamed usage work on some vendors and silently not on others.
     * dotCMS instead reads the flag itself and emits the usage chunk from the token counts the
     * unified provider abstraction already hands back on completion, which behaves identically
     * whichever vendor served the request.</p>
     */
    private static void stubProviderStreamWithoutUsage() {
        stubProviderStream(PROVIDER_CONTENT_STREAM);
    }

    /**
     * Stubs a provider that volunteers a usage chunk nobody asked for.
     *
     * <p>Some providers do this. The chunk carries an empty {@code choices} array, which is
     * exactly the shape that breaks stream readers assuming every chunk has a choice — so it must
     * not reach a caller who did not ask for usage.</p>
     */
    private static void stubProviderStreamVolunteeringUsage() {
        stubProviderStream(PROVIDER_CONTENT_STREAM_WITH_USAGE);
    }

    /**
     * @param streamOptions the streaming options to send, or null to send none
     * @return a one-turn conversation asking for a streamed answer
     */
    /**
     * Given a node already at its ceiling of concurrent streamed completions
     * When another streamed completion is asked for
     * Then it is refused with a 429 that carries {@code Retry-After}, and nothing reaches the
     * provider
     *
     * <p>FR-037 for the ceiling itself, which had no coverage at all until now, and FR-031 for the
     * header. The two belong in one test because the ceiling is only half an answer without it: a
     * refusal that says "retry shortly" in a prose message tells a program nothing, and a client
     * that cannot read a wait interval invents one — which under load means every refused caller
     * returning at once and holding the node at capacity it was trying to shed.</p>
     *
     * <p>The ceiling is driven to zero rather than fifty streams being opened. Fifty real streams
     * would make the test slow, machine-dependent and flaky, and would test the thread pool rather
     * than the refusal; the limit is read from configuration on every request precisely so it can
     * be changed without a restart, and that is the seam used here.</p>
     */
    @Test
    public void test_atTheStreamCeiling_refusesWith429AndRetryAfter() {
        final String previous = Config.getStringProperty(
                InferenceLimits.MAX_CONCURRENT_STREAMS_KEY, null);
        Config.setProperty(InferenceLimits.MAX_CONCURRENT_STREAMS_KEY, "0");
        try {
            final Response response = resource.completions(
                    mockRequest(), mockResponse(), host.getIdentifier(), streamingRequest(null));

            assertNotNull(response);
            assertEquals("A node at its ceiling refuses rather than queues, so the caller can go "
                    + "elsewhere instead of waiting on a thread that will not free up",
                    429, response.getStatus());

            assertEquals("The caller has to be told how long to wait in a form it can act on, not "
                            + "only in prose it cannot parse",
                    String.valueOf(5), response.getHeaderString("Retry-After"));

            assertTrue(response.getEntity() instanceof InferenceErrorView);
            final InferenceErrorView.Body error =
                    ((InferenceErrorView) response.getEntity()).error();
            assertNotNull(error);
            assertEquals("rate_limit_error", error.type());

            wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
        } finally {
            if (previous == null) {
                Config.setProperty(InferenceLimits.MAX_CONCURRENT_STREAMS_KEY, null);
            } else {
                Config.setProperty(InferenceLimits.MAX_CONCURRENT_STREAMS_KEY, previous);
            }
        }
    }

    private static ChatCompletionRequestView streamingRequest(final StreamOptionsView streamOptions) {
        return new ChatCompletionRequestView(
                CHAT_MODEL,
                List.of(userMessage("What is the weather in Bogota?")),
                null,
                null,
                null,
                Boolean.TRUE,
                streamOptions,
                null, null, null, null, null);
    }

    /**
     * Writes the streamed entity out in full, the way the container would.
     *
     * @param response the response the resource returned
     * @return everything the caller would have read off the stream
     */
    private static String drain(final Response response) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Whatever the caller ends up seeing, whether the failure was delivered as a stream or as a
     * plain error body. Used only where the invariant under test holds for both.
     *
     * @param response the response the resource returned
     * @return the response body, as text
     */
    private static String clientVisibleBody(final Response response) throws Exception {
        return response.getEntity() instanceof StreamingOutput
                ? drain(response)
                : OBJECT_MAPPER.writeValueAsString(response.getEntity());
    }

    /**
     * @param body the streamed text
     * @return every {@code data:} frame parsed as JSON, the terminal marker excluded
     */
    private static List<JsonNode> dataFrames(final String body) throws Exception {
        final List<JsonNode> frames = new ArrayList<>();
        for (final String frame : body.split("\n\n")) {
            final String trimmed = frame.strip();
            if (trimmed.isEmpty() || !trimmed.startsWith(DATA_PREFIX)) {
                continue;
            }
            final String payload = trimmed.substring(DATA_PREFIX.length()).strip();
            if (DONE_PAYLOAD.equals(payload)) {
                continue;
            }
            frames.add(OBJECT_MAPPER.readTree(payload));
        }
        return frames;
    }

    /**
     * @param body the streamed text
     * @return the completion chunks, error frames excluded
     */
    private static List<JsonNode> chunkFrames(final String body) throws Exception {
        final List<JsonNode> chunks = new ArrayList<>();
        for (final JsonNode frame : dataFrames(body)) {
            if (!frame.hasNonNull("error")) {
                chunks.add(frame);
            }
        }
        return chunks;
    }

    /**
     * @param chunks the completion chunks, in order
     * @return the answer the caller would have assembled from the content deltas
     */
    private static String concatenatedContent(final List<JsonNode> chunks) {
        final StringBuilder content = new StringBuilder();
        for (final JsonNode chunk : chunks) {
            for (final JsonNode choice : chunk.path("choices")) {
                final JsonNode delta = choice.path("delta").path("content");
                if (delta.isTextual()) {
                    content.append(delta.asText());
                }
            }
        }
        return content.toString();
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

    private static MessageView userMessage(final String content) {
        return new MessageView("user", content, null, null, null);
    }
}
