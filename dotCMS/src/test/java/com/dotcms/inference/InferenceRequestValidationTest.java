package com.dotcms.inference;

import com.dotcms.inference.model.InferenceMessage;
import com.dotcms.inference.model.InferenceRequest;
import com.dotcms.inference.model.InferenceToolCall;
import com.dotcms.inference.model.Role;
import com.dotcms.inference.rest.mapper.ChatCompletionMapper;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.MessageView;
import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Specifies request validation for the OpenAI-compatible inference family, across the two layers
 * that share responsibility for it.
 *
 * <p>The first group exercises the internal representation directly — the compact constructors of
 * {@link InferenceRequest} and {@link InferenceMessage}, which already enforce their invariants.
 * These tests pass today and are here to pin that behaviour down so a later refactor of the model
 * cannot quietly relax it.</p>
 *
 * <p>The second group exercises
 * {@link ChatCompletionMapper#toInferenceRequest(ChatCompletionRequestView)}, which translates the
 * wire shape into the internal one and is where wire-level validation belongs. That method is a
 * skeleton at the time of writing, so these tests fail — deliberately. They are the specification
 * the implementation is written against, in particular the FR-013 line: a field this family has no
 * opinion about is ignored, but a field that changes what the caller gets or pays for — {@code n}
 * being the example — must fail loudly rather than be silently dropped.</p>
 *
 * <p>Where a validation error is asserted, the assertion is on the error naming the offending
 * field or identity. A validation error that does not say what was wrong sends the caller back to
 * guessing, which is most of what these rules exist to prevent.</p>
 *
 * @see InferenceRequest
 * @see InferenceMessage
 * @see ChatCompletionMapper
 */
public class InferenceRequestValidationTest {

    private static final String VALID_MODEL = "gpt-4o";

    // ---------------------------------------------------------------------
    // Internal representation — InferenceRequest
    // ---------------------------------------------------------------------

    /**
     * Given a request built with a blank model,
     * When the record is constructed,
     * Then it is rejected naming {@code model} — there is no implicit default (FR-024).
     */
    @Test
    public void test_inferenceRequest_blankModel_throwsNamingModel() {
        final List<InferenceMessage> messages = List.of(InferenceMessage.of(Role.USER, "hello"));

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> InferenceRequest.builder("   ").messages(messages).build());

        assertMessageNamesToken(thrown.getMessage(), "model");
    }

    /**
     * Given a request built with a null model,
     * When the record is constructed,
     * Then it is rejected naming {@code model} rather than defaulting to some house model.
     */
    @Test
    public void test_inferenceRequest_nullModel_throwsNamingModel() {
        final List<InferenceMessage> messages = List.of(InferenceMessage.of(Role.USER, "hello"));

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> InferenceRequest.builder(null).messages(messages).build());

        assertMessageNamesToken(thrown.getMessage(), "model");
    }

    /**
     * Given a request carrying an empty conversation,
     * When the record is constructed,
     * Then it is rejected naming {@code message} — there is nothing to infer from (FR-002).
     */
    @Test
    public void test_inferenceRequest_emptyMessages_throwsNamingMessages() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> InferenceRequest.builder(VALID_MODEL).messages(List.of()).build());

        assertMessageNamesToken(thrown.getMessage(), "message");
    }

    /**
     * Given a TOOL turn whose {@code toolCallId} answers a call no earlier assistant turn made,
     * When the record is constructed,
     * Then it is rejected and the error names the orphaned identity, so the caller can find it
     * without diffing the conversation by hand (FR-005).
     */
    @Test
    public void test_inferenceRequest_uncorrelatedToolResult_throwsNamingOffendingId() {
        final String orphanId = "call_nobody_asked_for";
        final List<InferenceMessage> messages = List.of(
                InferenceMessage.of(Role.USER, "what is the weather?"),
                InferenceMessage.ofToolResult(orphanId, "get_weather", "{\"tempC\":21}"));

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> InferenceRequest.builder(VALID_MODEL).messages(messages).build());

        assertMessageNamesToken(thrown.getMessage(), orphanId);
    }

    /**
     * Given a TOOL turn whose {@code toolCallId} matches a call a preceding assistant turn made,
     * When the record is constructed,
     * Then it is accepted and the conversation survives intact — correlation is by identity, not
     * by position (FR-005).
     */
    @Test
    public void test_inferenceRequest_correlatedToolResult_isAccepted() {
        final String callId = "call_abc123";
        final List<InferenceMessage> messages = List.of(
                InferenceMessage.of(Role.USER, "what is the weather?"),
                InferenceMessage.ofToolCalls(null,
                        List.of(InferenceToolCall.of(callId, "get_weather", "{\"city\":\"SJO\"}"))),
                InferenceMessage.ofToolResult(callId, "get_weather", "{\"tempC\":21}"));

        final InferenceRequest request =
                InferenceRequest.builder(VALID_MODEL).messages(messages).build();

        assertNotNull("A correlated tool result must produce a request", request);
        assertEquals(3, request.messages().size());
        assertEquals(callId, request.messages().get(2).toolCallId());
    }

    // ---------------------------------------------------------------------
    // Internal representation — InferenceMessage
    // ---------------------------------------------------------------------

    /**
     * Given a TOOL turn with no {@code toolCallId},
     * When the record is constructed,
     * Then it is rejected naming {@code toolCallId} — a result that answers nothing in particular
     * cannot be correlated at all.
     */
    @Test
    public void test_inferenceMessage_toolRoleWithoutToolCallId_throwsNamingToolCallId() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new InferenceMessage(Role.TOOL, "{\"tempC\":21}", List.of(), null, "get_weather"));

        assertMessageNamesToken(thrown.getMessage(), "toolCallId");
    }

    /**
     * Given tool calls attached to a non-assistant turn,
     * When the record is constructed,
     * Then it is rejected naming ASSISTANT — only the model asks for tools to be executed.
     */
    @Test
    public void test_inferenceMessage_toolCallsOnNonAssistantRole_throwsNamingAssistant() {
        final List<InferenceToolCall> toolCalls =
                List.of(InferenceToolCall.of("call_abc123", "get_weather", "{}"));

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new InferenceMessage(Role.USER, "what is the weather?", toolCalls, null, null));

        assertMessageNamesToken(thrown.getMessage(), "ASSISTANT");
    }

    /**
     * Given a turn with neither content nor tool calls,
     * When the record is constructed,
     * Then it is rejected naming {@code content} — an empty turn carries nothing a provider could
     * act on, and paying for the round trip to find that out is the failure mode being avoided.
     */
    @Test
    public void test_inferenceMessage_noContentAndNoToolCalls_throwsNamingContent() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new InferenceMessage(Role.USER, null, List.of(), null, null));

        assertMessageNamesToken(thrown.getMessage(), "content");
    }

    // ---------------------------------------------------------------------
    // Wire layer — ChatCompletionMapper (specification; fails until T026)
    // ---------------------------------------------------------------------

    /**
     * Given a request view asking for more than one choice,
     * When it is mapped to the internal representation,
     * Then it is rejected with a validation error naming {@code n}, rather than silently served as
     * a single choice. Unknown fields are tolerated; a field that changes what the caller gets or
     * pays for is not (FR-013).
     */
    @Test
    public void test_toInferenceRequest_nGreaterThanOne_throwsNamingN() {
        final ChatCompletionRequestView view = requestView(VALID_MODEL,
                List.of(new MessageView("user", "hello", null, null, null)), 2);

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> ChatCompletionMapper.toInferenceRequest(view));

        assertMessageNamesToken(thrown.getMessage(), "n");
    }

    /**
     * Given a request view with no model,
     * When it is mapped to the internal representation,
     * Then it is rejected with a validation error naming {@code model}, at the wire layer, before
     * any provider is chosen (FR-024).
     */
    @Test
    public void test_toInferenceRequest_absentModel_throwsNamingModel() {
        final ChatCompletionRequestView view = requestView(null,
                List.of(new MessageView("user", "hello", null, null, null)), null);

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> ChatCompletionMapper.toInferenceRequest(view));

        assertMessageNamesToken(thrown.getMessage(), "model");
    }

    /**
     * Given a request view carrying an empty conversation,
     * When it is mapped to the internal representation,
     * Then it is rejected with a validation error naming {@code messages} (FR-002).
     */
    @Test
    public void test_toInferenceRequest_emptyMessages_throwsNamingMessages() {
        final ChatCompletionRequestView view = requestView(VALID_MODEL, List.of(), null);

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> ChatCompletionMapper.toInferenceRequest(view));

        assertMessageNamesToken(thrown.getMessage(), "message");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Builds an otherwise-minimal request view, so each test varies only the field it is about.
     *
     * @param model    the model id, possibly null
     * @param messages the conversation
     * @param n        the number of choices requested, possibly null
     * @return a request view with every other wire field left absent
     */
    private static ChatCompletionRequestView requestView(final String model,
                                                         final List<MessageView> messages,
                                                         final Integer n) {
        return new ChatCompletionRequestView(model, messages, null, null, null, null, null, null,
                null, null, null, n);
    }

    /**
     * Asserts a validation message actually names what was wrong.
     *
     * <p>Matched on a word boundary rather than a bare substring, so a message that happens to
     * contain the letters of a short field name — {@code n} above all — does not pass for one that
     * names the field.</p>
     *
     * @param message the exception message under test
     * @param token   the field name or identity the message must name
     */
    private static void assertMessageNamesToken(final String message, final String token) {
        assertNotNull("Validation error carried no message at all", message);
        final Pattern pattern =
                Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(token) + "(?![A-Za-z0-9_])");
        assertTrue("Validation error must name '" + token + "' but was: " + message,
                pattern.matcher(message).find());
    }
}
