package com.dotcms.content.index;

import com.fasterxml.jackson.core.StreamReadConstraints;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Predicts whether an index document will be rejected by the JSON parser of the engine clients.
 *
 * <p>Both engine clients read every index document again with Jackson before sending it: the
 * Elasticsearch client while building the bulk request body, the OpenSearch client when the
 * document is added to the batch. Jackson refuses any single string longer than its read limit
 * (20,000,000 characters by default) and any structure nested deeper than its depth limit. On the
 * Elasticsearch path that refusal happens in the middle of building the request, so nothing is
 * sent and every document of the request is reported failed (#37269).</p>
 *
 * <p>This check runs on the final document {@code Map}, before the document is serialized or
 * queued, so one bad document can be withheld on its own. It must see the final map because
 * {@code catchall} concatenates every field value into a single string: it is the largest string
 * in the document and can cross the limit while each authored field is well under it.</p>
 *
 * <p>The limits come from {@link StreamReadConstraints#defaults()}, not from Jackson's constants,
 * so a global override applied at startup is honoured — it is what the clients' parsers use.</p>
 */
public final class IndexDocumentConstraints {

    /** What kind of limit a document exceeds. */
    public enum Kind {
        /** A single string value is longer than the parser accepts. */
        STRING_LENGTH,
        /** Maps and lists are nested deeper than the parser accepts. */
        NESTING_DEPTH
    }

    /**
     * The first limit a document exceeds.
     *
     * @param kind      which limit
     * @param fieldPath key path of the offending value, e.g. {@code catchall} or
     *                  {@code relations[1].value}
     * @param actual    the value's real length, or the nesting depth reached
     * @param limit     the limit it exceeded
     */
    public record Violation(Kind kind, String fieldPath, long actual, long limit) {
    }

    /** Fixed constraints for tests; {@code null} means "Jackson's current defaults". */
    private final StreamReadConstraints fixed;

    /**
     * Uses Jackson's effective default read constraints, resolved on every check so a global
     * override applied after this instance was created is still honoured.
     */
    public IndexDocumentConstraints() {
        this.fixed = null;
    }

    /**
     * Uses the given constraints. Lets tests work with small limits instead of
     * 20-million-character strings.
     *
     * @param constraints the read constraints to enforce
     */
    IndexDocumentConstraints(final StreamReadConstraints constraints) {
        this.fixed = constraints;
    }

    /** The constraints in force for this check. */
    private StreamReadConstraints constraints() {
        return fixed != null ? fixed : StreamReadConstraints.defaults();
    }

    /**
     * Returns the maximum string length this check enforces.
     *
     * @return the maximum number of characters a single string value may have
     */
    public int maxStringLength() {
        return constraints().getMaxStringLength();
    }

    /**
     * Returns the maximum nesting depth this check enforces.
     *
     * @return the maximum number of nested maps and lists a document may have
     */
    public int maxNestingDepth() {
        return constraints().getMaxNestingDepth();
    }

    /**
     * Checks a document against the read constraints.
     *
     * @param document the final index document, as produced by the mapping step
     * @return the first violation found, or empty when the parser will accept the document
     */
    public Optional<Violation> check(final Map<String, Object> document) {
        return visit(document, "", 1, constraints());
    }

    /**
     * Walks one value. Containers (maps, collections, arrays) add a nesting level; strings are
     * measured; any other value is ignored.
     */
    private Optional<Violation> visit(final Object value, final String path, final int depth,
            final StreamReadConstraints constraints) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof CharSequence) {
            final int length = ((CharSequence) value).length();
            if (length > constraints.getMaxStringLength()) {
                return Optional.of(new Violation(Kind.STRING_LENGTH, path, length,
                        constraints.getMaxStringLength()));
            }
            return Optional.empty();
        }
        final boolean container = value instanceof Map || value instanceof Collection
                || value.getClass().isArray();
        if (!container) {
            return Optional.empty();
        }
        if (depth > constraints.getMaxNestingDepth()) {
            return Optional.of(new Violation(Kind.NESTING_DEPTH, path, depth,
                    constraints.getMaxNestingDepth()));
        }
        if (value instanceof Map) {
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                final String key = String.valueOf(entry.getKey());
                final Optional<Violation> found = visit(entry.getValue(),
                        path.isEmpty() ? key : path + "." + key, depth + 1, constraints);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
        if (value instanceof Collection) {
            int index = 0;
            for (final Object item : (Collection<?>) value) {
                final Optional<Violation> found = visit(item, path + "[" + index + "]",
                        depth + 1, constraints);
                if (found.isPresent()) {
                    return found;
                }
                index++;
            }
            return Optional.empty();
        }
        final int length = Array.getLength(value);
        for (int index = 0; index < length; index++) {
            final Optional<Violation> found = visit(Array.get(value, index),
                    path + "[" + index + "]", depth + 1, constraints);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }
}
