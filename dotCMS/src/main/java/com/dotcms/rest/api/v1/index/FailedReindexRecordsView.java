package com.dotcms.rest.api.v1.index;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The listing returned by {@code GET /api/v1/index/failed}: the limits a failure is measured
 * against, and the failed records.
 *
 * @param limits            the index-document limits in force
 * @param retryPolicy       how many failures park an entry
 * @param failedRecordCount the number of failed records
 * @param failedRecords     the failed records
 */
@Schema(description = "Failed reindex records with the limits they are measured against")
// Reading order, overriding the REST mapper's alphabetical sort: what the records are measured
// against first, then the records.
@JsonPropertyOrder({"limits", "retryPolicy", "failedRecordCount", "failedRecords"})
public record FailedReindexRecordsView(
        DocumentLimits limits,
        RetryPolicy retryPolicy,
        @Schema(description = "Number of failed records", example = "4")
        int failedRecordCount,
        List<FailedReindexRecordView> failedRecords) {

    /**
     * Index-document limits enforced before a document is sent to the engine (#37269); they are
     * the engine clients' JSON parser limits. A failed record's {@code violation.type} names the
     * one it exceeded, and its {@code actual} is in that limit's unit.
     *
     * @param maxStringLength the maximum length of a single string value
     * @param maxNestingDepth the maximum nesting of maps and lists
     */
    @Schema(description = "Index-document limits enforced before a document is sent to the engine")
    public record DocumentLimits(
            @Schema(description = "Maximum length of a single string value, in characters")
            Limit maxStringLength,
            @Schema(description = "Maximum nesting of maps and lists, in levels")
            Limit maxNestingDepth) {

        /**
         * Builds the limits in their units.
         *
         * @param maxStringLength the maximum characters of a single string value
         * @param maxNestingDepth the maximum nesting levels
         * @return the limits
         */
        static DocumentLimits of(final int maxStringLength, final int maxNestingDepth) {
            return new DocumentLimits(new Limit(maxStringLength, "chars"),
                    new Limit(maxNestingDepth, "levels"));
        }
    }

    /**
     * One limit and the unit it is measured in.
     *
     * @param value the limit
     * @param unit  {@code chars} or {@code levels}
     */
    @Schema(description = "A limit and the unit it is measured in")
    public record Limit(
            @Schema(description = "The limit", example = "20000000")
            int value,
            @Schema(description = "Unit of the limit", allowableValues = {"chars", "levels"})
            String unit) {
    }

    /**
     * How the reindex journal retries an entry.
     *
     * @param maxRetries how many times a failed entry is retried after its first attempt
     *                   ({@code RETRY_FAILED_INDEX_TIMES}); an entry listed here as exhausted
     *                   failed {@code maxRetries + 1} times
     */
    @Schema(description = "How the reindex journal retries an entry")
    public record RetryPolicy(
            @Schema(description = "Retries after the first attempt before an entry stops being"
                    + " retried (RETRY_FAILED_INDEX_TIMES); an exhausted entry failed"
                    + " maxRetries + 1 times", example = "5")
            int maxRetries) {
    }

    /**
     * Builds the listing.
     *
     * @param failedRecords     the failed records
     * @param maxStringLength   the string-length limit in force
     * @param maxNestingDepth   the nesting-depth limit in force
     * @param maxRetries        the configured number of retries
     * @return the listing
     */
    public static FailedReindexRecordsView of(final List<FailedReindexRecordView> failedRecords,
            final int maxStringLength, final int maxNestingDepth, final int maxRetries) {
        return new FailedReindexRecordsView(DocumentLimits.of(maxStringLength, maxNestingDepth),
                new RetryPolicy(maxRetries), failedRecords.size(), List.copyOf(failedRecords));
    }
}
