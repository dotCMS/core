package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * The outcome of reindexing one contentlet identifier during a bulk refresh run.
 * <p>
 * Results are reported per <b>identifier</b>, not per submitted inode: several language rows of the
 * same content collapse into a single record whose {@link #inodes()} lists every inode the caller
 * submitted for it — which is what a client would need to mark the right grid rows, if one ever consumed
 * these records. None does today; see {@code BulkRefreshContentletsProcessor}'s note on why they are
 * still produced.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = BulkRefreshItemResult.class)
@JsonDeserialize(as = BulkRefreshItemResult.class)
public interface AbstractBulkRefreshItemResult {

    /**
     * The resolved contentlet identifier, or empty when the submitted inode could not be resolved
     * (a row that went stale between selection and submit).
     */
    Optional<String> identifier();

    /**
     * The submitted inodes that resolved to this identifier. Never empty — a record exists only
     * because the caller asked about at least one inode.
     */
    List<String> inodes();

    /** Whether this identifier was reindexed, failed, or was never attempted. */
    BulkRefreshItemStatus status();

    /**
     * Present on {@link BulkRefreshItemStatus#FAILED} only. Root cause unwrapped, so the message
     * names the actual problem instead of a wrapper exception.
     */
    Optional<String> errorMessage();

    /**
     * How many versions were written to the index for this identifier. Lets a UI report "12
     * selected, 31 versions reindexed" honestly rather than implying one write per selected row.
     */
    @Value.Default
    default int versionsIndexed() {
        return 0;
    }

    /**
     * This record read as the shared per-item outcome (spec FR-018, research R3).
     * <p>
     * <b>Extraction, not modification.</b> Everything above stays exactly as it shipped —
     * {@link #identifier()}, {@link #inodes()} and {@link #versionsIndexed()} are untouched, so the
     * existing consumer and its tests are unaffected. This adds a way to <i>read</i> the
     * record through the generic shape that {@code #37062} and {@code #37063} also consume, so the
     * product ends with one batch-outcome contract rather than three.
     * <p>
     * The identifier becomes the generic {@code key()}: for a reindex that is what the item is, the
     * same way a file name is for an upload and a path is for a folder operation.
     */
    default BatchItemResult asBatchItemResult() {
        final BatchItemResult.Builder builder = BatchItemResult.builder()
                .key(identifier().orElse(""))
                .status(BatchItemStatus.valueOf(status().name()));

        // The shipped record carries a message and no code, which is exactly the gap the shared
        // type closes. Reindex failures were never classified, so they map to UNCLASSIFIED rather
        // than inventing a reason this consumer never produced.
        errorMessage().ifPresent(message -> builder
                .reason(BatchFailureReason.UNCLASSIFIED)
                .message(message));

        return builder.build();
    }
}
