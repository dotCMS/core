package com.dotcms.jobs.business.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link JobItemResultFactory}, the durable per-item checkpoint
 * (spec FR-036, FR-037; data-model §2).
 * <p>
 * <b>Integration rather than unit, deliberately.</b> The store writes through {@link DotConnect};
 * mocking that would assert against the mock and leave the SQL and the {@code (job_id, seq)}
 * primary key — the two things most likely to be wrong — untested. The repository has no unit test
 * touching {@code DotConnect} and 168 integration tests that do.
 * <p>
 * The store is what makes a run resumable. Without it a re-queued run restarts from the first file
 * and the outcome lies: the files the first attempt created come back as collisions, so the author
 * is told 30 files failed when all 30 are in the folder.
 */
public class JobItemResultFactoryIT extends Junit5WeldBaseTest {

    private static JobItemResultFactory factory;
    private String jobId;

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        factory = new JobItemResultFactory();
    }

    @AfterEach
    public void cleanUp() throws Exception {
        if (jobId != null) {
            new DotConnect().setSQL("DELETE FROM job_item_result WHERE job_id = ?")
                    .addParam(jobId).loadResult();
        }
    }

    /**
     * Method to test: {@link JobItemResultFactory#record} and {@code findByJobId}
     * <p>
     * Given scenario: Three items of a run are recorded one at a time, as the run would write them.
     * <p>
     * Expected result: All three read back, ordered by {@code seq} rather than by insertion or by
     * name, so the outcome lists results in the order the author chose the files (FR-015).
     */
    @Test
    public void test_record_writesEachItemAndReadsBackInSeqOrder() throws Exception {
        jobId = UUID.randomUUID().toString();

        factory.record(jobId, 2, "b.png", BatchItemStatus.SUCCESS, null, null, "inode-b");
        factory.record(jobId, 0, "a.pdf", BatchItemStatus.SUCCESS, null, null, "inode-a");
        factory.record(jobId, 1, "c.txt", BatchItemStatus.FAILED,
                BatchFailureReason.OVER_SIZE_LIMIT, "too big", null);

        final List<BatchItemResult> results = factory.findByJobId(jobId);

        assertEquals(3, results.size());
        assertEquals("a.pdf", results.get(0).key());
        assertEquals("c.txt", results.get(1).key());
        assertEquals("b.png", results.get(2).key());
    }

    /**
     * Method to test: the {@code (job_id, seq)} primary key
     * <p>
     * Given scenario: One batch contains <b>two files with the same name</b> — which spec.md
     * §Edge Cases explicitly allows, each subject to the same collision rule.
     * <p>
     * Expected result: Both rows persist and are distinguishable. This is why the key is
     * {@code (job_id, seq)} and not {@code (job_id, item_key)}: keying on the name would reject the
     * second write outright, and a resumed run could not tell which of the two had already
     * completed.
     */
    @Test
    public void test_twoItemsSharingAName_areKeyedApartBySeq() throws Exception {
        jobId = UUID.randomUUID().toString();

        factory.record(jobId, 0, "report.pdf", BatchItemStatus.SUCCESS, null, null, "inode-1");
        factory.record(jobId, 1, "report.pdf", BatchItemStatus.FAILED,
                BatchFailureReason.NAME_COLLISION, "already exists", null);

        final List<BatchItemResult> results = factory.findByJobId(jobId);

        assertEquals(2, results.size(),
                "both rows must survive; the batch is allowed two files of one name");
        assertEquals(BatchItemStatus.SUCCESS, results.get(0).status());
        assertEquals(BatchItemStatus.FAILED, results.get(1).status());
    }

    /**
     * Method to test: {@link JobItemResultFactory#record} idempotence on resume
     * <p>
     * Given scenario: The same {@code seq} is recorded twice, as happens when a run is re-queued
     * and re-processes an item whose row was already committed.
     * <p>
     * Expected result: One row, not two, and not a constraint violation that kills the run. The
     * resume path must be able to write without first checking.
     */
    @Test
    public void test_recordingTheSameSeqTwice_isIdempotent() throws Exception {
        jobId = UUID.randomUUID().toString();

        factory.record(jobId, 0, "a.pdf", BatchItemStatus.FAILED,
                BatchFailureReason.UNCLASSIFIED, "first attempt", null);
        factory.record(jobId, 0, "a.pdf", BatchItemStatus.SUCCESS, null, null, "inode-a");

        final List<BatchItemResult> results = factory.findByJobId(jobId);

        assertEquals(1, results.size());
        assertEquals(BatchItemStatus.SUCCESS, results.get(0).status(),
                "the later write wins, so a resumed run corrects its own record");
    }

    /**
     * Method to test: {@link JobItemResultFactory#findCompletedSeqs}
     * <p>
     * Given scenario: A run recorded one success and one failure, then was interrupted.
     * <p>
     * Expected result: Only the successful {@code seq} comes back. A resumed run skips what
     * succeeded and re-attempts what failed (FR-036) — skipping failures too would strand files the
     * author could otherwise have had on a second try.
     */
    @Test
    public void test_findCompletedSeqs_returnsSuccessesOnly() throws Exception {
        jobId = UUID.randomUUID().toString();

        factory.record(jobId, 0, "ok.pdf", BatchItemStatus.SUCCESS, null, null, "inode-ok");
        factory.record(jobId, 1, "bad.exe", BatchItemStatus.FAILED,
                BatchFailureReason.DISALLOWED_FILE_TYPE, "not allowed", null);

        final List<Integer> completed = factory.findCompletedSeqs(jobId);

        assertEquals(1, completed.size());
        assertTrue(completed.contains(0));
    }
}
