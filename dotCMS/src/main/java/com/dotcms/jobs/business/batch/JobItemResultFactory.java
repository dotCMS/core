package com.dotcms.jobs.business.batch;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads and writes {@code job_item_result}, the durable per-item record a batch job keeps while it
 * runs (#37166, spec FR-036 … FR-038; data-model §2).
 * <p>
 * <b>What this exists for.</b> The job framework keeps no mid-run per-item state — parameters are
 * immutable after creation, progress is one float, and the result is harvested once at the terminal
 * state — while the abandoned-job sweep re-queues a stalled run without consulting the retry
 * policy. Without these rows a re-queued run restarts from the first item and its report lies: the
 * items the first attempt completed come back as collisions.
 * <p>
 * <b>Rows are written as each item completes, inside that item's own transaction</b>, so the record
 * commits with the work it describes. They serve twice — the resume path reads them on start, and
 * the terminal state reads them to build the outcome — which is what keeps per-item reporting from
 * being accumulated in memory and lost on interruption, as bulk refresh's is.
 *
 * @author dotCMS
 */
public class JobItemResultFactory {

    /**
     * Upsert on {@code (job_id, seq)}. A resumed run may re-process an item whose row already
     * committed; the write must correct the record rather than fail the run on a constraint
     * violation, so the later write wins.
     */
    private static final String UPSERT =
            "INSERT INTO job_item_result "
                    + "(job_id, seq, item_key, status, reason, message, ref_id, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (job_id, seq) DO UPDATE SET "
                    + "item_key = EXCLUDED.item_key, status = EXCLUDED.status, "
                    + "reason = EXCLUDED.reason, message = EXCLUDED.message, "
                    + "ref_id = EXCLUDED.ref_id, updated_at = EXCLUDED.updated_at";

    private static final String FIND_BY_JOB =
            "SELECT item_key, status, reason, message FROM job_item_result "
                    + "WHERE job_id = ? ORDER BY seq";

    private static final String FIND_COMPLETED_SEQS =
            "SELECT seq FROM job_item_result WHERE job_id = ? AND status = ? ORDER BY seq";

    /**
     * Records the outcome of one item.
     *
     * @param jobId  the run this item belongs to
     * @param seq    the item's submission index — the key, because two items in one batch may share
     *               a name and a resumed run has to tell them apart
     * @param itemKey what the item is: a file name here, a folder path for a folder operation
     * @param status  succeeded, failed, or never attempted
     * @param reason  machine-readable cause; null unless {@code status} is
     *                {@link BatchItemStatus#FAILED}
     * @param message diagnostic detail for logs; never displayed to the author
     * @param refId   what the item produced — the created contentlet's identifier.
     *                <p>
     *                <b>Written and not yet read.</b> This previously claimed it made a resumed
     *                run's "already created" answer <i>verifiable rather than inferred</i>; it does
     *                not, and no code path realizes that. {@code findByJobId} does not select the
     *                column and {@code AbstractBatchItemResult} has no accessor for it, so resume
     *                is answered from {@code seq} alone. The claim is corrected here rather than
     *                the column dropped, deliberately: whether this table survives at all is under
     *                review, so changing its DDL now is churn either way. Decide the column's fate
     *                with the table's.
     */
    public void record(final String jobId, final int seq, final String itemKey,
                       final BatchItemStatus status, final BatchFailureReason reason,
                       final String message, final String refId) throws DotDataException {

        new DotConnect().setSQL(UPSERT)
                .addParam(jobId)
                .addParam(seq)
                .addParam(itemKey)
                .addParam(status.name())
                .addParam(reason == null ? null : reason.name())
                .addParam(message)
                .addParam(refId)
                .addParam(Timestamp.from(Instant.now()))
                .loadResult();
    }

    /**
     * Every recorded item for a run, in submission order — which is the order the author chose the
     * files, and therefore the order the outcome lists them (spec FR-015).
     */
    public List<BatchItemResult> findByJobId(final String jobId) throws DotDataException {

        final List<Map<String, Object>> rows = new DotConnect().setSQL(FIND_BY_JOB)
                .addParam(jobId)
                .loadObjectResults();

        final List<BatchItemResult> results = new ArrayList<>(rows.size());
        for (final Map<String, Object> row : rows) {
            final BatchItemResult.Builder builder = BatchItemResult.builder()
                    .key((String) row.get("item_key"))
                    .status(BatchItemStatus.valueOf((String) row.get("status")));

            final String reason = (String) row.get("reason");
            if (reason != null) {
                builder.reason(BatchFailureReason.valueOf(reason));
            }
            final String message = (String) row.get("message");
            if (message != null) {
                builder.message(message);
            }
            results.add(builder.build());
        }
        return results;
    }

    /**
     * The submission indexes a run already completed successfully.
     * <p>
     * <b>Successes only.</b> A resumed run skips these and re-attempts everything else — skipping
     * failures too would strand items the author could have had on a second try, which is the
     * opposite of what resumability is for.
     */
    public List<Integer> findCompletedSeqs(final String jobId) throws DotDataException {

        return new DotConnect().setSQL(FIND_COMPLETED_SEQS)
                .addParam(jobId)
                .addParam(BatchItemStatus.SUCCESS.name())
                .loadObjectResults()
                .stream()
                .map(row -> ((Number) row.get("seq")).intValue())
                .collect(Collectors.toList());
    }
}
