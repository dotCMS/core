package com.dotcms.content.index;

import com.dotcms.content.index.IndexDocumentConstraints.Kind;
import com.dotcms.content.index.IndexDocumentConstraints.Violation;
import java.util.Locale;

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

    /**
     * Renders the reason stored in the reindex journal, e.g.
     * {@code document 1a2b_1 (inode 9f8e) field 'catchall' is 21,034,112 chars; limit 20,000,000}.
     *
     * @return a one-line, human-readable failure reason naming the content, field and size
     */
    public String toFailureReason() {
        final String measure = violation.kind() == Kind.STRING_LENGTH ? "chars" : "levels deep";
        return String.format(Locale.US,
                "document %s_%d (inode %s) field '%s' is %,d %s; limit %,d — not sent to the index",
                identifier, languageId, inode, violation.fieldPath(), violation.actual(), measure,
                violation.limit());
    }
}
