package com.dotcms.inference.model;

import java.util.List;

/**
 * One event in a streamed completion.
 *
 * <p>Sealed so the serializer is a total function: every variant must be handled, and adding a
 * sixth cannot silently fall through to a default branch. That matters most for
 * {@link Error @Error} — the difference between a stream that failed and a stream that finished
 * is carried entirely by which variant ends it, and by the serializer refusing to write the
 * terminal done marker after an error.</p>
 */
public sealed interface InferenceStreamEvent
        permits InferenceStreamEvent.ContentDelta,
                InferenceStreamEvent.ToolCallDelta,
                InferenceStreamEvent.Finish,
                InferenceStreamEvent.Usage,
                InferenceStreamEvent.Error {

    /**
     * A fragment of the answer's text.
     *
     * @param text the fragment, as produced
     */
    record ContentDelta(String text) implements InferenceStreamEvent {
        public ContentDelta {
            text = text == null ? "" : text;
        }
    }

    /**
     * A fragment of one tool call.
     *
     * <p>{@code id} and {@code name} arrive on the first fragment of a call and are absent
     * afterwards; {@code partialArguments} accumulates across fragments. {@code index} identifies
     * which call in the turn a fragment belongs to, and is the only thing that does so on
     * continuation fragments.</p>
     *
     * @param index            which call in the turn this fragment belongs to
     * @param id               the call's identity, present on its first fragment only
     * @param name             the tool's name, present on its first fragment only
     * @param partialArguments the argument text carried by this fragment
     */
    record ToolCallDelta(int index, String id, String name, String partialArguments)
            implements InferenceStreamEvent {
        public ToolCallDelta {
            partialArguments = partialArguments == null ? "" : partialArguments;
        }
    }

    /**
     * Generation stopped normally.
     *
     * @param reason why it stopped; never {@link FinishReason#ERROR}
     */
    record Finish(FinishReason reason) implements InferenceStreamEvent {
        public Finish {
            if (reason == null) {
                throw new IllegalArgumentException("Finish reason is required");
            }
            if (reason == FinishReason.ERROR) {
                throw new IllegalArgumentException(
                        "A failed stream ends with an Error event, not a Finish with reason ERROR");
            }
        }
    }

    /**
     * Tokens consumed by the exchange.
     *
     * <p>Emitted only when the caller asked for it. The event it serializes to carries an empty
     * choices array, which some stream readers do not tolerate, so sending it unasked would break
     * clients this family exists to support.</p>
     *
     * @param usage the counts
     */
    record Usage(InferenceUsage usage) implements InferenceStreamEvent {
        public Usage {
            if (usage == null) {
                throw new IllegalArgumentException("Usage event requires counts");
            }
        }
    }

    /**
     * The stream failed after it had begun.
     *
     * <p>Once the first event is written the HTTP status is already sent and cannot carry the
     * failure, so this variant is the only way a caller learns the answer is not complete.</p>
     *
     * @param error what went wrong
     */
    record Error(InferenceError error) implements InferenceStreamEvent {
        public Error {
            if (error == null) {
                throw new IllegalArgumentException("Error event requires an error");
            }
        }
    }

    /**
     * Reassembles the complete arguments of one tool call from its fragments, in arrival order.
     *
     * @param events the events seen so far
     * @param index  the call to reassemble
     * @return the concatenated argument text, empty if no fragment matched
     */
    static String reassembleArguments(final List<InferenceStreamEvent> events, final int index) {
        final StringBuilder arguments = new StringBuilder();
        for (final InferenceStreamEvent event : events) {
            if (event instanceof ToolCallDelta delta && delta.index() == index) {
                arguments.append(delta.partialArguments());
            }
        }
        return arguments.toString();
    }
}
