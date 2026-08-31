package com.dotcms.rest.api.v1.content.bulkrefresh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.rest.exception.ValidationException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link BulkRefreshForm}.
 */
public class BulkRefreshFormTest {

    /**
     * Method to test: {@link BulkRefreshForm} constructor
     * <p>
     * Given scenario: Only {@code contentletIds} is supplied; both flags are omitted (null).
     * <p>
     * Expected result: Both flags default to false. This matters for {@code includeDependencies} in
     * particular — the single-item {@code _refresh} always includes dependencies, so a caller
     * migrating to the bulk endpoint must not silently inherit a per-item {@code loadDeps()} fan-out
     * across the whole batch.
     */
    @Test
    public void test_flags_defaultToFalse_whenOmitted() {
        final BulkRefreshForm form = new BulkRefreshForm(List.of("inode-1"), null, null);

        assertFalse("includeDependencies must default to false", form.isIncludeDependencies());
        assertFalse("includeItemResults must default to false", form.isIncludeItemResults());
        assertEquals(List.of("inode-1"), form.getContentletIds());
    }

    /**
     * Method to test: {@link BulkRefreshForm} constructor
     * <p>
     * Given scenario: Both flags are explicitly true.
     * <p>
     * Expected result: Both are honored.
     */
    @Test
    public void test_flags_honorExplicitTrue() {
        final BulkRefreshForm form = new BulkRefreshForm(List.of("inode-1"), true, true);

        assertTrue(form.isIncludeDependencies());
        assertTrue(form.isIncludeItemResults());
    }

    /**
     * Method to test: {@link BulkRefreshForm} constructor
     * <p>
     * Given scenario: {@code contentletIds} is an empty list.
     * <p>
     * Expected result: Rejected at construction. An empty selection is a client bug, not a no-op
     * job — enqueuing one would hand back a job id that can only ever report zero work.
     */
    @Test(expected = ValidationException.class)
    public void test_emptyContentletIds_isRejected() {
        new BulkRefreshForm(Collections.emptyList(), null, null);
    }

    /**
     * Method to test: {@link BulkRefreshForm} constructor
     * <p>
     * Given scenario: {@code contentletIds} is missing from the JSON altogether.
     * <p>
     * Expected result: Rejected at construction, same as an empty list.
     */
    @Test(expected = ValidationException.class)
    public void test_nullContentletIds_isRejected() {
        new BulkRefreshForm(null, null, null);
    }

    /**
     * Method to test: {@link BulkRefreshForm} constructor
     * <p>
     * Given scenario: The same inode is submitted several times.
     * <p>
     * Expected result: Accepted as-is. Duplicates are a normal consequence of a grid selection and
     * are collapsed by identifier server-side, so the form must not reject them — nor de-duplicate
     * them here, because the response reports the raw submitted count separately from the
     * de-duplicated total.
     */
    @Test
    public void test_duplicateInodes_areAccepted() {
        final BulkRefreshForm form =
                new BulkRefreshForm(List.of("inode-1", "inode-1", "inode-2"), null, null);

        assertEquals("Duplicates must survive the form untouched", 3,
                form.getContentletIds().size());
    }

    /**
     * Method to test: {@link BulkRefreshForm#toString()}
     * <p>
     * Given scenario: A form holding inodes is logged.
     * <p>
     * Expected result: The count is reported rather than the inodes themselves, so a 500-item
     * submission does not dump 500 ids into the log on every debug line.
     */
    @Test
    public void test_toString_reportsCountNotContents() {
        final String asString =
                new BulkRefreshForm(List.of("inode-1", "inode-2"), null, null).toString();

        assertTrue("Should report the count", asString.contains("2 item(s)"));
        assertFalse("Should not spell out the inodes", asString.contains("inode-1"));
    }
}
