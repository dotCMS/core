package com.dotcms.inference;

import com.dotcms.inference.model.InferenceStreamEvent;
import com.dotcms.inference.rest.mapper.SseSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SseSerializer} rendering incremental tool calls (FR-008) on a streamed
 * completion served at {@code /api/inference/v1/chat/completions}.
 *
 * <p>A tool call does not arrive whole. Its identity and name come on the first fragment, its
 * arguments dribble in across later ones, and the only thing tying a continuation fragment to the
 * call it belongs to is {@code index}. A client accumulates those fragments itself, so the
 * serializer's job is to emit exactly what belongs to each fragment and nothing more.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>A first fragment rendering {@code choices[0].delta.tool_calls[0]} with {@code index},
 *       {@code id}, {@code type} of {@code function} and {@code function.name}.</li>
 *   <li>A continuation fragment rendering only {@code index} and {@code function.arguments} —
 *       emitting a null {@code id} would read to a client as a second call starting.</li>
 *   <li>Argument fragments reassembling to the complete argument JSON, expressed both through
 *       {@link InferenceStreamEvent#reassembleArguments(List, int)} and by concatenating what the
 *       rendered frames actually carry.</li>
 *   <li>Two concurrent calls at index 0 and 1 keeping their fragments separate.</li>
 *   <li>The provider-assigned call {@code id} being carried verbatim, never synthesised from the
 *       index — a tool result is correlated back by that exact string.</li>
 * </ul>
 */
public class SseToolCallStreamTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DATA_PREFIX = "data: ";
    private static final String FRAME_TERMINATOR = "\n\n";

    private static final String COMPLETION_ID = "chatcmpl-tools";
    private static final String MODEL = "gpt-4o";
    private static final long CREATED = 1789000000L;

    /**
     * Given the first fragment of a tool call, carrying its id and the tool's name.
     * When it is rendered to a frame.
     * Then {@code choices[0].delta.tool_calls[0]} carries the index, the id, a {@code type} of
     * {@code function} and the function name. (FR-008)
     *
     * @throws Exception if the frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_firstToolCallFragment_rendersIndexIdTypeAndName() throws Exception {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.ToolCallDelta(0, "call_1", "get_weather", "{\"ci");

        final JsonNode toolCall = firstToolCallOf(
                SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        assertEquals(0, toolCall.path("index").asInt());
        assertEquals("call_1", toolCall.path("id").asText());
        assertEquals("function", toolCall.path("type").asText());
        assertEquals("get_weather", toolCall.path("function").path("name").asText());
        assertEquals("{\"ci", toolCall.path("function").path("arguments").asText());
    }

    /**
     * Given a continuation fragment with no id and no name, carrying only more argument text.
     * When it is rendered to a frame.
     * Then only {@code index} and {@code function.arguments} appear — a JSON {@code null} id or
     * name would read to a client as the start of a different call.
     *
     * @throws Exception if the frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_continuationToolCallFragment_omitsNullIdAndName() throws Exception {

        final InferenceStreamEvent event =
                new InferenceStreamEvent.ToolCallDelta(0, null, null, "ty\":\"Bogota\"}");

        final JsonNode toolCall = firstToolCallOf(
                SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        assertEquals(0, toolCall.path("index").asInt());
        assertEquals("ty\":\"Bogota\"}", toolCall.path("function").path("arguments").asText());

        assertFalse("A continuation fragment must not re-emit the call id, not even as null",
                toolCall.has("id"));
        assertFalse("A continuation fragment must not re-emit the tool name, not even as null",
                toolCall.path("function").has("name"));
    }

    /**
     * Given a tool call whose arguments arrive as three fragments.
     * When each fragment is rendered to a frame.
     * Then concatenating the argument text the frames carry yields the complete argument JSON, the
     * same string {@link InferenceStreamEvent#reassembleArguments(List, int)} produces.
     *
     * @throws Exception if a frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_argumentFragments_reassembleToCompleteArguments() throws Exception {

        final List<InferenceStreamEvent> events = List.of(
                new InferenceStreamEvent.ToolCallDelta(0, "call_1", "get_weather", "{\"ci"),
                new InferenceStreamEvent.ToolCallDelta(0, null, null, "ty\":\"Bo"),
                new InferenceStreamEvent.ToolCallDelta(0, null, null, "gota\"}"));

        final String expected = InferenceStreamEvent.reassembleArguments(events, 0);

        assertEquals("The fixture must describe one complete argument object",
                "{\"city\":\"Bogota\"}", expected);
        assertEquals("What the frames carry must reassemble to the complete arguments",
                expected, argumentsCarriedBy(events, 0));
    }

    /**
     * Given two tool calls streamed at the same time, at index 0 and index 1, whose fragments
     * interleave.
     * When each fragment is rendered to a frame.
     * Then the fragments of each call stay separate and each reassembles to its own arguments —
     * {@code index} is the only thing keeping them apart.
     *
     * @throws Exception if a frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_concurrentToolCalls_keepFragmentsSeparatePerIndex() throws Exception {

        final List<InferenceStreamEvent> events = List.of(
                new InferenceStreamEvent.ToolCallDelta(0, "call_1", "get_weather", "{\"city\":"),
                new InferenceStreamEvent.ToolCallDelta(1, "call_2", "get_time", "{\"zo"),
                new InferenceStreamEvent.ToolCallDelta(0, null, null, "\"Bogota\"}"),
                new InferenceStreamEvent.ToolCallDelta(1, null, null, "ne\":\"UTC\"}"));

        assertEquals("{\"city\":\"Bogota\"}",
                InferenceStreamEvent.reassembleArguments(events, 0));
        assertEquals("{\"zone\":\"UTC\"}",
                InferenceStreamEvent.reassembleArguments(events, 1));

        assertEquals("The first call's fragments must not pick up the second call's arguments",
                "{\"city\":\"Bogota\"}", argumentsCarriedBy(events, 0));
        assertEquals("The second call's fragments must not pick up the first call's arguments",
                "{\"zone\":\"UTC\"}", argumentsCarriedBy(events, 1));

        final JsonNode secondCall = firstToolCallOf(
                SseSerializer.toFrame(events.get(1), COMPLETION_ID, MODEL, CREATED));

        assertEquals("The second call must keep its own index", 1, secondCall.path("index").asInt());
        assertEquals("call_2", secondCall.path("id").asText());
        assertEquals("get_time", secondCall.path("function").path("name").asText());
    }

    /**
     * Given a tool call whose provider-assigned id looks nothing like its index.
     * When its first fragment is rendered to a frame.
     * Then that exact id is on the wire — a synthesised or index-derived id would break the
     * correlation a caller needs to hand the tool result back.
     *
     * @throws Exception if the frame does not carry parseable JSON
     */
    @Test
    public void test_toFrame_toolCallId_isCarriedVerbatim() throws Exception {

        final String providerAssignedId = "call_9zQ";

        final InferenceStreamEvent event =
                new InferenceStreamEvent.ToolCallDelta(2, providerAssignedId, "get_weather", "{}");

        final JsonNode toolCall = firstToolCallOf(
                SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

        assertEquals("The provider's id must be emitted verbatim",
                providerAssignedId, toolCall.path("id").asText());
        assertEquals("The index must not be confused with the id", 2, toolCall.path("index").asInt());
        assertFalse("The id must not be derived from the index",
                toolCall.path("id").asText().endsWith("_2"));
    }

    /**
     * Concatenates the argument text the rendered frames carry for one tool call.
     *
     * @param events the events of the turn, in arrival order
     * @param index  the call whose fragments to collect
     * @return the argument text as the frames carry it
     * @throws Exception if a frame does not carry parseable JSON
     */
    private static String argumentsCarriedBy(final List<InferenceStreamEvent> events,
                                             final int index) throws Exception {

        final StringBuilder arguments = new StringBuilder();

        for (final InferenceStreamEvent event : events) {

            final JsonNode toolCall = firstToolCallOf(
                    SseSerializer.toFrame(event, COMPLETION_ID, MODEL, CREATED));

            if (toolCall.path("index").asInt(-1) == index) {
                arguments.append(toolCall.path("function").path("arguments").asText(""));
            }
        }

        return arguments.toString();
    }

    /**
     * Extracts {@code choices[0].delta.tool_calls[0]} from one rendered frame.
     *
     * @param frame the complete frame, prefix and terminator included
     * @return the single tool-call fragment the chunk carries
     * @throws Exception if the payload is not parseable JSON
     */
    private static JsonNode firstToolCallOf(final String frame) throws Exception {

        assertTrue("Frame must begin with the SSE data prefix: " + frame,
                frame.startsWith(DATA_PREFIX));
        assertTrue("Frame must end with a blank line: " + frame, frame.endsWith(FRAME_TERMINATOR));

        final String payload =
                frame.substring(DATA_PREFIX.length(), frame.length() - FRAME_TERMINATOR.length());

        final JsonNode toolCalls = OBJECT_MAPPER.readTree(payload)
                .path("choices").path(0).path("delta").path("tool_calls");

        assertEquals("A tool-call chunk carries exactly one fragment", 1, toolCalls.size());

        return toolCalls.path(0);
    }
}
