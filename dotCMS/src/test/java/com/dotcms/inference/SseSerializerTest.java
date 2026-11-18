package com.dotcms.inference;

import com.dotcms.inference.model.FinishReason;
import com.dotcms.inference.model.InferenceStreamEvent;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.rest.mapper.SseSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SseSerializer}, which renders internal {@link InferenceStreamEvent}s as the
 * server-sent events a standard chat-completions client reads from
 * {@code /api/inference/v1/chat/completions} when {@code "stream": true}.
 *
 * <p>This class covers the chunk envelope; incremental tool calls are covered by
 * {@code SseToolCallStreamTest}.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>Frame framing — a {@code data: } prefix and the blank-line terminator {@code \n\n} that
 *       tells an SSE reader the event is complete. Without the terminator a client blocks holding
 *       a chunk it already has.</li>
 *   <li>Envelope fields on every chunk — {@code id}, {@code object} equal to
 *       {@code chat.completion.chunk}, {@code created} and {@code model}.</li>
 *   <li>The id, model and creation time being the ones passed in, and staying identical across
 *       successive chunks of the same completion — a client correlates chunks by that id.</li>
 *   <li>A content fragment landing at {@code choices[0].delta.content} with {@code index} 0.</li>
 *   <li>A finish rendering the wire finish-reason string with no content left in the delta, which
 *       is what distinguishes a completed answer from a truncated one.</li>
 *   <li>{@link SseSerializer#shouldWriteDoneMarker(InferenceStreamEvent)} being true after a
 *       finish and after a usage event — the two events that legitimately end a stream.</li>
 *   <li>{@link SseSerializer#DONE_MARKER} being exactly {@code data: [DONE]\n\n}, since a client
 *       detects end-of-stream by matching those bytes.</li>
 * </ul>
 */
public class SseSerializerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DATA_PREFIX = "data: ";
    private static final String FRAME_TERMINATOR = "\n\n";

    private static final String COMPLETION_ID = "chatcmpl-8f3a";
    private static final String MODEL = "gpt-4o";
    private static final long CREATED = 1789000000L;

    /**
     * Given a content fragment of an answer.
     * When it is rendered to a frame.
     * Then the frame begins with {@code data: } and ends with the blank-line terminator that marks
     * an SSE event complete.
     */
    @Test
    public void test_toFrame_contentDelta_isDataPrefixedAndBlankLineTerminated() {

        final InferenceStreamEvent event = new InferenceStreamEvent.ContentDelta("Hello");

        final String frame = SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED);

        assertTrue("A streamed frame must begin with the SSE data prefix: " + frame,
                frame.startsWith(DATA_PREFIX));
        assertTrue("A streamed frame must end with a blank line or the client never dispatches it",
                frame.endsWith(FRAME_TERMINATOR));
        assertFalse("The payload between prefix and terminator must not be empty",
                payloadOf(frame).isBlank());
    }

    /**
     * Given a content fragment of an answer.
     * When it is rendered to a frame.
     * Then the parsed chunk carries every envelope field, with {@code object} equal to
     * {@code chat.completion.chunk}.
     *
     * @throws Exception if the frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_contentDelta_carriesChunkEnvelopeFields() throws Exception {

        final InferenceStreamEvent event = new InferenceStreamEvent.ContentDelta("Hello");

        final JsonNode chunk = parseFrame(SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        assertTrue("Every chunk must carry an id", chunk.has("id"));
        assertTrue("Every chunk must carry an object", chunk.has("object"));
        assertTrue("Every chunk must carry a created", chunk.has("created"));
        assertTrue("Every chunk must carry a model", chunk.has("model"));
        assertEquals("chat.completion.chunk", chunk.path("object").asText());
        assertEquals(SseSerializer.CHUNK_OBJECT, chunk.path("object").asText());
    }

    /**
     * Given two content fragments of the same completion rendered with the same id, model and
     * creation time.
     * When both are rendered to frames.
     * Then both carry exactly the values passed in — a client correlates the chunks of one
     * completion by an id that never changes mid-stream.
     *
     * @throws Exception if a frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_successiveChunks_repeatTheSameIdModelAndCreated() throws Exception {

        final JsonNode first = parseFrame(SseSerializer.toFrame(
                new InferenceStreamEvent.ContentDelta("The weather "), COMPLETION_ID, MODEL, CREATED));
        final JsonNode second = parseFrame(SseSerializer.toFrame(
                new InferenceStreamEvent.ContentDelta("is 19C."), COMPLETION_ID, MODEL, CREATED));

        assertEquals(COMPLETION_ID, first.path("id").asText());
        assertEquals(MODEL, first.path("model").asText());
        assertEquals(CREATED, first.path("created").asLong());

        assertEquals("The id must not change between chunks of one completion",
                first.path("id").asText(), second.path("id").asText());
        assertEquals("The model must not change between chunks of one completion",
                first.path("model").asText(), second.path("model").asText());
        assertEquals("The creation time must not change between chunks of one completion",
                first.path("created").asLong(), second.path("created").asLong());
    }

    /**
     * Given a content fragment of an answer.
     * When it is rendered to a frame.
     * Then the text sits at {@code choices[0].delta.content} with {@code choices[0].index} of 0.
     *
     * @throws Exception if the frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_contentDelta_placesTextInFirstChoiceDelta() throws Exception {

        final String fragment = "The weather in Bogota ";

        final JsonNode chunk = parseFrame(SseSerializer.toFrame(
                new InferenceStreamEvent.ContentDelta(fragment), COMPLETION_ID, MODEL, CREATED));

        final JsonNode choice = chunk.path("choices").path(0);

        assertEquals("A content chunk carries exactly one choice", 1, chunk.path("choices").size());
        assertEquals(0, choice.path("index").asInt());
        assertEquals(fragment, choice.path("delta").path("content").asText());
    }

    /**
     * Given each finish reason that has a wire equivalent.
     * When the finish event is rendered to a frame.
     * Then {@code choices[0].finish_reason} is the wire string and the delta carries no leftover
     * content — the finish chunk announces the end, it does not add to the answer.
     *
     * @throws Exception if a frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_finishEvent_rendersWireFinishReasonWithEmptyDelta() throws Exception {

        assertEquals("stop", finishReasonOnWireFor(FinishReason.STOP));
        assertEquals("length", finishReasonOnWireFor(FinishReason.LENGTH));
        assertEquals("tool_calls", finishReasonOnWireFor(FinishReason.TOOL_CALLS));
        assertEquals("content_filter", finishReasonOnWireFor(FinishReason.CONTENT_FILTER));

        final JsonNode chunk = parseFrame(SseSerializer.toFrame(
                new InferenceStreamEvent.Finish(FinishReason.STOP), COMPLETION_ID, MODEL, CREATED));
        final JsonNode choice = chunk.path("choices").path(0);
        final JsonNode delta = choice.path("delta");

        assertEquals(0, choice.path("index").asInt());
        assertTrue("A finish chunk must not carry content in its delta",
                delta.isMissingNode() || delta.isNull() || delta.isEmpty()
                        || delta.path("content").asText("").isEmpty());
        assertFalse("A finish chunk must not carry tool-call fragments", delta.has("tool_calls"));
    }

    /**
     * Given a stream that ended with a normal finish.
     * When the serializer is asked whether to close the stream.
     * Then it says yes, so the client sees a cleanly finished stream rather than a truncated one.
     */
    @Test
    public void test_shouldWriteDoneMarker_afterFinish_returnsTrue() {

        final InferenceStreamEvent finish = new InferenceStreamEvent.Finish(FinishReason.STOP);

        assertTrue("A normally finished stream must be closed with the done marker",
                SseSerializer.shouldWriteDoneMarker(finish));
    }

    /**
     * Given a stream whose last event is the usage chunk the caller asked for.
     * When the serializer is asked whether to close the stream.
     * Then it says yes — usage arrives immediately before the terminal marker.
     */
    @Test
    public void test_shouldWriteDoneMarker_afterUsage_returnsTrue() {

        final InferenceStreamEvent usage =
                new InferenceStreamEvent.Usage(new InferenceUsage(82, 17, 99));

        assertTrue("A usage chunk is the last event before the done marker",
                SseSerializer.shouldWriteDoneMarker(usage));
    }

    /**
     * Given a client that detects end-of-stream by matching the terminal marker byte for byte.
     * When the marker constant is read.
     * Then it is exactly {@code data: [DONE]} followed by the blank-line terminator.
     */
    @Test
    public void test_doneMarker_constant_isExactlyTheWireBytes() {

        assertEquals("data: [DONE]\n\n", SseSerializer.DONE_MARKER);
    }

    /**
     * Renders a finish event carrying the given reason and returns the wire string it produced.
     *
     * @param finishReason the internal reason generation stopped
     * @return the {@code finish_reason} on the rendered chunk's first choice
     * @throws Exception if the frame does not carry parseable JSON
     */
    private static String finishReasonOnWireFor(final FinishReason finishReason) throws Exception {

        final JsonNode chunk = parseFrame(SseSerializer.toFrame(
                new InferenceStreamEvent.Finish(finishReason), COMPLETION_ID, MODEL, CREATED));

        return chunk.path("choices").path(0).path("finish_reason").asText();
    }

    /**
     * Parses the JSON payload carried by one rendered SSE frame.
     *
     * @param frame the complete frame, prefix and terminator included
     * @return the parsed chunk
     * @throws Exception if the payload is not parseable JSON
     */
    private static JsonNode parseFrame(final String frame) throws Exception {

        return OBJECT_MAPPER.readTree(payloadOf(frame));
    }

    /**
     * Strips the {@code data: } prefix and the trailing blank line from a rendered frame.
     *
     * @param frame the complete frame
     * @return the JSON text the frame carries
     */
    private static String payloadOf(final String frame) {

        assertTrue("Frame must begin with the SSE data prefix: " + frame,
                frame.startsWith(DATA_PREFIX));
        assertTrue("Frame must end with a blank line: " + frame, frame.endsWith(FRAME_TERMINATOR));

        return frame.substring(DATA_PREFIX.length(), frame.length() - FRAME_TERMINATOR.length());
    }
}
