package com.dotcms.util.content.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.common.db.Params;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * Covers the batching contract of
 * {@link PopulateContentletAsJSONUtil#inBatchesOf(List, java.util.function.Consumer)}.
 *
 * <p>The behaviour under test used to be spread over two places — a per-record method that appended
 * to a shared accumulator and flushed it when full, and a caller that had to remember a second flush
 * for the remainder. These tests pin the part that mattered and was easiest to get wrong: the short
 * final batch.</p>
 */
public class PopulateContentletAsJSONUtilBatchingTest {

    /** Whatever the configured batch size is, these tests derive their expectations from it. */
    private static final int BATCH = 200;

    private static List<Params> params(final int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new Params("inode-" + i, "{\"n\":" + i + "}"))
                .collect(java.util.stream.Collectors.toList());
    }

    private static List<List<Params>> collectBatches(final int count) {
        final List<List<Params>> batches = new ArrayList<>();
        PopulateContentletAsJSONUtil.inBatchesOf(params(count), batches::add);
        return batches;
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#inBatchesOf(List, java.util.function.Consumer)}
     * Given scenario: a record count that is not a multiple of the batch size.
     * Expected result: full batches, and a final short batch holding the remainder.
     */
    @Test
    public void test_inBatchesOf_partialFinalBatch_isEmitted() {

        final List<List<Params>> batches = collectBatches((BATCH * 2) + 50);

        assertEquals("two full batches plus the remainder", 3, batches.size());
        assertEquals(BATCH, batches.get(0).size());
        assertEquals(BATCH, batches.get(1).size());
        assertEquals("the short tail is emitted, not dropped", 50, batches.get(2).size());
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#inBatchesOf(List, java.util.function.Consumer)}
     * Given scenario: a record count that is an exact multiple of the batch size.
     * Expected result: only full batches, and no trailing empty one.
     */
    @Test
    public void test_inBatchesOf_exactMultiple_emitsNoEmptyTail() {

        final List<List<Params>> batches = collectBatches(BATCH * 2);

        assertEquals(2, batches.size());
        batches.forEach(batch -> assertEquals(BATCH, batch.size()));
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#inBatchesOf(List, java.util.function.Consumer)}
     * Given scenario: fewer records than one batch.
     * Expected result: a single short batch. The old shape reached this only through the caller's
     * separate leftover flush, which is the line that was easy to forget.
     */
    @Test
    public void test_inBatchesOf_fewerThanOneBatch_stillEmits() {

        final List<List<Params>> batches = collectBatches(7);

        assertEquals(1, batches.size());
        assertEquals(7, batches.get(0).size());
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#inBatchesOf(List, java.util.function.Consumer)}
     * Given scenario: no records at all.
     * Expected result: the executor is never invoked — no empty batch reaches the database.
     */
    @Test
    public void test_inBatchesOf_empty_neverCallsTheExecutor() {

        assertTrue(collectBatches(0).isEmpty());
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#inBatchesOf(List, java.util.function.Consumer)}
     * Given scenario: every record is delivered exactly once, in order.
     * Expected result: flattening the batches reproduces the input.
     */
    @Test
    public void test_inBatchesOf_deliversEveryRecordOnce_inOrder() {

        final int count = (BATCH * 3) + 1;
        final List<Params> flattened = collectBatches(count).stream()
                .flatMap(List::stream)
                .collect(java.util.stream.Collectors.toList());

        assertEquals(count, flattened.size());
        assertEquals(params(count).toString(), flattened.toString());
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#inBatchesOf(List, java.util.function.Consumer)}
     * Given scenario: a consumer that tries to mutate the batch it was handed.
     * Expected result: it fails. This is why the batch executors no longer clear their argument —
     * a window is unmodifiable, and the old {@code finally { params.clear(); }} would now throw.
     */
    @Test
    public void test_inBatchesOf_windowIsUnmodifiable() {

        final List<List<Params>> batches = collectBatches(10);

        assertThrows(UnsupportedOperationException.class, () -> batches.get(0).clear());
    }
}
