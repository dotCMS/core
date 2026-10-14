package com.dotcms.rest.api.v1.index;

import com.dotcms.content.index.IndexDocumentConstraints.Kind;
import com.dotcms.content.index.IndexDocumentConstraints.Violation;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;

/**
 * One reindex journal entry that ran out of retries, as returned by
 * {@code GET /api/v1/index/failed}.
 *
 * <p>It identifies the content and says why indexing failed, and deliberately carries no field
 * values: the entries that end up here are often the oversized documents #37269 withholds, and
 * embedding their content made the old {@code /api/v1/esindex/failed} response large enough to
 * crash the browser tab of the Maintenance portlet's download button. Every key is always
 * present; a value that is not known is an explicit {@code null}.</p>
 *
 * @param identifier          the contentlet identifier of the journal entry
 * @param inode               the inode of a version of the content, resolved only to show it; it
 *                            is not necessarily the version that failed (the journal is keyed by
 *                            identifier). The withheld version's own inode is in
 *                            {@code violation}. {@code null} when the content no longer exists
 * @param title               the stored title of that version; {@code null} when the content no
 *                            longer exists or no title is stored
 * @param contentTypeVariable the content type variable; {@code null} when it cannot be resolved
 * @param languageId          the language of the version resolved for display, not necessarily
 *                            the one that failed; {@code null} when unknown
 * @param pendingOperation    what the index still owes for the identifier: {@code reindex}, or
 *                            {@code delete} for a removal that could not be applied
 * @param failedAttempts      how many times indexing the entry failed
 * @param priority            the raw journal priority, which encodes the attempts
 * @param lastFailureReason   the last failure recorded for the entry; {@code null} when none was
 *                            recorded
 * @param violation           the document limit that was exceeded; {@code null} when the failure
 *                            was not a document-limit violation
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "A reindex journal entry that exhausted its retries, without content values")
public record FailedReindexRecordView(
        @Schema(description = "Contentlet identifier", example = "d5edd758f166259247356e7ea2deca4e")
        String identifier,
        @Schema(description = "Inode of a version of the content, resolved only to show it; not"
                + " necessarily the version that failed. The withheld version's inode is in"
                + " violation.inode. Null if the content no longer exists", nullable = true)
        @Nullable String inode,
        @Schema(description = "Stored title of the version resolved for display; null if the content"
                + " no longer exists or no title is stored", nullable = true)
        @Nullable String title,
        @Schema(description = "Content type variable; null if it cannot be resolved",
                example = "Blog", nullable = true)
        @Nullable String contentTypeVariable,
        @Schema(description = "Language of the version resolved for display; not necessarily the"
                + " version that failed. The withheld version's language is in"
                + " violation.languageId. Null if unknown", example = "1", nullable = true)
        @Nullable Long languageId,
        @Schema(description = "What the index still owes for the identifier",
                allowableValues = {"reindex", "delete"})
        String pendingOperation,
        @Schema(description = "How many times indexing the entry failed", example = "5")
        int failedAttempts,
        @Schema(description = "Raw journal priority; encodes the failed attempts", example = "505")
        int priority,
        @Schema(description = "Last failure recorded for the entry, as stored by the reindex journal"
                + " (truncated to 300 characters); null if no message was recorded", nullable = true)
        @Nullable String lastFailureReason,
        @Schema(description = "The document limit that was exceeded; null if the failure was not a"
                + " document-limit violation", nullable = true)
        @Nullable ViolationView violation) {

    /**
     * A document limit that one version of the entry exceeded, and which version that was, so its
     * index document was withheld (#37269).
     *
     * @param limit     which limit: {@code maxStringLength} or {@code maxNestingDepth}
     * @param fieldPath the path of the offending field in the index document
     * @param actual    the document's value, in {@code unit}
     * @param allowed   the limit, in {@code unit}
     * @param unit      {@code chars} for a string length, {@code levels} for a nesting depth
     * @param inode      the inode of the withheld version, as recorded when it was withheld
     * @param languageId the language of the withheld version, as recorded when it was withheld
     */
    @Schema(description = "A document limit exceeded by the index document, and the version withheld for it")
    public record ViolationView(
            @Schema(description = "Which limit was exceeded",
                    allowableValues = {"maxStringLength", "maxNestingDepth"})
            String limit,
            @Schema(description = "Path of the offending field in the index document", example = "catchall")
            String fieldPath,
            @Schema(description = "The document's value, in unit", example = "42000792")
            long actual,
            @Schema(description = "The limit, in unit", example = "20000000")
            long allowed,
            @Schema(description = "Unit of actual and allowed", allowableValues = {"chars", "levels"})
            String unit,
            @Schema(description = "Inode of the withheld version", example = "9f8e7d6c-1111-2222-3333-444455556666")
            String inode,
            @Schema(description = "Language of the withheld version", example = "1")
            long languageId) {

        /**
         * Builds the view of a withheld document read from the journal.
         *
         * @param withheld the withheld document and the limit it exceeded
         * @return its view
         */
        static ViolationView of(final IndexDocumentViolation withheld) {
            final Violation violation = withheld.violation();
            final boolean length = violation.kind() == Kind.STRING_LENGTH;
            return new ViolationView(length ? "maxStringLength" : "maxNestingDepth",
                    violation.fieldPath(), violation.actual(), violation.limit(),
                    length ? "chars" : "levels", withheld.inode(), withheld.languageId());
        }
    }

    /**
     * Builds the view of a failed journal entry.
     *
     * @param row     the failed journal entry
     * @param display the display data of a version of the entry's content, or {@code null} when
     *                none exists (e.g. a removal whose content was destroyed, or archived content)
     * @return the view, which never carries the content's field values
     */
    public static FailedReindexRecordView from(final ReindexEntry row,
            @Nullable final DisplayVersion display) {
        // markAsFailed stores a missing message as "": report it the same as no reason at all.
        final String reason = UtilMethods.isSet(row.getLastResult()) ? row.getLastResult() : null;
        return new FailedReindexRecordView(
                row.getIdentToIndex(),
                display != null ? display.inode() : null,
                display != null ? display.title() : null,
                display != null ? display.contentTypeVariable() : null,
                display != null ? display.languageId() : null,
                row.isDelete() ? "delete" : "reindex",
                row.errorCount(),
                row.getPriority(),
                reason,
                IndexDocumentViolation.violationOf(reason).map(ViolationView::of).orElse(null));
    }
}
