package com.dotcms.jobs.business.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link BatchFailureReason}, asserting the four values bulk folder delete adds
 * (spec.md FR-019, #37063) land as an <b>additive</b> change to a shared enum bulk upload and bulk
 * refresh already read.
 * <p>
 * <b>Expected to FAIL until T011</b> ({@code PATH_NOT_FOUND}, {@code PROTECTED_FOLDER},
 * {@code IN_USE}, {@code COVERED_BY_PARENT} do not exist on {@code main} yet) — this is the Red
 * phase for Foundational task T006.
 * <p>
 * <b>Named to match the frontend half, not the spec's original sketch</b> — reconciled 2026-09-19
 * against {@code DOT_FOLDER_DELETE_FAILURE_REASONS} (PR dotCMS/core#37612), which had already
 * fixed these names independently, with client copy already written against them. Adopting them
 * here rather than asking the frontend to rename avoids losing that copy to the
 * {@code UNCLASSIFIED} fallback.
 */
public class BatchFailureReasonTest {

    /**
     * Method to test: {@link BatchFailureReason} enum values
     * <p>
     * Given scenario: The four reasons bulk folder delete's spec agrees with the client (FR-019) —
     * an unresolvable/non-folder path, a protected folder, locked content, and an ancestor removing
     * a descendant first.
     * <p>
     * Expected result: All four exist as named constants on the shared enum.
     */
    @Test
    public void test_bulkFolderDeleteReasons_exist() {
        assertEquals("PATH_NOT_FOUND", BatchFailureReason.PATH_NOT_FOUND.name());
        assertEquals("PROTECTED_FOLDER", BatchFailureReason.PROTECTED_FOLDER.name());
        assertEquals("IN_USE", BatchFailureReason.IN_USE.name());
        assertEquals("COVERED_BY_PARENT", BatchFailureReason.COVERED_BY_PARENT.name());
    }

    /**
     * Method to test: {@link BatchFailureReason} enum values
     * <p>
     * Given scenario: The reasons bulk upload already shipped with, and the two reasons FR-019 says
     * are reused rather than added ({@code PERMISSION_DENIED}, {@code UNCLASSIFIED}).
     * <p>
     * Expected result: All of them still exist, unrenamed and unremoved — confirming this feature's
     * change to the enum is purely additive, not a change to what bulk upload/refresh already read.
     */
    @Test
    public void test_existingReasons_areUnaffected() {
        final List<String> existing = Arrays.asList(
                "OVER_SIZE_LIMIT", "DISALLOWED_FILE_TYPE", "NAME_COLLISION",
                "FOLDER_FILTER_MISMATCH", "PERMISSION_DENIED", "STAGED_CONTENT_UNAVAILABLE",
                "UNCLASSIFIED"
        );

        final EnumSet<BatchFailureReason> all = EnumSet.allOf(BatchFailureReason.class);
        final List<String> names = all.stream().map(Enum::name).toList();

        for (final String name : existing) {
            assertTrue("expected pre-existing reason " + name + " to still be present",
                    names.contains(name));
        }
    }
}
