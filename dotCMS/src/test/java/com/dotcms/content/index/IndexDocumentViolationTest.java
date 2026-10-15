package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.IndexDocumentConstraints.Kind;
import com.dotcms.content.index.IndexDocumentConstraints.Violation;
import java.util.Optional;
import org.junit.Test;

/**
 * Unit tests for reading an {@link IndexDocumentViolation} back from the journal reason written
 * by {@link IndexDocumentViolation#toFailureReason()} (#37269, AC-007).
 *
 * <p>The reindex journal stores the failure reason as text only and keys its rows by identifier,
 * so the reason is the only place that records which version was withheld. The failed-records
 * endpoint recovers the identifier, inode, language, field, length and limit from it. Writing and
 * reading the format live in the same class, and these tests pin the round trip.</p>
 */
public class IndexDocumentViolationTest {

    /**
     * Given Scenario: The reason of a string-length violation.
     * Expected Result: Reading it back yields the same identifier, inode, language, kind, field,
     * actual length and limit.
     */
    @Test
    public void test_stringLengthReason_roundTrips() {
        final IndexDocumentViolation written = new IndexDocumentViolation(
                "d5edd758f166259247356e7ea2deca4e", "9f8e7d6c-1111-2222-3333-444455556666",
                4_791_063L, new Violation(Kind.STRING_LENGTH, "catchall", 42_000_792L,
                        20_000_000L));

        final Optional<IndexDocumentViolation> read =
                IndexDocumentViolation.violationOf(written.toFailureReason());

        assertEquals(Optional.of(written), read);
    }

    /**
     * Given Scenario: The reason of a nesting-depth violation on a nested path.
     * Expected Result: The round trip preserves the kind and the path.
     */
    @Test
    public void test_nestingDepthReason_roundTrips() {
        final IndexDocumentViolation written = new IndexDocumentViolation("id", "inode", 1L,
                new Violation(Kind.NESTING_DEPTH, "blocks.content[0].attrs", 1001L, 1000L));

        final Optional<IndexDocumentViolation> read =
                IndexDocumentViolation.violationOf(written.toFailureReason());

        assertEquals(Optional.of(written), read);
    }

    /**
     * Given Scenario: A reason where the violation is followed by the primary's error, as written
     * when queueing a sibling also failed.
     * Expected Result: The violation is still read.
     */
    @Test
    public void test_reasonWithTrailingError_stillReadsViolation() {
        final IndexDocumentViolation written = new IndexDocumentViolation("id", "inode", 1L,
                new Violation(Kind.STRING_LENGTH, "body", 25_000_000L, 20_000_000L));
        final String reason = written.toFailureReason() + "; primary rejected document";

        assertEquals(Optional.of(written), IndexDocumentViolation.violationOf(reason));
    }

    /**
     * Given Scenario: The reason of an entry whose working and live versions were both withheld,
     * joined with {@code "; "} as the reindex thread writes it.
     * Expected Result: The first withheld version is read, with its own inode and language.
     */
    @Test
    public void test_twoWithheldVersions_readsTheFirst() {
        final IndexDocumentViolation first = new IndexDocumentViolation("id", "inode-working", 2L,
                new Violation(Kind.STRING_LENGTH, "body", 25_000_000L, 20_000_000L));
        final IndexDocumentViolation second = new IndexDocumentViolation("id", "inode-live", 1L,
                new Violation(Kind.STRING_LENGTH, "catchall", 30_000_000L, 20_000_000L));
        final String reason = first.toFailureReason() + "; " + second.toFailureReason();

        assertEquals(Optional.of(first), IndexDocumentViolation.violationOf(reason));
    }

    /**
     * Given Scenario: The reason of a nesting-depth violation whose field path contains an
     * apostrophe, as a user-defined key of a key/value or JSON field can.
     * Expected Result: The round trip still recovers the whole path and the withheld version.
     */
    @Test
    public void test_fieldPathWithApostrophe_roundTrips() {
        final IndexDocumentViolation written = new IndexDocumentViolation("id", "inode", 1L,
                new Violation(Kind.NESTING_DEPTH, "metadata.o'brien[0].value", 1001L, 1000L));

        assertEquals(Optional.of(written),
                IndexDocumentViolation.violationOf(written.toFailureReason()));
    }

    /**
     * Given Scenario: The reason of a withheld document whose field path contains a line break,
     * followed by a second withheld document's reason, as the journal joins them.
     * Expected Result: The first document is read, whole, and not the second one.
     */
    @Test
    public void test_fieldPathWithLineBreak_readsTheFirst() {
        final IndexDocumentViolation first = new IndexDocumentViolation("id", "inode-1", 1L,
                new Violation(Kind.NESTING_DEPTH, "metadata.line\nbreak.value", 1001L, 1000L));
        final IndexDocumentViolation second = new IndexDocumentViolation("id", "inode-2", 1L,
                new Violation(Kind.STRING_LENGTH, "body", 25_000_000L, 20_000_000L));

        assertEquals(Optional.of(first), IndexDocumentViolation.violationOf(
                first.toFailureReason() + "; " + second.toFailureReason()));
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
