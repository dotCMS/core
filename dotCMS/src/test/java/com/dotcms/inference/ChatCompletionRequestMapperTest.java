package com.dotcms.inference;

import com.dotcms.inference.model.InferenceMessage;
import com.dotcms.inference.model.InferenceRequest;
import com.dotcms.inference.model.InferenceToolCall;
import com.dotcms.inference.model.InferenceToolSpec;
import com.dotcms.inference.model.ResponseFormat;
import com.dotcms.inference.model.Role;
import com.dotcms.inference.model.ToolChoice;
import com.dotcms.inference.rest.mapper.ChatCompletionMapper;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
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
 * Unit tests for {@link ChatCompletionMapper#toInferenceRequest(ChatCompletionRequestView)} — the
 * translation of the OpenAI chat-completions wire shape into dotCMS's provider-neutral
 * {@link InferenceRequest}.
 *
 * <p>The cases here are the ones where a wire format and an internal model can silently diverge:
 * role naming, tool-call identity, the unparsed JSON text of tool arguments, the JSON Schema of a
 * declared tool, and the sampling parameters a caller pays for. Dropping any of those compiles
 * fine and fails only against a real provider, which is why each gets its own assertion here
 * rather than a round-trip smoke test.</p>
 *
 * <p>Most cases deserialize a realistic payload with Jackson rather than calling the view's
 * canonical constructor, so the {@code @JsonProperty} names on the wire record — {@code max_tokens},
 * {@code tool_call_id}, {@code stream_options} — are exercised alongside the mapping itself.</p>
 */
public class ChatCompletionRequestMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Deserializes a chat-completions payload into the inbound view.
     *
     * @param json the request body as a client would send it
     * @return the bound view
     * @throws Exception when the payload is not readable
     */
    private static ChatCompletionRequestView parse(final String json) throws Exception {
        return MAPPER.readValue(json, ChatCompletionRequestView.class);
    }

    /**
     * Given a conversation carrying every role the format defines, in order,
     * When mapped to the internal request,
     * Then each wire role becomes its {@link Role} constant and the turn order is preserved.
     */
    @Test
    public void test_toInferenceRequest_allFourRoles_mapsRolesAndPreservesOrder() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"You are a helpful assistant.\"},"
                + "{\"role\":\"user\",\"content\":\"What is the weather in Bogota?\"},"
                + "{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["
                + "{\"id\":\"call_1\",\"type\":\"function\",\"function\":"
                + "{\"name\":\"get_weather\",\"arguments\":\"{\\\"city\\\":\\\"Bogota\\\"}\"}}]},"
                + "{\"role\":\"tool\",\"tool_call_id\":\"call_1\",\"content\":\"{\\\"tempC\\\":19}\"}"
                + "]}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        final List<InferenceMessage> messages = request.messages();
        assertEquals("gpt-4o", request.model());
        assertEquals(4, messages.size());
        assertEquals(Role.SYSTEM, messages.get(0).role());
        assertEquals(Role.USER, messages.get(1).role());
        assertEquals(Role.ASSISTANT, messages.get(2).role());
        assertEquals(Role.TOOL, messages.get(3).role());
        assertEquals("You are a helpful assistant.", messages.get(0).content());
        assertEquals("What is the weather in Bogota?", messages.get(1).content());
        assertEquals("{\"tempC\":19}", messages.get(3).content());
    }

    /**
     * Given an assistant turn replaying a tool call the model previously asked for,
     * When mapped to the internal request,
     * Then the provider-assigned id is carried through verbatim and the arguments stay as the
     * unparsed JSON text the model produced.
     */
    @Test
    public void test_toInferenceRequest_toolCallWithId_preservesIdentity() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":["
                + "{\"role\":\"user\",\"content\":\"Weather?\"},"
                + "{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["
                + "{\"id\":\"call_abc123\",\"type\":\"function\",\"function\":"
                + "{\"name\":\"get_weather\",\"arguments\":\"{\\\"city\\\":\\\"Bogota\\\",\\\"unit\\\":\\\"c\\\"}\"}}]}"
                + "]}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        final InferenceMessage assistant = request.messages().get(1);
        assertEquals(Role.ASSISTANT, assistant.role());
        assertTrue(assistant.hasToolCalls());
        assertEquals(1, assistant.toolCalls().size());

        final InferenceToolCall call = assistant.toolCalls().get(0);
        assertEquals("call_abc123", call.id());
        assertEquals("get_weather", call.name());
        assertEquals("{\"city\":\"Bogota\",\"unit\":\"c\"}", call.arguments());
    }

    /**
     * Given a tool result answering an earlier call,
     * When mapped to the internal request,
     * Then the correlating {@code tool_call_id} survives into
     * {@link InferenceMessage#toolCallId()} together with the tool name.
     */
    @Test
    public void test_toInferenceRequest_toolResultTurn_preservesToolCallId() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":["
                + "{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["
                + "{\"id\":\"call_1\",\"type\":\"function\",\"function\":"
                + "{\"name\":\"get_weather\",\"arguments\":\"{}\"}}]},"
                + "{\"role\":\"tool\",\"tool_call_id\":\"call_1\",\"name\":\"get_weather\","
                + "\"content\":\"{\\\"tempC\\\":19}\"}"
                + "]}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        final InferenceMessage toolResult = request.messages().get(1);
        assertEquals(Role.TOOL, toolResult.role());
        assertEquals("call_1", toolResult.toolCallId());
        assertEquals("get_weather", toolResult.name());
        assertFalse(toolResult.hasToolCalls());
    }

    /**
     * Given a declared tool with a name, a description and a JSON Schema for its arguments,
     * When mapped to the internal request,
     * Then an {@link InferenceToolSpec} carries all three, with the schema node intact.
     */
    @Test
    public void test_toInferenceRequest_toolDeclaration_mapsNameDescriptionAndSchema() throws Exception {
        final String schema = "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}},"
                + "\"required\":[\"city\"]}";
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Weather?\"}],"
                + "\"tools\":[{\"type\":\"function\",\"function\":{"
                + "\"name\":\"get_weather\",\"description\":\"Current weather for a city\","
                + "\"parameters\":" + schema + "}}]}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertTrue(request.hasTools());
        assertEquals(1, request.tools().size());

        final InferenceToolSpec spec = request.tools().get(0);
        final JsonNode expectedSchema = MAPPER.readTree(schema);
        assertEquals("get_weather", spec.name());
        assertEquals("Current weather for a city", spec.description());
        assertNotNull(spec.parameters());
        assertEquals(expectedSchema, spec.parameters());
    }

    /**
     * Given the string tool choice {@code "auto"},
     * When mapped to the internal request,
     * Then the choice is {@link ToolChoice.Mode#AUTO} and names no function.
     */
    @Test
    public void test_toInferenceRequest_toolChoiceAuto_mapsToAutoMode() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"tool_choice\":\"auto\"}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertNotNull(request.toolChoice());
        assertEquals(ToolChoice.Mode.AUTO, request.toolChoice().mode());
        assertNull(request.toolChoice().function());
    }

    /**
     * Given the string tool choice {@code "required"},
     * When mapped to the internal request,
     * Then the choice is {@link ToolChoice.Mode#REQUIRED}.
     */
    @Test
    public void test_toInferenceRequest_toolChoiceRequired_mapsToRequiredMode() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"tool_choice\":\"required\"}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertNotNull(request.toolChoice());
        assertEquals(ToolChoice.Mode.REQUIRED, request.toolChoice().mode());
        assertNull(request.toolChoice().function());
    }

    /**
     * Given the string tool choice {@code "none"},
     * When mapped to the internal request,
     * Then the choice is {@link ToolChoice.Mode#NONE}.
     */
    @Test
    public void test_toInferenceRequest_toolChoiceNone_mapsToNoneMode() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"tool_choice\":\"none\"}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertNotNull(request.toolChoice());
        assertEquals(ToolChoice.Mode.NONE, request.toolChoice().mode());
        assertNull(request.toolChoice().function());
    }

    /**
     * Given an object tool choice naming one function,
     * When mapped to the internal request,
     * Then the choice is {@link ToolChoice.Mode#FUNCTION} carrying that function's name.
     */
    @Test
    public void test_toInferenceRequest_toolChoiceNamedFunction_mapsToFunctionMode() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"tool_choice\":{\"type\":\"function\",\"function\":{\"name\":\"get_weather\"}}}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertNotNull(request.toolChoice());
        assertEquals(ToolChoice.Mode.FUNCTION, request.toolChoice().mode());
        assertEquals("get_weather", request.toolChoice().function());
    }

    /**
     * Given a response format of {@code {"type":"text"}},
     * When mapped to the internal request,
     * Then the format is {@link ResponseFormat.Type#TEXT} with no schema.
     */
    @Test
    public void test_toInferenceRequest_responseFormatText_mapsToTextType() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"response_format\":{\"type\":\"text\"}}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertNotNull(request.responseFormat());
        assertEquals(ResponseFormat.Type.TEXT, request.responseFormat().type());
        assertNull(request.responseFormat().schema());
    }

    /**
     * Given a response format of {@code {"type":"json_object"}},
     * When mapped to the internal request,
     * Then the format is {@link ResponseFormat.Type#JSON_OBJECT} with no schema.
     */
    @Test
    public void test_toInferenceRequest_responseFormatJsonObject_mapsToJsonObjectType() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"response_format\":{\"type\":\"json_object\"}}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertNotNull(request.responseFormat());
        assertEquals(ResponseFormat.Type.JSON_OBJECT, request.responseFormat().type());
        assertNull(request.responseFormat().schema());
    }

    /**
     * Given a request carrying temperature, max_tokens, top_p and stop,
     * When mapped to the internal request,
     * Then every sampling parameter reaches the internal field it belongs to — a dropped
     * {@code max_tokens} is both a cost and a correctness failure, so it is asserted by value.
     */
    @Test
    public void test_toInferenceRequest_samplingParameters_passThroughUnchanged() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"temperature\":0.7,"
                + "\"max_tokens\":1024,"
                + "\"top_p\":0.95,"
                + "\"stop\":[\"\\n\\n\",\"END\"]}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertEquals(Double.valueOf(0.7), request.temperature());
        assertEquals(Integer.valueOf(1024), request.maxOutputTokens());
        assertEquals(Double.valueOf(0.95), request.topP());
        assertEquals(List.of("\n\n", "END"), request.stopSequences());
    }

    /**
     * Given a request that says nothing about streaming,
     * When mapped to the internal request,
     * Then both {@code stream} and {@code includeUsageInStream} default to false, and the optional
     * sampling parameters stay null rather than being invented.
     */
    @Test
    public void test_toInferenceRequest_streamAbsent_defaultsToFalse() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}]}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertFalse(request.stream());
        assertFalse(request.includeUsageInStream());
        assertNull(request.temperature());
        assertNull(request.maxOutputTokens());
        assertNull(request.topP());
        assertTrue(request.stopSequences().isEmpty());
    }

    /**
     * Given a streaming request asking for usage through the standard streaming option,
     * When mapped to the internal request,
     * Then {@code stream} is true and {@code includeUsageInStream} is true.
     */
    @Test
    public void test_toInferenceRequest_streamOptionsIncludeUsage_mapsToIncludeUsageInStream()
            throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],"
                + "\"stream\":true,"
                + "\"stream_options\":{\"include_usage\":true}}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertTrue(request.stream());
        assertTrue(request.includeUsageInStream());
    }

    /**
     * Given a streaming request with no stream options at all,
     * When mapped to the internal request,
     * Then streaming is on but usage is withheld — {@code includeUsageInStream} defaults to false.
     * The view is built through its canonical constructor here, so the default does not
     * depend on Jackson leaving the field null.
     */
    @Test
    public void test_toInferenceRequest_streamOptionsAbsent_includeUsageDefaultsToFalse() {
        final ChatCompletionRequestView view = new ChatCompletionRequestView(
                "gpt-4o",
                List.of(new ChatCompletionRequestView.MessageView("user", "Hi", null, null, null)),
                null,
                null,
                null,
                Boolean.TRUE,
                null,
                null,
                null,
                null,
                null,
                null);

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(view);

        assertTrue(request.stream());
        assertFalse(request.includeUsageInStream());
    }

    /**
     * Given a payload carrying fields this family has no opinion about,
     * When mapped to the internal request,
     * Then the incidental fields are ignored and the fields that do matter still map, so a
     * client's default payload is not rejected over a field that changes nothing.
     */
    @Test
    public void test_toInferenceRequest_unknownFields_areIgnored() throws Exception {
        final String json = "{"
                + "\"model\":\"gpt-4o\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"Hi\",\"extra_turn_field\":42}],"
                + "\"user\":\"user-123\","
                + "\"presence_penalty\":0.1,"
                + "\"frequency_penalty\":0.2,"
                + "\"logit_bias\":{\"50256\":-100},"
                + "\"seed\":7,"
                + "\"service_tier\":\"auto\","
                + "\"max_tokens\":256}";

        final InferenceRequest request = ChatCompletionMapper.toInferenceRequest(parse(json));

        assertEquals("gpt-4o", request.model());
        assertEquals(1, request.messages().size());
        assertEquals(Role.USER, request.messages().get(0).role());
        assertEquals("Hi", request.messages().get(0).content());
        assertEquals(Integer.valueOf(256), request.maxOutputTokens());
    }
}
