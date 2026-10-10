package com.dotcms.rest.api.v1.index;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The listing returned by {@code GET /api/v1/index/failed}: the limits a failure is measured
 * against, and the failed records.
 *
 * @param documentLimits    the index-document limits in force
 * @param retryPolicy       how many failures park an entry
 * @param failedRecordCount the number of failed records
 * @param failedRecords     the failed records
 */
@Schema(description = "Failed reindex records with the limits they are measured against")
public record FailedReindexRecordsView(
        DocumentLimits documentLimits,
        RetryPolicy retryPolicy,
        @Schema(description = "Number of failed records", example = "4")
        int failedRecordCount,
        List<FailedReindexRecordView> failedRecords) {

    /**
     * Index-document limits enforced before a document is sent to the engine (#37269); they are
     * the engine clients' JSON parser limits.
     *
     * @param maxStringLength the maximum characters of a single string value
     * @param maxNestingDepth the maximum nesting of maps and lists
     */
    @Schema(description = "Index-document limits enforced before a document is sent to the engine")
    public record DocumentLimits(
            @Schema(description = "Maximum characters of a single string value", example = "20000000")
            int maxStringLength,
            @Schema(description = "Maximum nesting of maps and lists", example = "1000")
            int maxNestingDepth) {
    }

    /**
     * How the reindex journal retries an entry.
     *
     * @param maxFailedAttempts the failures after which an entry stops being retried and is listed
     *                          here
     */
    @Schema(description = "How the reindex journal retries an entry")
    public record RetryPolicy(
            @Schema(description = "Failures after which an entry stops being retried",
                    example = "5")
            int maxFailedAttempts) {
    }

    /**
     * Builds the listing.
     *
     * @param failedRecords     the failed records
     * @param maxStringLength   the string-length limit in force
     * @param maxNestingDepth   the nesting-depth limit in force
     * @param maxFailedAttempts the configured retry limit
     * @return the listing
     */
    public static FailedReindexRecordsView of(final List<FailedReindexRecordView> failedRecords,
            final int maxStringLength, final int maxNestingDepth, final int maxFailedAttempts) {
        return new FailedReindexRecordsView(new DocumentLimits(maxStringLength, maxNestingDepth),
                new RetryPolicy(maxFailedAttempts), failedRecords.size(), List.copyOf(failedRecords));
    }
}
