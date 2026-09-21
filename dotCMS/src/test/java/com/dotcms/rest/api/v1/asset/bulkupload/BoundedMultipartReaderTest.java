package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.jobs.business.batch.BatchFailureReason;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link BoundedMultipartReader} (spec FR-010a, FR-013c.2; research R10).
 * <p>
 * The point of every test here is <b>where the read stops</b>. A ceiling checked after the body has
 * arrived is not a bound — an author could write an unbounded amount to shared storage and only
 * then be refused. These assert that the reader stops mid-body, which is the difference between a
 * limit on what gets <i>processed</i> and a limit on what can be <i>written</i>.
 */
public class BoundedMultipartReaderTest {

    private static List<UploadPart> parts(final int count, final int bytesEach) {
        final List<UploadPart> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            parts.add(new UploadPart("file-" + i + ".bin", new ByteArrayInputStream(new byte[bytesEach])));
        }
        return parts;
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read}
     * <p>
     * Given scenario: A submission inside both ceilings.
     * <p>
     * Expected result: Every part is staged, in submission order — which the outcome later relies
     * on, because results are listed in the order the author chose the files (FR-015).
     */
    @Test
    public void test_read_stagesEveryPartInOrder_whenInsideBothCeilings() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader = new BoundedMultipartReader(staging, 100, 1_000_000L);

        final List<StagedPart> result = reader.read(parts(3, 10));

        assertEquals(3, result.size());
        assertEquals("file-0.bin", result.get(0).fileName());
        assertEquals("file-2.bin", result.get(2).fileName());
        assertTrue("nothing is reclaimed on the happy path", staging.reclaimed().isEmpty());
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read}
     * <p>
     * Given scenario: 5 parts against a file-count ceiling of 3.
     * <p>
     * Expected result: Refused as {@code FILE_COUNT}, and — the assertion that matters — <b>the
     * fourth and fifth parts were never staged</b>. Reading the whole body first and counting
     * afterwards would produce the same exception while having written every byte.
     */
    @Test
    public void test_read_abortsMidBody_whenTheFileCountCeilingIsCrossed() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader = new BoundedMultipartReader(staging, 3, 1_000_000L);

        final BulkUploadRefusedException refused = assertThrows(BulkUploadRefusedException.class,
                () -> reader.read(parts(5, 10)));

        assertEquals(BulkUploadRefusedException.Ceiling.FILE_COUNT, refused.ceiling());
        assertTrue("the read must stop at the ceiling, not after the body",
                staging.staged().size() <= 3);
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read}
     * <p>
     * Given scenario: 5 parts of 100 bytes against a total ceiling of 250 bytes.
     * <p>
     * Expected result: Refused as {@code TOTAL_SIZE} — answered {@code 413} by the resource — with
     * at most 3 parts staged. The total is accumulated from what staging <b>measured</b>, never
     * from a figure the caller declared (FR-013), because a caller can under-declare or omit one.
     */
    @Test
    public void test_read_abortsMidBody_whenTheTotalSizeCeilingIsCrossed() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader = new BoundedMultipartReader(staging, 100, 250L);

        final BulkUploadRefusedException refused = assertThrows(BulkUploadRefusedException.class,
                () -> reader.read(parts(5, 100)));

        assertEquals(BulkUploadRefusedException.Ceiling.TOTAL_SIZE, refused.ceiling());
        assertTrue("no more than the ceiling may reach disk",
                staging.staged().size() <= 3);
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read}
     * <p>
     * Given scenario: A submission exactly at both ceilings — 3 parts of 100 bytes, ceilings of 3
     * and 300.
     * <p>
     * Expected result: Accepted. The ceiling is a maximum, not an exclusive bound; refusing the
     * configured maximum would make the documented default a lie.
     */
    @Test
    public void test_read_acceptsASubmissionExactlyAtBothCeilings() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader = new BoundedMultipartReader(staging, 3, 300L);

        assertEquals(3, reader.read(parts(3, 100)).size());
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read}
     * <p>
     * Given scenario: One part in the middle of an otherwise valid batch is larger than the
     * staging layer's per-file ceiling.
     * <p>
     * Expected result: That part alone is refused, carrying {@code OVER_SIZE_LIMIT}, and
     * <b>every other part still stages</b>.
     * <p>
     * <b>Before this, the whole submission died.</b> The staging layer enforces the same ceiling
     * but reports it as a {@code DotStateException} carrying a size-flavoured translated message —
     * and raises the identical class and wording when the read simply dies underneath it. So the
     * two were indistinguishable, the safe reading was "the read died", and one over-size file
     * refused everything the author had sent. FR-011 says the opposite: a size rejection is that
     * file's own failure and must not fail the batch.
     */
    @Test
    public void test_read_refusesOnlyTheOversizePart_andStagesTheRest() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader =
                new BoundedMultipartReader(staging, 100, 1_000_000L, 50L);

        final List<UploadPart> parts = new ArrayList<>();
        parts.add(new UploadPart("small-1.bin", new ByteArrayInputStream(new byte[10])));
        parts.add(new UploadPart("too-big.bin", new ByteArrayInputStream(new byte[500])));
        parts.add(new UploadPart("small-2.bin", new ByteArrayInputStream(new byte[10])));

        final List<StagedPart> result = reader.read(parts);

        assertEquals("every part must still be accounted for, refused or not", 3, result.size());

        assertFalse("the over-size part is refused", result.get(1).isStaged());
        assertEquals(BatchFailureReason.OVER_SIZE_LIMIT, result.get(1).refusedReason());
        assertNull("a refused part has nothing staged to point at", result.get(1).tempFileId());

        assertTrue("the part before it stages", result.get(0).isStaged());
        assertTrue("and so does the part after it — the batch keeps going",
                result.get(2).isStaged());

        assertEquals("both valid parts reached the staging layer", 2, staging.staged().size());
        assertTrue("and neither was reclaimed, because the read completed",
                staging.leaked().size() == 2);
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read}
     * <p>
     * Given scenario: No per-file ceiling is configured — which is how the staging layer ships
     * ({@code TEMP_RESOURCE_MAX_FILE_SIZE} defaults to {@code -1}).
     * <p>
     * Expected result: Nothing is wrapped and nothing is refused, however large the part.
     * <p>
     * The negative control for the test above: it confirms the new bound is the configured ceiling
     * and not something this reader invented, so the default installation behaves exactly as it did.
     */
    @Test
    public void test_read_appliesNoPerFileCeiling_whenNoneIsConfigured() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader =
                new BoundedMultipartReader(staging, 100, 1_000_000L, -1L);

        final List<StagedPart> result = reader.read(parts(2, 5_000));

        assertEquals(2, result.size());
        assertTrue("with no ceiling configured, a large part is not refused",
                result.get(0).isStaged() && result.get(1).isStaged());
    }

}
