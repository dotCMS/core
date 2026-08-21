package com.dotcms.rest.api.v1.content.bulkrefresh;

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
}
