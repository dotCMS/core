package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.EmbeddingsRequestView;
import com.dotcms.inference.rest.view.ImageGenerationRequestView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.internal.models.ModelsListResponse;
import dev.langchain4j.model.output.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves that every one of the four operations returns payloads that deserialize into a
 * standard client library's own result types <strong>with no adapter</strong>.
 *
 * <p>Every other test in this family asserts the shape dotCMS <em>intended</em> to produce, by
 * reading the view objects it returned. That is a different claim, and a weaker one: a payload can
 * satisfy every assertion this codebase makes about it and still fail in the only place that
 * matters, because a real client library is stricter than the test that wrote the field. A
 * required property spelled correctly but typed as a string instead of a number, an enum value the
 * library maps to an enum constant, a field the library refuses to ignore — none of those show up
 * until something that was not written here tries to read the bytes.</p>
 *
 * <p>So this test does not read the view objects at all. It serializes what the endpoint returned
 * exactly as the REST layer would, serves those bytes from WireMock, and points an
 * <strong>unmodified</strong> OpenAI client at them. The client is LangChain4j's, chosen because it
 * is already a dependency and is a genuine third-party OpenAI-compatible implementation: nothing in
 * it was written with dotCMS in mind, and it is the same client code a customer would point at
 * these endpoints. Assertions are made on the library's own result types — {@link ChatResponse},
 * {@link Embedding}, {@link Image}, {@link ModelsListResponse} — never on ours.</p>
 *
 * <p>"No adapter" is the load-bearing phrase, and it is what the absence of code here demonstrates:
 * the payload goes from the endpoint to the client untouched except by {@code writeValueAsString}.
 * If a field had to be renamed, re-typed, or removed to make the client accept it, that work would
 * be visible in this file, and the no-adapter promise would be false.</p>
 *
 * <p>Two WireMock roles share one server, separated by path prefix. The provider stubs sit at the
 * root ({@code /chat/completions}) and stand in for OpenAI as dotCMS's upstream. The client-facing
 * stubs sit under {@value #DOTCMS_FACING_PREFIX} and replay dotCMS's own answer back to the
 * library. Reusing one server is deliberate: a second would need a second fixed port, and the port
 * these tests already share is the thing most likely to collide.</p>
 */
public class InferenceClientConformanceTest {

    /** Where the client-facing stubs live, so they cannot be confused with the provider stubs. */
    private static final String DOTCMS_FACING_PREFIX = "/dotcms-facing";

    private static final String BASE_URL =
            "http://localhost:" + AiTest.PORT + DOTCMS_FACING_PREFIX;

    private static final String CHAT_MODEL = "gpt-4o-mini";
    private static final String EMBEDDINGS_MODEL = AiTest.EMBEDDINGS_MODEL;
    private static final String IMAGE_MODEL = AiTest.IMAGE_MODEL;

    private static final String PROMPT = "A cat in a hammock";
    private static final String ANSWER = "A cat is asleep in a hammock.";

    /** A 1x1 PNG, so the image assertions are about transport rather than about pixels. */
    private static final String IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQ=="; 

    /** What the stubbed provider answers dotCMS for a completion. */
    private static final String PROVIDER_CHAT_RESPONSE = """
            {
              "id": "chatcmpl-conformance",
              "object": "chat.completion",
              "created": 1789000000,
              "model": "%s",
              "choices": [
                {
                  "index": 0,
                  "message": {"role": "assistant", "content": "%s"},
                  "finish_reason": "stop"
                }
              ],
              "usage": {"prompt_tokens": 11, "completion_tokens": 7, "total_tokens": 18}
            }
            """.formatted(CHAT_MODEL, ANSWER);

    /** What the stubbed provider answers dotCMS for a streamed completion. */
    private static final String PROVIDER_STREAM_RESPONSE =
            "data: {\"id\":\"chatcmpl-conformance\",\"object\":\"chat.completion.chunk\","
                    + "\"created\":1789000000,\"model\":\"" + CHAT_MODEL + "\",\"choices\":"
                    + "[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"A cat \"},"
                    + "\"finish_reason\":null}]}\n\n"
                    + "data: {\"id\":\"chatcmpl-conformance\",\"object\":\"chat.completion.chunk\","
                    + "\"created\":1789000000,\"model\":\"" + CHAT_MODEL + "\",\"choices\":"
                    + "[{\"index\":0,\"delta\":{\"content\":\"is asleep.\"},"
                    + "\"finish_reason\":null}]}\n\n"
                    + "data: {\"id\":\"chatcmpl-conformance\",\"object\":\"chat.completion.chunk\","
                    + "\"created\":1789000000,\"model\":\"" + CHAT_MODEL + "\",\"choices\":"
                    + "[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n"
                    + "data: [DONE]\n\n";

    private static final String PROVIDER_EMBEDDINGS_RESPONSE = """
            {
              "object": "list",
              "model": "%s",
              "data": [
                {"object": "embedding", "index": 0, "embedding": [0.1, 0.2, 0.3]}
              ],
              "usage": {"prompt_tokens": 4, "total_tokens": 4}
            }
            """.formatted(EMBEDDINGS_MODEL);

    private static final String PROVIDER_IMAGE_RESPONSE = """
            {
              "created": 1789000000,
              "data": [{"b64_json": "%s", "revised_prompt": "A cat asleep in a hammock."}]
            }
            """.formatted(IMAGE_BASE64);

    /** The mapper the REST layer serializes responses with, so the bytes match what a caller gets. */
    private static final ObjectMapper MAPPER = DotObjectMapperProvider.createDefaultMapper();

    private static WireMockServer wireMockServer;
    private static User user;
    private static String bearerToken;

    private Host host;

    private final ChatCompletionsResource chatResource = new ChatCompletionsResource();
    private final ModelsResource modelsResource = new ModelsResource();
    private final EmbeddingsResource embeddingsResource = new EmbeddingsResource();
    private final ImagesResource imagesResource = new ImagesResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProvider();

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
        host = new SiteDataGen()
                .name("inference-conformance-" + UUID.randomUUID() + ".dotcms.com")
                .nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(
                host, AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a completion produced by dotCMS
     * When an unmodified OpenAI client library reads it
     * Then it deserializes into the library's own {@link ChatResponse}, carrying the answer, the
     * finish reason and the token usage
     *
     * <p>This is the operation every agent framework drives first. The finish reason and usage
     * are asserted alongside the text because they are what a framework acts on rather than
     * displays: a client decides whether to continue a tool loop from the finish reason, and bills
     * or budgets from the usage, so a payload that carries readable text and an unreadable finish
     * reason is not usable even though it looks right.</p>
     */
    @Test
    public void test_chatCompletion_deserializesIntoTheClientLibrarysOwnType() throws Exception {
        final javax.ws.rs.core.Response dotcmsAnswer = chatResource.completions(
                mockRequest("POST", bearerToken), mockResponse(), host.getIdentifier(),
                new ChatCompletionRequestView(CHAT_MODEL,
                        List.of(new ChatCompletionRequestView.MessageView("user", PROMPT, null, null, null)),
                        null, null, null, null, null, null, null, null, null, null));
        assertEquals(200, dotcmsAnswer.getStatus());

        replayToClient("/chat/completions", MAPPER.writeValueAsString(dotcmsAnswer.getEntity()));

        final ChatResponse parsed = OpenAiChatModel.builder()
                .apiKey("not-used-by-the-stub")
                .baseUrl(BASE_URL)
                .modelName(CHAT_MODEL)
                .maxRetries(0)
                .build()
                .chat(dev.langchain4j.model.chat.request.ChatRequest.builder()
                        .messages(UserMessage.from(PROMPT))
                        .build());

        assertNotNull("The client library produced no response at all", parsed);
        assertNotNull(parsed.aiMessage());
        assertEquals("The answer text must survive the round trip into the library's own type",
                ANSWER, parsed.aiMessage().text());
        assertNotNull("A client decides whether to keep going from the finish reason, so an "
                + "unreadable one breaks tool loops even when the text is fine",
                parsed.finishReason());
        assertNotNull("A client budgets from usage; it has to deserialize", parsed.tokenUsage());
        assertEquals(Integer.valueOf(11), parsed.tokenUsage().inputTokenCount());
        assertEquals(Integer.valueOf(7), parsed.tokenUsage().outputTokenCount());
    }

    /**
     * Given a streamed completion produced by dotCMS, as the bytes a caller receives
     * When an unmodified OpenAI client library consumes the event stream
     * Then it reassembles the answer from the library's own streaming callbacks
     *
     * <p>This is the case most likely to break in a way no other test in
     * this file would catch. Streaming is not JSON — it is a framing format, and a client can parse
     * every individual chunk correctly while still failing on the frame boundaries, the terminal
     * {@code [DONE]} sentinel, or a usage event whose {@code choices} array is empty. The assertion
     * is on the text the library reassembled, because that is the only evidence that every frame
     * was read in order and none was dropped.</p>
     */
    @Test
    public void test_streamedCompletion_isReadableByTheClientLibrarysOwnStreamReader()
            throws Exception {
        final javax.ws.rs.core.Response dotcmsAnswer = chatResource.completions(
                mockRequest("POST", bearerToken), mockResponse(), host.getIdentifier(),
                new ChatCompletionRequestView(CHAT_MODEL,
                        List.of(new ChatCompletionRequestView.MessageView("user", PROMPT, null, null, null)),
                        null, null, null, Boolean.TRUE, null, null, null, null, null, null));
        assertEquals(200, dotcmsAnswer.getStatus());
        assertTrue("A streamed completion must come back as a stream, not a buffered body",
                dotcmsAnswer.getEntity() instanceof StreamingOutput);

        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        ((StreamingOutput) dotcmsAnswer.getEntity()).write(captured);
        final String sse = captured.toString(java.nio.charset.StandardCharsets.UTF_8);

        assertTrue("dotCMS must terminate the stream with the sentinel the format defines, or a "
                + "client waits forever", sse.contains("data: [DONE]"));

        wireMockServer.stubFor(post(urlPathEqualTo(DOTCMS_FACING_PREFIX + "/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sse)));

        final StringBuilder reassembled = new StringBuilder();
        final java.util.concurrent.CompletableFuture<ChatResponse> done =
                new java.util.concurrent.CompletableFuture<>();

        dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
                .apiKey("not-used-by-the-stub")
                .baseUrl(BASE_URL)
                .modelName(CHAT_MODEL)
                .build()
                .chat(dev.langchain4j.model.chat.request.ChatRequest.builder()
                                .messages(UserMessage.from(PROMPT))
                                .build(),
                        new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
                            @Override
                            public void onPartialResponse(final String token) {
                                reassembled.append(token);
                            }

                            @Override
                            public void onCompleteResponse(final ChatResponse response) {
                                done.complete(response);
                            }

                            @Override
                            public void onError(final Throwable error) {
                                done.completeExceptionally(error);
                            }
                        });

        final ChatResponse parsed = done.get(30, java.util.concurrent.TimeUnit.SECONDS);

        assertNotNull(parsed);
        assertFalse("The library read the stream but reassembled nothing, which means the frames "
                + "were not where it expected them", reassembled.toString().isBlank());
        assertEquals("Every frame must arrive, in order, exactly once",
                "A cat is asleep.", reassembled.toString());
    }

    /**
     * Given the model listing produced by dotCMS
     * When an unmodified OpenAI client library reads it
     * Then it deserializes into the library's own {@link ModelsListResponse}
     *
     * <p>This is the one operation with no high-level client API to drive,
     * so the library's response type is used directly rather than through a model class — which is
     * the same thing the library's own client does with the bytes once they arrive.</p>
     */
    @Test
    public void test_modelListing_deserializesIntoTheClientLibrarysOwnType() throws Exception {
        final javax.ws.rs.core.Response dotcmsAnswer = modelsResource.models(
                mockRequest("GET", bearerToken), mockResponse(), host.getIdentifier());
        assertEquals(200, dotcmsAnswer.getStatus());

        final String wire = MAPPER.writeValueAsString(dotcmsAnswer.getEntity());

        // Configured the way an OpenAI client configures its own reader — snake_case, because the
        // format spells fields owned_by rather than ownedBy, and tolerant of properties this
        // version's type does not model. Both are the client's settings rather than a concession
        // made for dotCMS: a reader that rejected unknown fields could not survive OpenAI adding
        // one, so no real client is written that way, and pretending otherwise would fail this
        // test on payloads every deployed client accepts.
        final ObjectMapper clientMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final ModelsListResponse parsed = clientMapper.readValue(wire, ModelsListResponse.class);

        assertNotNull(parsed);
        assertEquals("The listing must announce itself as a list, which is what a client keys on",
                "list", parsed.getObject());
        assertNotNull(parsed.getData());
        assertFalse("A site with a configured chat model must list at least that model",
                parsed.getData().isEmpty());
        assertTrue("The configured model has to appear in the listing a caller picks from",
                parsed.getData().stream().anyMatch(m -> CHAT_MODEL.equals(m.id())));
    }

    /**
     * Given embeddings produced by dotCMS
     * When an unmodified OpenAI client library reads them
     * Then they deserialize into the library's own {@link Embedding} vectors
     *
     * <p>This is the operation that carries the most data and the least tolerance for a wrong
     * type: an embedding is a list of numbers, and a payload that renders them as strings parses
     * as JSON and fails in the client. The vector's contents are asserted, not merely its presence,
     * for that reason.</p>
     */
    @Test
    public void test_embeddings_deserializeIntoTheClientLibrarysOwnType() throws Exception {
        final ArrayNode input = MAPPER.createArrayNode();
        input.add(new TextNode("The quick brown fox"));

        final javax.ws.rs.core.Response dotcmsAnswer = embeddingsResource.embeddings(
                mockRequest("POST", bearerToken), mockResponse(), host.getIdentifier(),
                new EmbeddingsRequestView(EMBEDDINGS_MODEL, input));
        assertEquals(200, dotcmsAnswer.getStatus());

        replayToClient("/embeddings", MAPPER.writeValueAsString(dotcmsAnswer.getEntity()));

        final EmbeddingModel client = OpenAiEmbeddingModel.builder()
                .apiKey("not-used-by-the-stub")
                .baseUrl(BASE_URL)
                .modelName(EMBEDDINGS_MODEL)
                .maxRetries(0)
                .build();

        final Response<Embedding> parsed = client.embed("The quick brown fox");

        assertNotNull(parsed);
        assertNotNull("The client library produced no vector", parsed.content());
        assertEquals("The vector must arrive with the dimensions dotCMS sent, not truncated or "
                + "padded by the client's own parsing", 3, parsed.content().dimension());
        assertEquals(0.1f, parsed.content().vector()[0], 0.0001f);
        assertEquals(0.3f, parsed.content().vector()[2], 0.0001f);
    }

    /**
     * Given a generated image produced by dotCMS
     * When an unmodified OpenAI client library reads it
     * Then it deserializes into the library's own {@link Image}, carrying the base64 payload
     *
     * <p>This is the place the inline-only rule for images meets a real client: the
     * library populates either the URL or the base64 field depending on what arrived, so asserting
     * that it found base64 is simultaneously a conformance check and a check that dotCMS never
     * handed out a hosted artifact.</p>
     */
    @Test
    public void test_generatedImage_deserializesIntoTheClientLibrarysOwnType() throws Exception {
        final javax.ws.rs.core.Response dotcmsAnswer = imagesResource.generations(
                mockRequest("POST", bearerToken), mockResponse(), host.getIdentifier(),
                new ImageGenerationRequestView(IMAGE_MODEL, PROMPT, 1, AiTest.IMAGE_SIZE));
        assertEquals(200, dotcmsAnswer.getStatus());

        replayToClient("/images/generations", MAPPER.writeValueAsString(dotcmsAnswer.getEntity()));

        final ImageModel client = OpenAiImageModel.builder()
                .apiKey("not-used-by-the-stub")
                .baseUrl(BASE_URL)
                .modelName(IMAGE_MODEL)
                .maxRetries(0)
                .build();

        final Response<Image> parsed = client.generate(PROMPT);

        assertNotNull(parsed);
        assertNotNull("The client library produced no image", parsed.content());
        assertNotNull("The bytes are returned inline, so this is where a client finds them",
                parsed.content().base64Data());
        assertFalse(parsed.content().base64Data().isBlank());
        assertNull("A client that received a URL instead would be holding a hosted artifact "
                + "this family promises never to mint", parsed.content().url());
    }

    /**
     * Serves dotCMS's own answer back to the client library, on the client-facing prefix.
     *
     * @param operationPath the path the client library appends to its base URL
     * @param dotcmsPayload the bytes dotCMS produced, replayed verbatim
     */
    private static void replayToClient(final String operationPath, final String dotcmsPayload) {
        wireMockServer.stubFor(post(urlPathEqualTo(DOTCMS_FACING_PREFIX + operationPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(dotcmsPayload)));
    }

    /**
     * Stubs the upstream provider dotCMS itself calls, at the server root.
     */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo("/chat/completions"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock
                        .matchingJsonPath("$[?(@.stream == true)]"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(PROVIDER_STREAM_RESPONSE)));

        wireMockServer.stubFor(post(urlPathEqualTo("/chat/completions"))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_CHAT_RESPONSE)));

        wireMockServer.stubFor(post(urlPathEqualTo("/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_EMBEDDINGS_RESPONSE)));

        wireMockServer.stubFor(post(urlPathEqualTo("/images/generations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_IMAGE_RESPONSE)));

        wireMockServer.stubFor(get(urlPathEqualTo("/models"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"object\":\"list\",\"data\":[]}")));
    }

    private static HttpServletRequest mockRequest(final String method, final String credential) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Map<String, Object> attributes = new HashMap<>();
        final String uri = "/api/inference/v1/conformance";

        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost" + uri));
        when(request.getMethod()).thenReturn(method);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getHeader("Authorization")).thenReturn(credential);

        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        return request;
    }

    private static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }
}
