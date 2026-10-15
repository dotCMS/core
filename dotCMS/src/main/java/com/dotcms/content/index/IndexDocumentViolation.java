package com.dotcms.content.index;

import com.dotcms.content.index.IndexDocumentConstraints.Kind;
import com.dotcms.content.index.IndexDocumentConstraints.Violation;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An index document that was withheld because the engine clients' JSON parser would reject it,
 * together with the contentlet version it belongs to.
 *
 * <p>Its {@link #toFailureReason()} text is what the reindex journal records for the entry, so an
 * operator can find the offending content directly instead of seeing the parser's message
 * repeated on every document that shared its bulk request (#37269).</p>
 *
 * @param identifier the contentlet identifier
 * @param inode      the inode of the offending version
 * @param languageId the language of the offending version
 * @param violation  which limit was exceeded, where, and by how much
 */
public record IndexDocumentViolation(String identifier, String inode, long languageId,
                                     Violation violation) {

    /** Unit word written after a string length in {@link #toFailureReason()}. */
    private static final String CHARS = "chars";

    /** Unit words written after a nesting depth in {@link #toFailureReason()}. */
    private static final String LEVELS_DEEP = "levels deep";

    /**
     * Reads a reason written by {@link #toFailureReason()}: identifier, language, inode, field
     * path, actual value, unit and limit. Kept next to the writer so the two cannot drift apart.
     * The identifier is matched greedily up to the last {@code _<digits>}, which is the language;
     * the field path is matched lazily up to the size that follows it, so a path holding an
     * apostrophe (a user-defined key/value or JSON key) is still read whole.
     */
    private static final Pattern REASON = Pattern.compile(
            "document (\\S+)_(\\d+) \\(inode ([^)]*)\\) field '(.*?)' is ([0-9,]+) ("
                    + CHARS + "|" + LEVELS_DEEP + "); limit ([0-9,]+)");

    /**
     * Reads back the withheld document recorded in a reindex journal reason.
     *
     * <p>The journal stores the reason as text only and keys its rows by identifier, so this is
     * the only way the failed-records listing can tell which version was withheld — its inode and
     * language — and which limit it exceeded, where, and by how much. Only the first withheld
     * document is read when several documents of the entry were withheld; text that follows it
     * (such as a later queueing error) is ignored.</p>
     *
     * @param failureReason a journal reason, possibly {@code null} or unrelated to document limits
     * @return the withheld document, or empty when the reason was not written by
     *         {@link #toFailureReason()}
     */
    public static Optional<IndexDocumentViolation> violationOf(final String failureReason) {
        if (failureReason == null || failureReason.isEmpty()) {
            return Optional.empty();
        }
        final Matcher matcher = REASON.matcher(failureReason);
        if (!matcher.find()) {
            return Optional.empty();
        }
        final Kind kind = CHARS.equals(matcher.group(6)) ? Kind.STRING_LENGTH : Kind.NESTING_DEPTH;
        return Optional.of(new IndexDocumentViolation(matcher.group(1),
                matcher.group(3), Long.parseLong(matcher.group(2)),
                new Violation(kind, matcher.group(4),
                        Long.parseLong(matcher.group(5).replace(",", "")),
                        Long.parseLong(matcher.group(7).replace(",", "")))));
    }

    /**
     * Renders the reason stored in the reindex journal, e.g.
     * {@code document 1a2b_1 (inode 9f8e) field 'catchall' is 21,034,112 chars; limit 20,000,000}.
     *
     * @return a one-line, human-readable failure reason naming the content, field and size
     */
    public String toFailureReason() {
        final String measure = violation.kind() == Kind.STRING_LENGTH ? CHARS : LEVELS_DEEP;
        return String.format(Locale.US,
                "document %s_%d (inode %s) field '%s' is %,d %s; limit %,d — not sent to the index",
                identifier, languageId, inode, violation.fieldPath(), violation.actual(), measure,
                violation.limit());
    }
}
