package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.IndexDocumentConstraints.Kind;
import com.dotcms.content.index.IndexDocumentConstraints.Violation;
import java.util.Optional;
import org.junit.Test;

/**
 * Unit tests for reading a {@link Violation} back from the journal reason written by
 * {@link IndexDocumentViolation#toFailureReason()} (#37269, AC-007).
 *
 * <p>The reindex journal stores the failure reason as text only, so the failed-records endpoint
 * recovers the field, length and limit from it. Writing and reading the format live in the same
 * class, and these tests pin the round trip.</p>
 */
public class IndexDocumentViolationTest {

    /**
     * Given Scenario: The reason of a string-length violation.
     * Expected Result: Reading it back yields the same kind, field, actual length and limit.
     */
    @Test
    public void test_stringLengthReason_roundTrips() {
        final Violation written = new Violation(Kind.STRING_LENGTH, "catchall", 42_000_792L,
                20_000_000L);

        final Optional<Violation> read = IndexDocumentViolation.violationOf(
                new IndexDocumentViolation("id", "inode", 1L, written).toFailureReason());

        assertEquals(Optional.of(written), read);
    }

    /**
     * Given Scenario: The reason of a nesting-depth violation on a nested path.
     * Expected Result: The round trip preserves the kind and the path.
     */
    @Test
    public void test_nestingDepthReason_roundTrips() {
        final Violation written = new Violation(Kind.NESTING_DEPTH, "blocks.content[0].attrs", 1001L,
                1000L);

        final Optional<Violation> read = IndexDocumentViolation.violationOf(
                new IndexDocumentViolation("id", "inode", 1L, written).toFailureReason());

        assertEquals(Optional.of(written), read);
    }

    /**
     * Given Scenario: A reason where the violation is followed by the primary's error, as written
     * when queueing a sibling also failed.
     * Expected Result: The violation is still read.
     */
    @Test
    public void test_reasonWithTrailingError_stillReadsViolation() {
        final Violation written = new Violation(Kind.STRING_LENGTH, "body", 25_000_000L, 20_000_000L);
        final String reason = new IndexDocumentViolation("id", "inode", 1L, written).toFailureReason()
                + "; primary rejected document";

        assertEquals(Optional.of(written), IndexDocumentViolation.violationOf(reason));
    }

    /**
     * Given Scenario: Reasons that are not document-limit violations, and no reason at all.
     * Expected Result: Nothing is read.
     */
    @Test
    public void test_otherReasons_readNothing() {
        assertFalse(IndexDocumentViolation.violationOf(
                "status line [HTTP/1.1 413 Request Entity Too Large]").isPresent());
        assertFalse(IndexDocumentViolation.violationOf(null).isPresent());
        assertTrue(IndexDocumentViolation.violationOf("").isEmpty());
    }
}
