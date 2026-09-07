package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
}
