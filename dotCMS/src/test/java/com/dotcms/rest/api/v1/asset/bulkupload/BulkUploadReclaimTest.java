package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for reclaim on the paths where a submission ends without becoming a run
 * (spec FR-013d, SC-008).
 * <p>
 * <b>Why these are their own class.</b> Reclaim runs on exception paths, which is where cleanup is
 * forgotten, and the consequence is not recoverable elsewhere: nothing purges staged content on a
 * schedule — the repository has no cleanup task — so anything left here is left permanently, in
 * bytes the author cannot see and no run will ever collect.
 * <p>
 * FR-013d covers two paths and they are <b>not the same code</b>. A refusal is raised by this side,
 * so this side knows to clean up. A read that dies underneath — the author navigated away, the
 * connection dropped — raises nothing from this side, so a reclaim scoped to the refusal path never
 * runs. The second is the likelier of the two, because it is the author's own action rather than a
 * limit being hit.
 */
public class BulkUploadReclaimTest {

    private static List<UploadPart> parts(final int count, final int bytesEach) {
        final List<UploadPart> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            parts.add(new UploadPart("file-" + i + ".bin", new ByteArrayInputStream(new byte[bytesEach])));
        }
        return parts;
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read} — FR-013d.1, the refusal path
     * <p>
     * Given scenario: A submission crosses the total-size ceiling after some parts are already on
     * disk.
     * <p>
     * Expected result: Every part staged before the abort is reclaimed. The refusal never becomes a
     * run, so nothing downstream would ever collect them.
     */
    @Test
    public void test_partsStagedBeforeACeilingRefusal_areReclaimed() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader = new BoundedMultipartReader(staging, 100, 250L);

        assertThrows(BulkUploadRefusedException.class, () -> reader.read(parts(5, 100)));

        assertTrue("staged parts must not survive a refusal", staging.staged().size() > 0);
        assertEquals("everything staged before the abort is reclaimed",
                List.of(), staging.leaked());
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read} — FR-013d.2, the broken-read path
     * <p>
     * Given scenario: The third part fails to stage because the client went away mid-upload. No
     * ceiling was crossed and this side raises nothing of its own.
     * <p>
     * Expected result: The two parts already staged are still reclaimed. This is the case
     * C-001a1 names — the guarantee starts at the handle, and an abandoned submission must cost the
     * author nothing and leave nothing behind. A reclaim written only into the refusal branch
     * passes the test above and fails this one, which is exactly why both exist.
     */
    @Test
    public void test_partsStagedBeforeAReadThatDies_areAlsoReclaimed() {
        final FakeBatchStaging staging = new FakeBatchStaging().failOnPart(2);
        final BoundedMultipartReader reader = new BoundedMultipartReader(staging, 100, 1_000_000L);

        assertThrows(RuntimeException.class, () -> reader.read(parts(5, 10)));

        assertEquals("two parts landed before the client went away", 2, staging.staged().size());
        assertEquals("an abandoned upload leaves nothing behind", List.of(), staging.leaked());
    }

    /**
     * Method to test: {@link BoundedMultipartReader#read}
     * <p>
     * Given scenario: A submission that completes within both ceilings.
     * <p>
     * Expected result: Nothing is reclaimed. The batch is about to run against exactly these parts;
     * reclaiming on the happy path would delete the content the job is queued to process.
     */
    @Test
    public void test_nothingIsReclaimed_whenTheReadCompletes() {
        final FakeBatchStaging staging = new FakeBatchStaging();
        final BoundedMultipartReader reader = new BoundedMultipartReader(staging, 100, 1_000_000L);

        reader.read(parts(3, 10));

        assertTrue(staging.reclaimed().isEmpty());
        assertEquals(3, staging.staged().size());
    }
}
