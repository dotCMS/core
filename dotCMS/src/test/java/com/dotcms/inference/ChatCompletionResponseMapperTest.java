package com.dotcms.inference;

import com.dotcms.inference.model.FinishReason;
import com.dotcms.inference.model.InferenceMessage;
import com.dotcms.inference.model.InferenceResponse;
import com.dotcms.inference.model.InferenceToolCall;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.model.Role;
import com.dotcms.inference.rest.mapper.ChatCompletionMapper;
import com.dotcms.inference.rest.view.ChatCompletionView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ChatCompletionMapper#toView(InferenceResponse)}, the outbound half of the
 * translation between dotCMS's internal {@link InferenceResponse} and the OpenAI-compatible
 * chat-completions wire shape served at {@code /api/inference/v1/chat/completions}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>Envelope fields — {@code id}, {@code object}, {@code created}, {@code model}.</li>
 *   <li>{@code model} reporting the model that actually served the request, which is how a caller
 *       detects that a site's fallback chain moved on to a later entry.</li>
 *   <li>A text answer rendering as exactly one choice, index {@code 0}, role {@code assistant},
 *       finish reason {@code stop}.</li>
 *   <li>A tool-calling answer rendering {@code tool_calls}, with the provider-assigned id carried
 *       verbatim rather than synthesised.</li>
 *   <li>Every serializable {@link FinishReason} mapping to its wire string.</li>
 *   <li>Usage mapping to {@code prompt_tokens} / {@code completion_tokens} / {@code total_tokens},
 *       and {@link InferenceUsage#UNREPORTED} leaving usage absent rather than fabricating zeros —
 *       a made-up count is indistinguishable from a real one to anyone reconciling spend.</li>
 *   <li>Jackson serialization producing the snake_case wire field names, since the point of the
 *       whole family is byte-compatibility with standard clients.</li>
 * </ul>
 */
public class ChatCompletionResponseMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Given a completed response with an id, a model and a creation time.
     * When it is rendered to the wire shape.
     * Then every envelope field is populated and {@code object} is {@code chat.completion}.
     */
    @Test
    public void test_toView_textResponse_populatesEnvelopeFields() {

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-8f3a",
                "gpt-4o",
                1789000000L,
                InferenceMessage.of(Role.ASSISTANT, "The weather in Bogota is 19C."),
                FinishReason.STOP,
                new InferenceUsage(82, 17, 99));

        final ChatCompletionView view = ChatCompletionMapper.toView(response);

        assertNotNull("The mapper must return a view", view);
        assertEquals("chatcmpl-8f3a", view.id());
        assertEquals("chat.completion", view.object());
        assertEquals(ChatCompletionView.OBJECT, view.object());
        assertEquals(1789000000L, view.created());
        assertEquals("gpt-4o", view.model());
    }

    /**
     * Given a response served by a fallback model, not the one the caller asked for.
     * When it is rendered to the wire shape.
     * Then {@code model} names the model that actually ran, which is the only signal a caller has
     * that a fallback-chain hop happened.
     */
    @Test
    public void test_toView_fallbackModelServedRequest_reportsServingModel() {

        final String modelTheCallerAskedFor = "gpt-4o";
        final String modelThatActuallyServed = "gpt-4o-mini";

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-fallback",
                modelThatActuallyServed,
                1789000001L,
                InferenceMessage.of(Role.ASSISTANT, "Served by the next chain entry."),
                FinishReason.STOP,
                InferenceUsage.UNREPORTED);

        final ChatCompletionView view = ChatCompletionMapper.toView(response);

        assertEquals("The served model must be reported, not the requested one",
                modelThatActuallyServed, view.model());
        assertFalse("The requested model must not be echoed back when a fallback served the request",
                modelTheCallerAskedFor.equals(view.model()));
    }

    /**
     * Given an assistant turn carrying plain text.
     * When it is rendered to the wire shape.
     * Then there is exactly one choice at index 0, with role {@code assistant}, the content, and
     * finish reason {@code stop}.
     */
    @Test
    public void test_toView_textAnswer_mapsToSingleAssistantChoice() {

        final String content = "Bogota is 19C right now.";

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-text",
                "gpt-4o",
                1789000002L,
                InferenceMessage.of(Role.ASSISTANT, content),
                FinishReason.STOP,
                InferenceUsage.UNREPORTED);

        final ChatCompletionView view = ChatCompletionMapper.toView(response);

        assertNotNull("Choices must be present", view.choices());
        assertEquals("Exactly one choice is supported", 1, view.choices().size());

        final ChatCompletionView.ChoiceView choice = view.choices().get(0);
        assertEquals(0, choice.index());
        assertEquals("stop", choice.finishReason());

        final ChatCompletionView.MessageView message = choice.message();
        assertNotNull("The choice must carry a message", message);
        assertEquals("assistant", message.role());
        assertEquals(content, message.content());
        assertTrue("A text answer carries no tool calls",
                message.toolCalls() == null || message.toolCalls().isEmpty());
    }

    /**
     * Given an assistant turn asking for two tools to be executed.
     * When it is rendered to the wire shape.
     * Then the finish reason is {@code tool_calls} and each entry carries its provider-assigned id
     * verbatim, type {@code function}, and the function name and raw JSON arguments.
     */
    @Test
    public void test_toView_toolCallAnswer_mapsToolCallsCarryingIdVerbatim() {

        final InferenceToolCall firstCall =
                new InferenceToolCall("call_1", "get_weather", "{\"city\":\"Bogota\"}", 0);
        final InferenceToolCall secondCall =
                new InferenceToolCall("call_2", "get_time", "{\"tz\":\"America/Bogota\"}", 1);

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-tools",
                "gpt-4o",
                1789000003L,
                InferenceMessage.ofToolCalls(null, List.of(firstCall, secondCall)),
                FinishReason.TOOL_CALLS,
                InferenceUsage.UNREPORTED);

        final ChatCompletionView view = ChatCompletionMapper.toView(response);

        assertEquals("Exactly one choice is supported", 1, view.choices().size());

        final ChatCompletionView.ChoiceView choice = view.choices().get(0);
        assertEquals("tool_calls", choice.finishReason());

        final ChatCompletionView.MessageView message = choice.message();
        assertEquals("assistant", message.role());
        assertNull("A tool-calling turn with no text carries null content", message.content());

        final List<ChatCompletionView.ToolCallView> toolCalls = message.toolCalls();
        assertNotNull("Tool calls must be present", toolCalls);
        assertEquals(2, toolCalls.size());

        final ChatCompletionView.ToolCallView first = toolCalls.get(0);
        assertEquals("The provider-assigned id must be carried verbatim", "call_1", first.id());
        assertEquals("function", first.type());
        assertNotNull("The function payload must be present", first.function());
        assertEquals("get_weather", first.function().name());
        assertEquals("Arguments are passed through unparsed",
                "{\"city\":\"Bogota\"}", first.function().arguments());

        final ChatCompletionView.ToolCallView second = toolCalls.get(1);
        assertEquals("The provider-assigned id must be carried verbatim", "call_2", second.id());
        assertEquals("function", second.type());
        assertEquals("get_time", second.function().name());
        assertEquals("{\"tz\":\"America/Bogota\"}", second.function().arguments());
    }

    /**
     * Given each {@link FinishReason} that has a wire equivalent.
     * When a response carrying it is rendered.
     * Then the choice's {@code finish_reason} is the matching wire string.
     */
    @Test
    public void test_toView_finishReasons_mapToWireStrings() {

        assertEquals("stop", finishReasonOnWireFor(FinishReason.STOP));
        assertEquals("length", finishReasonOnWireFor(FinishReason.LENGTH));
        assertEquals("tool_calls", finishReasonOnWireFor(FinishReason.TOOL_CALLS));
        assertEquals("content_filter", finishReasonOnWireFor(FinishReason.CONTENT_FILTER));
    }

    /**
     * Given a response whose provider reported token counts.
     * When it is rendered to the wire shape.
     * Then usage carries {@code prompt_tokens}, {@code completion_tokens} and {@code total_tokens}.
     */
    @Test
    public void test_toView_reportedUsage_mapsTokenCounts() {

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-usage",
                "gpt-4o",
                1789000004L,
                InferenceMessage.of(Role.ASSISTANT, "Counted."),
                FinishReason.STOP,
                new InferenceUsage(82, 17, 99));

        final ChatCompletionView view = ChatCompletionMapper.toView(response);

        final ChatCompletionView.UsageView usage = view.usage();
        assertNotNull("Reported usage must be rendered", usage);
        assertEquals(Integer.valueOf(82), usage.promptTokens());
        assertEquals(Integer.valueOf(17), usage.completionTokens());
        assertEquals(Integer.valueOf(99), usage.totalTokens());
    }

    /**
     * Given a response whose provider reported no usage at all.
     * When it is rendered to the wire shape.
     * Then usage is absent rather than zeroed — a fabricated count is indistinguishable from a real
     * one to anyone reconciling spend.
     */
    @Test
    public void test_toView_unreportedUsage_omitsUsageRatherThanFabricatingZeros() {

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-nousage",
                "gpt-4o",
                1789000005L,
                InferenceMessage.of(Role.ASSISTANT, "Uncounted."),
                FinishReason.STOP,
                InferenceUsage.UNREPORTED);

        final ChatCompletionView view = ChatCompletionMapper.toView(response);

        assertNull("Unreported usage must be absent, never zeros", view.usage());
    }

    /**
     * Given a rendered tool-calling response with reported usage.
     * When it is serialized with Jackson.
     * Then the JSON carries the snake_case wire field names standard clients deserialize —
     * {@code finish_reason}, {@code tool_calls}, {@code prompt_tokens} — and no camelCase variants.
     *
     * @throws Exception when serialization fails
     */
    @Test
    public void test_toView_serializedWithJackson_producesWireFieldNames() throws Exception {

        final InferenceToolCall toolCall =
                new InferenceToolCall("call_1", "get_weather", "{\"city\":\"Bogota\"}", 0);

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-wire",
                "gpt-4o",
                1789000006L,
                InferenceMessage.ofToolCalls(null, List.of(toolCall)),
                FinishReason.TOOL_CALLS,
                new InferenceUsage(82, 17, 99));

        final ChatCompletionView view = ChatCompletionMapper.toView(response);
        final String json = OBJECT_MAPPER.writeValueAsString(view);
        final JsonNode node = OBJECT_MAPPER.readTree(json);

        assertEquals("chatcmpl-wire", node.path("id").asText());
        assertEquals("chat.completion", node.path("object").asText());
        assertEquals(1789000006L, node.path("created").asLong());
        assertEquals("gpt-4o", node.path("model").asText());

        final JsonNode choice = node.path("choices").path(0);
        assertEquals(0, choice.path("index").asInt());
        assertTrue("finish_reason must be the wire field name",
                choice.has("finish_reason"));
        assertEquals("tool_calls", choice.path("finish_reason").asText());
        assertFalse("camelCase finishReason must not leak onto the wire",
                choice.has("finishReason"));

        final JsonNode message = choice.path("message");
        assertEquals("assistant", message.path("role").asText());
        assertTrue("tool_calls must be the wire field name", message.has("tool_calls"));
        assertFalse("camelCase toolCalls must not leak onto the wire", message.has("toolCalls"));

        final JsonNode wireToolCall = message.path("tool_calls").path(0);
        assertEquals("call_1", wireToolCall.path("id").asText());
        assertEquals("function", wireToolCall.path("type").asText());
        assertEquals("get_weather", wireToolCall.path("function").path("name").asText());
        assertEquals("{\"city\":\"Bogota\"}",
                wireToolCall.path("function").path("arguments").asText());

        final JsonNode usage = node.path("usage");
        assertTrue("prompt_tokens must be the wire field name", usage.has("prompt_tokens"));
        assertFalse("camelCase promptTokens must not leak onto the wire", usage.has("promptTokens"));
        assertEquals(82, usage.path("prompt_tokens").asInt());
        assertEquals(17, usage.path("completion_tokens").asInt());
        assertEquals(99, usage.path("total_tokens").asInt());
    }

    /**
     * Renders a minimal response carrying the given finish reason and returns the wire string the
     * mapper produced for it.
     *
     * @param finishReason the internal reason generation stopped
     * @return the {@code finish_reason} on the rendered choice
     */
    private static String finishReasonOnWireFor(final FinishReason finishReason) {

        final InferenceMessage message = finishReason == FinishReason.TOOL_CALLS
                ? InferenceMessage.ofToolCalls(null,
                        List.of(InferenceToolCall.of("call_1", "get_weather", "{}")))
                : InferenceMessage.of(Role.ASSISTANT, "An answer.");

        final InferenceResponse response = new InferenceResponse(
                "chatcmpl-" + finishReason.name().toLowerCase(),
                "gpt-4o",
                1789000007L,
                message,
                finishReason,
                InferenceUsage.UNREPORTED);

        return ChatCompletionMapper.toView(response).choices().get(0).finishReason();
    }
}
