package com.dotcms.rest.api.v1.index;

import com.dotcms.content.index.IndexDocumentConstraints.Kind;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
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
 * <p>The journal is keyed by identifier, so which version failed is known only when the failure
 * is a document-limit violation, whose reason names it. Then {@code inode}, {@code languageId},
 * {@code title} and {@code contentTypeVariable} describe that version; otherwise they describe a
 * version of the content resolved only to show it.</p>
 *
 * @param identifier          the contentlet identifier of the journal entry
 * @param inode               the withheld version's inode for a document-limit violation,
 *                            otherwise a version resolved for display; {@code null} when unknown
 * @param title               the stored title of that version; {@code null} when unknown or no
 *                            title is stored
 * @param contentTypeVariable the content type variable; {@code null} when it cannot be resolved
 * @param languageId          the language of that version; {@code null} when unknown
 * @param archived            {@code true} when every version of the content is archived;
 *                            {@code null} when the content could not be resolved
 * @param operation           what the index still owes for the identifier: {@code reindex}, or
 *                            {@code delete} for a removal that could not be applied
 * @param failedAttempts      how many times indexing the entry failed
 * @param priority            the raw journal priority, which encodes the attempts
 * @param failureReason       the last failure recorded for the entry; {@code null} when none was
 *                            recorded
 * @param violation           the document limit that was exceeded; {@code null} when the failure
 *                            was not a document-limit violation
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "A reindex journal entry that exhausted its retries, without content values")
public record FailedReindexRecordView(
        @Schema(description = "Contentlet identifier", example = "d5edd758f166259247356e7ea2deca4e")
        String identifier,
        @Schema(description = "Inode of the withheld version when the failure is a document-limit"
                + " violation; otherwise of a version of the content resolved only to show it."
                + " Null if unknown", nullable = true)
        @Nullable String inode,
        @Schema(description = "Stored title of that version; null if unknown or no title is stored",
                nullable = true)
        @Nullable String title,
        @Schema(description = "Content type variable; null if it cannot be resolved",
                example = "Blog", nullable = true)
        @Nullable String contentTypeVariable,
        @Schema(description = "Language of that version; null if unknown", example = "1",
                nullable = true)
        @Nullable Long languageId,
        @Schema(description = "True when every version of the content is archived; null if the"
                + " content could not be resolved", nullable = true)
        @Nullable Boolean archived,
        @Schema(description = "What the index still owes for the identifier",
                allowableValues = {"reindex", "delete"})
        String operation,
        @Schema(description = "How many times indexing the entry failed", example = "6")
        int failedAttempts,
        @Schema(description = "Raw journal priority; encodes the failed attempts", example = "505")
        int priority,
        @Schema(description = "Last failure recorded for the entry, as stored by the reindex journal"
                + " (truncated to 300 characters); null if no message was recorded", nullable = true)
        @Nullable String failureReason,
        @Schema(description = "The document limit that was exceeded; null if the failure was not a"
                + " document-limit violation", nullable = true)
        @Nullable ViolationView violation) {

    /**
     * A document limit that the withheld version's index document exceeded (#37269). The limit
     * in force, and its unit, is the root's {@code limits} entry named by {@code type}.
     *
     * @param type      which limit: {@code maxStringLength} or {@code maxNestingDepth}
     * @param fieldPath the path of the offending field in the index document
     * @param actual    the document's value, in the unit of that limit
     */
    @Schema(description = "A document limit exceeded by the index document")
    public record ViolationView(
            @Schema(description = "Which limit was exceeded; names the root limits entry"
                    + " that holds its value and unit",
                    allowableValues = {"maxStringLength", "maxNestingDepth"})
            String type,
            @Schema(description = "Path of the offending field in the index document", example = "catchall")
            String fieldPath,
            @Schema(description = "The document's value, in the unit of limits[type]",
                    example = "42000792")
            long actual) {

        /**
         * Builds the view of a withheld document read from the journal.
         *
         * @param withheld the withheld document and the limit it exceeded
         * @return its view
         */
        static ViolationView of(final IndexDocumentViolation withheld) {
            final boolean length = withheld.violation().kind() == Kind.STRING_LENGTH;
            return new ViolationView(length ? "maxStringLength" : "maxNestingDepth",
                    withheld.violation().fieldPath(), withheld.violation().actual());
        }
    }

    /**
     * Builds the view of a failed journal entry.
     *
     * @param row     the failed journal entry
     * @param display the display data of the withheld version (for a document-limit violation)
     *                or of a version of the entry's content; {@code null} when none was found
     * @return the view, which never carries the content's field values
     */
    public static FailedReindexRecordView from(final ReindexEntry row,
            @Nullable final DisplayVersion display) {
        // markAsFailed stores a missing message as "": report it the same as no reason at all.
        final String reason = UtilMethods.isSet(row.getLastResult()) ? row.getLastResult() : null;
        final Optional<IndexDocumentViolation> withheld = IndexDocumentViolation.violationOf(reason);
        return new FailedReindexRecordView(
                row.getIdentToIndex(),
                withheld.map(IndexDocumentViolation::inode)
                        .orElse(display != null ? display.inode() : null),
                display != null ? display.title() : null,
                display != null ? display.contentTypeVariable() : null,
                withheld.map(IndexDocumentViolation::languageId)
                        .orElse(display != null ? display.languageId() : null),
                display != null ? display.archived() : null,
                row.isDelete() ? "delete" : "reindex",
                failedAttempts(row),
                row.getPriority(),
                reason,
                withheld.map(ViolationView::of).orElse(null));
    }

    /**
     * Counts every failure of the entry. The priority counts errors in its last two digits, but
     * the failure that exhausts the retries parks the entry by adding the {@code ERROR} base
     * instead of a count, so a parked entry failed once more than its priority counts.
     */
    private static int failedAttempts(final ReindexEntry row) {
        return row.getPriority() >= Priority.ERROR.dbValue()
                ? row.errorCount() + 1
                : row.errorCount();
    }
}
