package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.EmbeddingsRequestView;
import com.dotcms.inference.rest.view.ImageGenerationRequestView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Holds FR-036's line: metadata may be logged, the caller's content may not.
 *
 * <p>No request or response body is written to a log today. That was true before this test
 * existed and is exactly why the test is worth having — nothing enforced it. Every log line in
 * this family carries identifiers, sizes, model names and timings, and a contributor adding
 * {@code Logger.debug(this, "request: " + requestView)} while chasing a bug would break the
 * requirement without failing a single other test in the suite. The leak would then sit in the
 * logs of every deployment until someone read one.</p>
 *
 * <p>The method is a sentinel: a string that could only have come from the caller's content is
 * planted in the prompt and the embeddings input, every log record emitted anywhere in the JVM
 * during the call is captured, and the test fails if the sentinel appears in any of them. Because
 * the appender is attached to the root logger rather than to a named one, a future log statement
 * in a class nobody thought of is covered too.</p>
 *
 * <p>The failure paths are exercised alongside the happy ones, because that is where content
 * leaks into logs in practice: the natural thing to write when a provider rejects a request is
 * the request that was rejected.</p>
 */
public class InferenceLoggingTest {

    /**
     * Planted in the caller's content. Nonsense on purpose — a realistic prompt could plausibly
     * appear in a log for some unrelated reason, and the assertion has to mean one thing.
     */
    private static final String SENTINEL = "zqx-carrier-pigeon-7741-confidential";

    private static final String CHAT_MODEL = "gpt-4o-mini";
    private static final String COMPLETIONS_PATH = "/chat/completions";
    private static final String EMBEDDINGS_PATH = "/embeddings";
    private static final String IMAGES_PATH = "/images/generations";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static WireMockServer wireMockServer;
    private static User user;
    private static String bearerToken;

    private Host host;
    private CapturingAppender appender;
    private Logger rootLogger;

    private final ChatCompletionsResource chatResource = new ChatCompletionsResource();
    private final EmbeddingsResource embeddingsResource = new EmbeddingsResource();
    private final ImagesResource imagesResource = new ImagesResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();

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
                .name("inference-logging-" + UUID.randomUUID() + ".dotcms.com")
                .nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(
                host, AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));

        appender = new CapturingAppender();
        appender.start();
        rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(appender);
    }

    @After
    public void after() throws Exception {
        if (rootLogger != null && appender != null) {
            rootLogger.removeAppender(appender);
            appender.stop();
        }
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a completion whose prompt carries a sentinel, served successfully
     * When the exchange finishes
     * Then the sentinel appears in no log record
     */
    @Test
    public void test_completion_doesNotLogThePrompt() {
        stubProvider(COMPLETIONS_PATH, 200, providerChatResponse());

        chatResource.completions(mockRequest(), mockResponse(), host.getIdentifier(),
                chatRequest(false));

        assertSentinelNeverLogged("a completion that succeeded");
    }

    /**
     * Given a streamed completion whose prompt carries a sentinel
     * When the stream is consumed to its end
     * Then the sentinel appears in no log record
     *
     * <p>Separated from the buffered case because the streaming path logs on its own threads and
     * has its own failure and timeout reporting, none of which the buffered path exercises.</p>
     */
    @Test
    public void test_streamedCompletion_doesNotLogThePrompt() throws Exception {
        stubProvider(COMPLETIONS_PATH, 200, providerChatResponse());

        final Response response = chatResource.completions(
                mockRequest(), mockResponse(), host.getIdentifier(), chatRequest(true));
        if (response.getEntity() instanceof StreamingOutput streaming) {
            streaming.write(new ByteArrayOutputStream());
        }

        assertSentinelNeverLogged("a streamed completion");
    }

    /**
     * Given a completion the provider refuses
     * When the failure is reported
     * Then the sentinel appears in no log record
     *
     * <p>The case that matters most. When a provider rejects a request the obvious thing to log
     * is the request that was rejected, so this is where a prompt reaches a log file first.</p>
     */
    @Test
    public void test_failedCompletion_doesNotLogThePrompt() {
        stubProvider(COMPLETIONS_PATH, 500, "{\"error\":{\"message\":\"upstream exploded\"}}");

        chatResource.completions(mockRequest(), mockResponse(), host.getIdentifier(),
                chatRequest(false));

        assertSentinelNeverLogged("a completion the provider refused");
    }

    /**
     * Given embeddings whose input carries a sentinel, and a provider that refuses
     * When the failure is reported
     * Then the sentinel appears in no log record
     *
     * <p>Embeddings carry the most caller content of any operation here — whole documents, in
     * batches — so they are the costliest thing to leak and are checked on the failure path.</p>
     */
    @Test
    public void test_failedEmbeddings_doesNotLogTheInput() {
        stubProvider(EMBEDDINGS_PATH, 500, "{\"error\":{\"message\":\"upstream exploded\"}}");

        final ArrayNode input = MAPPER.createArrayNode();
        input.add(new TextNode("Please embed " + SENTINEL));

        embeddingsResource.embeddings(mockRequest(), mockResponse(), host.getIdentifier(),
                new EmbeddingsRequestView(AiTest.EMBEDDINGS_MODEL, input));

        assertSentinelNeverLogged("embeddings the provider refused");
    }

    /**
     * Given an image request whose prompt carries a sentinel, and a provider that refuses
     * When the failure is reported
     * Then the sentinel appears in no log record
     */
    @Test
    public void test_failedImageGeneration_doesNotLogThePrompt() {
        stubProvider(IMAGES_PATH, 500, "{\"error\":{\"message\":\"upstream exploded\"}}");

        imagesResource.generations(mockRequest(), mockResponse(), host.getIdentifier(),
                new ImageGenerationRequestView(AiTest.IMAGE_MODEL,
                        "Draw " + SENTINEL, 1, AiTest.IMAGE_SIZE));

        assertSentinelNeverLogged("an image request the provider refused");
    }

    /**
     * Fails when the caller's content reached a log, naming what was being exercised.
     *
     * @param what a description of the exchange, so a failure says which path leaked
     */
    private void assertSentinelNeverLogged(final String what) {
        final List<String> captured = appender.messages();

        assertFalse("Nothing was logged at all during " + what + ", so this assertion proves "
                        + "nothing — the appender is not attached, or the code stopped logging "
                        + "and this test has quietly stopped testing anything",
                captured.isEmpty());

        final List<String> leaking = new ArrayList<>();
        for (final String message : captured) {
            if (message != null && message.contains(SENTINEL)) {
                leaking.add(message);
            }
        }

        assertTrue("FR-036 allows metadata in the logs and never the caller's content, but "
                        + what + " wrote it: " + leaking,
                leaking.isEmpty());
    }

    private ChatCompletionRequestView chatRequest(final boolean stream) {
        return new ChatCompletionRequestView(
                CHAT_MODEL,
                Collections.singletonList(new ChatCompletionRequestView.MessageView(
                        "user", "Summarise " + SENTINEL, null, null, null)),
                null, null, null, stream ? Boolean.TRUE : null,
                null, null, null, null, null, null);
    }

    private static void stubProvider(final String path, final int status, final String body) {
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private static String providerChatResponse() {
        return "{\"id\":\"chatcmpl-log\",\"object\":\"chat.completion\",\"created\":1789000000,"
                + "\"model\":\"" + CHAT_MODEL + "\",\"choices\":[{\"index\":0,\"message\":"
                + "{\"role\":\"assistant\",\"content\":\"A summary.\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":9,\"completion_tokens\":3,\"total_tokens\":12}}";
    }

    private HttpServletRequest mockRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Map<String, Object> attributes = new HashMap<>();
        final String uri = "/api/inference/v1/logging";

        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getServerName()).thenReturn(host.getHostname());
        when(request.getHeader("Authorization")).thenReturn(bearerToken);

        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        return request;
    }

    private static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }

    /**
     * Collects every log record emitted while it is attached, including the rendered throwable.
     *
     * <p>The throwable matters as much as the message: a provider client's exception can quote the
     * request back, so logging a safe message with an unsafe cause leaks just as effectively.</p>
     */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> captured = Collections.synchronizedList(new ArrayList<>());

        private CapturingAppender() {
            super("InferenceLoggingCapture", null, null, true, null);
        }

        @Override
        public void append(final LogEvent event) {
            captured.add(event.getMessage() == null
                    ? null : event.getMessage().getFormattedMessage());
            if (event.getThrown() != null) {
                for (Throwable current = event.getThrown();
                        current != null && current != current.getCause();
                        current = current.getCause()) {
                    captured.add(current.getMessage());
                }
            }
        }

        private List<String> messages() {
            synchronized (captured) {
                return new ArrayList<>(captured);
            }
        }
    }
}
