package com.dotcms.jobs.business.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshItemResult;
import org.junit.Test;

/**
 * Unit tests for {@link BatchItemResult}, the shared per-item outcome (spec FR-018, FR-018a).
 * <p>
 * These assert the <b>generalization</b>, which is the whole reason the type was extracted: the
 * same record has to describe an uploaded file, a copied folder path, and a reindexed contentlet
 * without any of the three being a special case. SC-006 turns on it.
 */
public class BatchItemResultTest {

    /**
     * Method to test: {@link AbstractBatchItemResult#key()}
     * <p>
     * Given scenario: The same type is asked to describe a bulk-upload item, keyed by file name,
     * and a folder-operation item, keyed by path.
     * <p>
     * Expected result: Both are ordinary values of the same field. This is FR-018a stated as a
     * test: the shipped bulk refresh record is keyed by contentlet identifier and inodes, which an
     * uploading file does not have — it does not exist until the run creates it — so a generic key
     * is the substance of the generalization, not a cosmetic rename.
     */
    @Test
    public void test_key_carriesAFileNameAndAFolderPathEqually() {
        final BatchItemResult file = BatchItemResult.builder()
                .key("brochure.pdf")
                .status(BatchItemStatus.SUCCESS)
                .build();

        final BatchItemResult folder = BatchItemResult.builder()
                .key("/application/themes/quest/")
                .status(BatchItemStatus.SUCCESS)
                .build();

        assertEquals("brochure.pdf", file.key());
        assertEquals("/application/themes/quest/", folder.key());
    }

    /**
     * Method to test: {@link AbstractBatchItemResult#reason()} and {@code message()}
     * <p>
     * Given scenario: A failed item carries a machine-readable reason and a diagnostic message.
     * <p>
     * Expected result: Both are present and distinct. The reason is what the client maps to product
     * copy; the message is for logs and is never displayed (FR-016, FR-016a). The shipped record
     * carries only the message, which is precisely what a client cannot present.
     */
    @Test
    public void test_failedItem_carriesReasonAndMessageSeparately() {
        final BatchItemResult failed = BatchItemResult.builder()
                .key("huge.mov")
                .status(BatchItemStatus.FAILED)
                .reason(BatchFailureReason.OVER_SIZE_LIMIT)
                .message("File exceeds the maximum size of 50 MB")
                .build();

        assertTrue(failed.reason().isPresent());
        assertEquals(BatchFailureReason.OVER_SIZE_LIMIT, failed.reason().get());
        assertTrue(failed.message().isPresent());
    }

    /**
     * Method to test: {@link AbstractBatchItemResult#reason()}
     * <p>
     * Given scenario: A successful item, and one skipped because the run was cancelled before
     * reaching it.
     * <p>
     * Expected result: Neither carries a reason. Skipped is a distinct outcome from failed (FR-028)
     * — a cancelled run's remainder is not a batch of failures, and reporting it as one would tell
     * the author their files were rejected when they were simply never tried.
     */
    @Test
    public void test_successAndSkipped_carryNoReason() {
        final BatchItemResult ok = BatchItemResult.builder()
                .key("a.png").status(BatchItemStatus.SUCCESS).build();
        final BatchItemResult skipped = BatchItemResult.builder()
                .key("z.png").status(BatchItemStatus.SKIPPED).build();

        assertFalse(ok.reason().isPresent());
        assertFalse(skipped.reason().isPresent());
        assertEquals(BatchItemStatus.SKIPPED, skipped.status());
    }

    /**
     * Method to test: the extraction itself (T011, research R3)
     * <p>
     * Given scenario: The shipped bulk refresh record is asked for its shared view.
     * <p>
     * Expected result: It exposes one, and the shipped accessors still work. This is SC-006 — the
     * generalization must be an <b>extraction</b>, not a second shape and not a rewrite: bulk
     * refresh keeps {@code identifier()} and {@code inodes()}, and gains a way to be read as the
     * shared type so #37062 and #37063 consume one contract rather than three.
     * <p>
     * <b>Expected to FAIL until T011.</b>
     */
    @Test
    public void test_bulkRefreshRecord_isExpressibleAsTheSharedType() {
        final BulkRefreshItemResult shipped = BulkRefreshItemResult.builder()
                .identifier("abc-123")
                .addInodes("inode-1", "inode-2")
                .status(com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshItemStatus.SUCCESS)
                .build();

        // the shipped accessors must survive untouched
        assertEquals(2, shipped.inodes().size());
        assertTrue(shipped.identifier().isPresent());

        final BatchItemResult shared = shipped.asBatchItemResult();
        assertEquals("abc-123", shared.key());
        assertEquals(BatchItemStatus.SUCCESS, shared.status());
    }
}
