package com.dotcms.jobs.business.batch;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * The outcome of one item in a batch job run, whatever the item is.
 * <p>
 * <b>Generalizes the shape bulk refresh already ships</b> rather than defining a second one. Two deltas make it general:
 * <ul>
 *   <li>A <b>generic {@link #key()}</b>. The shipped record is keyed by contentlet identifier and
 *       inodes; an uploading file has neither — it does not exist until the run creates it — and
 *       neither does a folder path.</li>
 *   <li>A <b>machine-readable {@link #reason()}</b>. The shipped record carries a human-readable
 *       message only, which a client cannot map to copy.</li>
 * </ul>
 * Lives in a package neither this feature nor bulk refresh owns, so neither holds the other's
 * contract. {@code #37062} (folder copy, bulk delete) and {@code #37063} consume it unchanged.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = BatchItemResult.class)
@JsonDeserialize(as = BatchItemResult.class)
public interface AbstractBatchItemResult {

    /**
     * What the item is, in terms the caller chose: a file name for a bulk upload, a folder path
     * for a folder operation, a contentlet identifier for a reindex. Deliberately a plain string —
     * the point of the generalization.
     */
    String key();

    /** Whether the item succeeded, failed, or was never attempted. */
    BatchItemStatus status();

    /**
     * Present on {@link BatchItemStatus#FAILED} only. <b>This is what the client presents</b>,
     * mapped to product copy.
     */
    Optional<BatchFailureReason> reason();

    /**
     * Diagnostic detail for logs. <b>Never displayed to the author</b> it carries
     * validation text, which is neither localized for the author nor stable.
     */
    Optional<String> message();
}
