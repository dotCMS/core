package com.dotcms.inference;

import com.dotcms.inference.model.FinishReason;
import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.model.InferenceStreamEvent;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.rest.mapper.SseSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for how {@link SseSerializer} ends a stream that failed after it had begun.
 *
 * <p>Once the first chunk is written the HTTP status is already on the wire and can no longer
 * carry the failure. The error event is the only way a caller learns the answer is incomplete,
 * and <strong>withholding the terminal {@code [DONE]} marker</strong> is what stops a client that
 * does not parse the error event from reading a truncated answer as a finished one. That single
 * behaviour — {@link SseSerializer#shouldWriteDoneMarker(InferenceStreamEvent)} returning
 * {@code false} after an {@link InferenceStreamEvent.Error} — is the most important assertion in
 * the streaming work.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>An error event rendering a {@code data:} frame whose JSON has a top-level {@code error}
 *       object with {@code message} and {@code type}.</li>
 *   <li>That body carrying no HTTP status field — retryability rides on the status code, and the
 *       body stays conformant to the standard error shape, which has no such field.</li>
 *   <li><strong>{@code shouldWriteDoneMarker} returning {@code false} after an error.</strong></li>
 *   <li>It returning {@code true} after {@link InferenceStreamEvent.Finish} and after
 *       {@link InferenceStreamEvent.Usage}, so the false case is demonstrably specific to failure
 *       rather than blanket behaviour.</li>
 *   <li>An error frame being neither equal to nor containing
 *       {@link SseSerializer#DONE_MARKER}.</li>
 *   <li>The two ways a stream fails mid-flight — an upstream provider error and the completion
 *       timeout expiring — behaving identically.</li>
 * </ul>
 */
public class SseFailureTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** The prefix every server-sent event payload is written behind. */
    private static final String DATA_PREFIX = "data: ";

    private static final String COMPLETION_ID = "chatcmpl-failed";
    private static final String MODEL = "gpt-4o";
    private static final long CREATED = 1789000000L;

    private static final String UPSTREAM_MESSAGE = "Upstream provider unavailable";
    private static final String TIMEOUT_MESSAGE = "The completion timed out before it finished";

    /**
     * Given a provider that failed part-way through generating an answer.
     * When the error event is rendered.
     * Then the frame's JSON carries a top-level {@code error} object with {@code message} and
     * {@code type}, which is the shape a standard client deserializes into its own error type.
     *
     * @throws Exception when the rendered frame cannot be parsed
     */
    @Test
    public void test_toFrame_errorEvent_rendersTopLevelErrorObject() throws Exception {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.Error(InferenceError.upstream(UPSTREAM_MESSAGE));

        final JsonNode frame = parseFrame(
                SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        final JsonNode error = frame.get("error");
        assertNotNull("A failed stream's last frame carries a top-level error object", error);
        assertTrue("The error must be an object, not a bare string", error.isObject());
        assertEquals("The safe description must be carried verbatim",
                UPSTREAM_MESSAGE, error.path("message").asText());
        assertEquals("The error family must be carried",
                "api_error", error.path("type").asText());
    }

    /**
     * Given an upstream failure whose {@link InferenceError#httpStatus()} is 502.
     * When the error event is rendered.
     * Then no HTTP status field reaches the body — retryability is conveyed by the response's
     * status code, which is what a standard client's back-off keys off, and the standard error
     * shape has no field to put it in.
     *
     * @throws Exception when the rendered frame cannot be parsed
     */
    @Test
    public void test_toFrame_errorEvent_omitsHttpStatusFromBody() throws Exception {

        final InferenceError upstream = InferenceError.upstream(UPSTREAM_MESSAGE);
        assertEquals("Precondition: an upstream failure is a 502", 502, upstream.httpStatus());
        assertTrue("Precondition: a 502 is retryable by its status", upstream.isRetryable());

        final String rendered = SseSerializer.toFrame(
                new InferenceStreamEvent.Error(upstream), COMPLETION_ID, MODEL, CREATED);
        final JsonNode frame = parseFrame(rendered);
        final JsonNode error = frame.get("error");

        assertNotNull("A failed stream's last frame carries a top-level error object", error);
        assertFalse("The status must not leak into the body", error.has("httpStatus"));
        assertFalse("The status must not leak into the body", error.has("http_status"));
        assertFalse("The status must not leak into the body", error.has("status"));
        assertFalse("The status must not leak into the body", error.has("statusCode"));
        assertFalse("The status must not leak into the body", error.has("status_code"));
        assertFalse("Retryability rides on the status code, not on an invented body field",
                error.has("retryable"));
        assertFalse("The status must not leak into the body at the top level",
                frame.has("httpStatus"));
        assertFalse("The status must not leak into the body anywhere", rendered.contains("502"));
    }

    /**
     * Given a stream that failed after its first chunk was already written.
     * When the serializer is asked whether the terminal marker follows the error event.
     * Then it says <strong>no</strong>.
     *
     * <p>This is the single most important assertion in the streaming work: withholding
     * {@code [DONE]} is the only thing that stops a client which does not parse the error event
     * from treating a truncated answer as a complete one.</p>
     */
    @Test
    public void test_shouldWriteDoneMarker_errorEvent_returnsFalse() {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.Error(InferenceError.upstream(UPSTREAM_MESSAGE));

        assertFalse("A failed stream must NOT be terminated with [DONE] — a client that does not "
                        + "parse the error event must see a truncated stream, never a cleanly "
                        + "finished one",
                SseSerializer.shouldWriteDoneMarker(event));
    }

    /**
     * Given the completion timeout expiring mid-generation, the second way a stream fails after it
     * has started.
     * When the serializer is asked whether the terminal marker follows.
     * Then it says no, identically to the provider-failure path — the caller must not be able to
     * tell a timed-out answer from a complete one only by luck.
     */
    @Test
    public void test_shouldWriteDoneMarker_completionTimeoutErrorEvent_returnsFalse() {

        final InferenceStreamEvent event = new InferenceStreamEvent.Error(completionTimeout());

        assertFalse("A stream cut short by the completion timeout must NOT be terminated with "
                        + "[DONE] either",
                SseSerializer.shouldWriteDoneMarker(event));
    }

    /**
     * Given streams that ended normally — one on a finish event, one on a usage event.
     * When the serializer is asked whether the terminal marker follows.
     * Then it says yes for every serializable finish reason and for usage, so the {@code false}
     * returned after an error is demonstrably specific to failure rather than blanket behaviour.
     */
    @Test
    public void test_shouldWriteDoneMarker_finishAndUsageEvents_returnTrue() {

        for (final FinishReason reason : FinishReason.values()) {
            if (reason == FinishReason.ERROR) {
                // Never serialized as a finish reason; a failed stream ends with an Error event.
                continue;
            }
            final InferenceStreamEvent finish = new InferenceStreamEvent.Finish(reason);
            assertTrue("A stream that finished with " + reason + " must be terminated with [DONE]",
                    SseSerializer.shouldWriteDoneMarker(finish));
        }

        final InferenceStreamEvent usage =
                new InferenceStreamEvent.Usage(new InferenceUsage(82, 17, 99));
        assertTrue("A stream that ends with usage must still be terminated with [DONE]",
                SseSerializer.shouldWriteDoneMarker(usage));

        final InferenceStreamEvent error =
                new InferenceStreamEvent.Error(InferenceError.upstream(UPSTREAM_MESSAGE));
        assertFalse("Only failure withholds the terminal marker",
                SseSerializer.shouldWriteDoneMarker(error));
    }

    /**
     * Given an error event.
     * When it is rendered to a frame.
     * Then the frame is neither equal to nor contains {@link SseSerializer#DONE_MARKER}, so a
     * client scanning the byte stream for the terminator never finds one on a failed stream.
     */
    @Test
    public void test_toFrame_errorEvent_frameNeitherIsNorContainsDoneMarker() {

        final InferenceStreamEvent upstreamFailure =
                new InferenceStreamEvent.Error(InferenceError.upstream(UPSTREAM_MESSAGE));
        final InferenceStreamEvent timeoutFailure =
                new InferenceStreamEvent.Error(completionTimeout());

        for (final InferenceStreamEvent event
                : new InferenceStreamEvent[] {upstreamFailure, timeoutFailure}) {

            final String frame = SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED);

            assertNotNull("The serializer must render an error frame", frame);
            assertTrue("An SSE frame is written behind the data: prefix",
                    frame.startsWith(DATA_PREFIX));
            assertNotEquals("An error frame is not the terminal marker",
                    SseSerializer.DONE_MARKER, frame);
            assertFalse("An error frame must not carry the terminal marker",
                    frame.contains(SseSerializer.DONE_MARKER));
            assertFalse("An error frame must not carry the terminal marker's payload",
                    frame.contains("[DONE]"));
        }
    }

    /**
     * Given the completion timeout expiring mid-generation.
     * When the error event is rendered.
     * Then it renders the same standard error shape as the provider-failure path — a top-level
     * {@code error} object with {@code message} and {@code type}, and no status in the body — so
     * the second way to fail mid-stream is indistinguishable in handling from the first.
     *
     * @throws Exception when the rendered frame cannot be parsed
     */
    @Test
    public void test_toFrame_completionTimeoutErrorEvent_rendersSameShapeAsUpstreamFailure()
            throws Exception {

        final InferenceError timeout = completionTimeout();

        final JsonNode frame = parseFrame(SseSerializer.toFrame(
                new InferenceStreamEvent.Error(timeout), COMPLETION_ID, MODEL, CREATED));

        final JsonNode error = frame.get("error");
        assertNotNull("A timed-out stream's last frame carries a top-level error object", error);
        assertEquals("The timeout's safe description must be carried verbatim",
                TIMEOUT_MESSAGE, error.path("message").asText());
        assertEquals("A timeout is reported in the same error family as any upstream failure",
                timeout.type(), error.path("type").asText());
        assertFalse("The status must not leak into the body", error.has("httpStatus"));
        assertFalse("The status must not leak into the body", error.has("status"));
    }

    /**
     * A timeout-flavoured error — the second way a stream fails once it has begun, when
     * {@code DOT_INFERENCE_COMPLETION_TIMEOUT_SECONDS} expires mid-generation.
     *
     * @return the error the streaming path raises on a completion timeout
     */
    private static InferenceError completionTimeout() {
        return new InferenceError("api_error", TIMEOUT_MESSAGE, null, 504);
    }

    /**
     * Parses a rendered frame's JSON payload, stripping the {@code data: } prefix and the trailing
     * newlines that terminate a server-sent event.
     *
     * @param frame the frame the serializer rendered
     * @return the parsed payload
     * @throws Exception when the payload is not parseable JSON
     */
    private static JsonNode parseFrame(final String frame) throws Exception {

        assertNotNull("The serializer must render a frame", frame);
        assertTrue("An SSE frame is written behind the data: prefix", frame.startsWith(DATA_PREFIX));

        final String payload = frame.substring(DATA_PREFIX.length()).trim();
        return OBJECT_MAPPER.readTree(payload);
    }
}
