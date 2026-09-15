package com.dotcms.inference;

import com.dotcms.inference.model.InferenceStreamEvent;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.rest.mapper.SseSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the streamed usage chunk rendered by
 * {@link SseSerializer#toFrame(InferenceStreamEvent, String, String, long)} (FR-009, research R5).
 *
 * <p>In the chat-completions wire format the usage event is a chunk whose {@code choices} array is
 * <strong>empty</strong>. Some stream readers assume every chunk has a non-empty {@code choices}
 * and break on one that does not, which is why the event is emitted only when the caller asked
 * for it via the standard streaming option — sending it unasked would break exactly the clients
 * this endpoint family exists to support.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>A {@link InferenceStreamEvent.Usage} event rendering a chunk with a present but empty
 *       {@code choices} array.</li>
 *   <li>That chunk still carrying the per-chunk envelope — {@code id}, {@code object}
 *       ({@code chat.completion.chunk}), {@code created}, {@code model} (FR-007).</li>
 *   <li>The counts landing on the wire as {@code usage.prompt_tokens},
 *       {@code usage.completion_tokens} and {@code usage.total_tokens}.</li>
 *   <li>{@link InferenceUsage#UNREPORTED} rendering with the token fields absent or null rather
 *       than as zeros — a fabricated count is indistinguishable from a real one to anyone
 *       reconciling spend.</li>
 *   <li>{@link SseSerializer#shouldWriteDoneMarker(InferenceStreamEvent)} returning {@code true}
 *       after a usage event, so a stream that ends with usage still terminates properly.</li>
 * </ul>
 */
public class SseUsageGatingTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** The prefix every server-sent event payload is written behind. */
    private static final String DATA_PREFIX = "data: ";

    private static final String COMPLETION_ID = "chatcmpl-usage";
    private static final String MODEL = "gpt-4o";
    private static final long CREATED = 1789000000L;

    /**
     * Given a usage event closing a stream.
     * When it is rendered to a frame.
     * Then the chunk carries a {@code choices} array that is present and empty, which is the shape
     * standard clients recognise as a usage-only chunk. (FR-009)
     *
     * @throws Exception when the rendered frame cannot be parsed
     */
    @Test
    public void test_toFrame_usageEvent_rendersEmptyChoicesArray() throws Exception {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.Usage(new InferenceUsage(82, 17, 99));

        final JsonNode chunk = parseFrame(
                SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        final JsonNode choices = chunk.get("choices");
        assertNotNull("A usage chunk must still carry a choices field", choices);
        assertTrue("choices must be an array", choices.isArray());
        assertEquals("A usage chunk carries an empty choices array", 0, choices.size());
    }

    /**
     * Given a usage event.
     * When it is rendered to a frame.
     * Then the chunk carries the same envelope as every other chunk — {@code id}, {@code object},
     * {@code created}, {@code model} — because a client stitching a stream together keys off those
     * on every frame, usage-only ones included. (FR-007)
     *
     * @throws Exception when the rendered frame cannot be parsed
     */
    @Test
    public void test_toFrame_usageEvent_carriesChunkEnvelope() throws Exception {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.Usage(new InferenceUsage(82, 17, 99));

        final JsonNode chunk = parseFrame(
                SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        assertEquals("The usage chunk must carry the completion id",
                COMPLETION_ID, chunk.path("id").asText());
        assertEquals("Every chunk is a chat.completion.chunk",
                SseSerializer.CHUNK_OBJECT, chunk.path("object").asText());
        assertEquals("Every chunk is a chat.completion.chunk",
                "chat.completion.chunk", chunk.path("object").asText());
        assertEquals("The usage chunk must carry the creation time",
                CREATED, chunk.path("created").asLong());
        assertEquals("The usage chunk must name the model that served the completion",
                MODEL, chunk.path("model").asText());
    }

    /**
     * Given a usage event whose counts the provider reported in full.
     * When it is rendered to a frame.
     * Then the numbers appear under {@code usage} as the snake_case wire names standard clients
     * deserialize: {@code prompt_tokens}, {@code completion_tokens}, {@code total_tokens}.
     *
     * @throws Exception when the rendered frame cannot be parsed
     */
    @Test
    public void test_toFrame_reportedUsage_rendersWireTokenFieldNames() throws Exception {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.Usage(new InferenceUsage(82, 17, 99));

        final JsonNode chunk = parseFrame(
                SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        final JsonNode usage = chunk.get("usage");
        assertNotNull("Reported usage must be rendered", usage);
        assertEquals("Prompt tokens ride on usage.prompt_tokens",
                82, usage.path("prompt_tokens").asInt());
        assertEquals("Completion tokens ride on usage.completion_tokens",
                17, usage.path("completion_tokens").asInt());
        assertEquals("Total tokens ride on usage.total_tokens",
                99, usage.path("total_tokens").asInt());

        assertFalse("camelCase variants would not deserialize into a standard client's types",
                usage.has("inputTokens"));
        assertFalse("camelCase variants would not deserialize into a standard client's types",
                usage.has("outputTokens"));
        assertFalse("camelCase variants would not deserialize into a standard client's types",
                usage.has("totalTokens"));
    }

    /**
     * Given a usage event carrying {@link InferenceUsage#UNREPORTED}, the counts a provider did
     * not report.
     * When it is rendered to a frame.
     * Then the token fields are absent or null rather than zero — a fabricated count is
     * indistinguishable from a real one to anyone reconciling spend.
     *
     * @throws Exception when the rendered frame cannot be parsed
     */
    @Test
    public void test_toFrame_unreportedUsage_omitsTokenCountsRatherThanFabricatingZeros()
            throws Exception {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.Usage(InferenceUsage.UNREPORTED);

        final String frame = SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED);
        final JsonNode chunk = parseFrame(frame);

        final JsonNode usage = chunk.get("usage");
        if (usage != null && !usage.isNull()) {
            assertAbsentOrNull("Unreported prompt tokens must never render as zero",
                    usage, "prompt_tokens");
            assertAbsentOrNull("Unreported completion tokens must never render as zero",
                    usage, "completion_tokens");
            assertAbsentOrNull("Unreported total tokens must never render as zero",
                    usage, "total_tokens");
        }

        assertFalse("Unreported usage must not be fabricated as zeroed counts",
                frame.contains("\"prompt_tokens\":0"));
        assertFalse("Unreported usage must not be fabricated as zeroed counts",
                frame.contains("\"completion_tokens\":0"));
        assertFalse("Unreported usage must not be fabricated as zeroed counts",
                frame.contains("\"total_tokens\":0"));
    }

    /**
     * Given a usage event, which on an asking stream is the last event before the terminator.
     * When the serializer is asked whether the terminal marker follows.
     * Then it says yes, so a stream that ends with usage still closes cleanly and a client is not
     * left waiting on a stream that will never terminate. (FR-009)
     */
    @Test
    public void test_shouldWriteDoneMarker_usageEvent_returnsTrue() {

        final InferenceStreamEvent reportedUsage =
                new InferenceStreamEvent.Usage(new InferenceUsage(82, 17, 99));
        final InferenceStreamEvent unreportedUsage =
                new InferenceStreamEvent.Usage(InferenceUsage.UNREPORTED);

        assertTrue("A stream ending with usage must still be terminated with the done marker",
                SseSerializer.shouldWriteDoneMarker(reportedUsage));
        assertTrue("Whether the provider reported counts does not change stream termination",
                SseSerializer.shouldWriteDoneMarker(unreportedUsage));
    }

    /**
     * Given a usage event.
     * When it is rendered to a frame.
     * Then the frame is a {@code data:} frame carrying the chunk, and is not itself the terminal
     * marker — usage precedes {@code [DONE]}, it does not replace it.
     */
    @Test
    public void test_toFrame_usageEvent_rendersDataFrameDistinctFromDoneMarker() {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.Usage(new InferenceUsage(82, 17, 99));

        final String frame = SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED);

        assertNotNull("The serializer must render a frame", frame);
        assertTrue("An SSE frame is written behind the data: prefix", frame.startsWith(DATA_PREFIX));
        assertFalse("The usage frame is not the terminal marker",
                frame.equals(SseSerializer.DONE_MARKER));
    }

    /**
     * Parses a rendered frame's JSON payload, stripping the {@code data: } prefix and the trailing
     * newlines that terminate a server-sent event.
     *
     * @param frame the frame the serializer rendered
     * @return the parsed chunk
     * @throws Exception when the payload is not parseable JSON
     */
    private static JsonNode parseFrame(final String frame) throws Exception {

        assertNotNull("The serializer must render a frame", frame);
        assertTrue("An SSE frame is written behind the data: prefix", frame.startsWith(DATA_PREFIX));

        final String payload = frame.substring(DATA_PREFIX.length()).trim();
        return OBJECT_MAPPER.readTree(payload);
    }

    /**
     * Asserts a field is either absent from the node or explicitly null — never a fabricated value.
     *
     * @param message the failure message
     * @param parent  the node the field would live on
     * @param field   the wire field name
     */
    private static void assertAbsentOrNull(final String message,
                                           final JsonNode parent,
                                           final String field) {

        final JsonNode value = parent.get(field);
        assertTrue(message, value == null || value.isNull());
    }
}
