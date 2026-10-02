package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.IndexDocumentConstraints.Violation;
import com.fasterxml.jackson.core.StreamReadConstraints;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

/**
 * Unit tests for {@link IndexDocumentConstraints}.
 *
 * <p>Both engine clients re-read every index document with Jackson before sending it, and Jackson
 * rejects any single string longer than its read limit (20,000,000 characters by default) or any
 * structure nested deeper than its depth limit. The check under test predicts that rejection on
 * the document {@code Map} before anything is queued, so one bad document can be withheld instead
 * of failing the whole bulk request (#37269).</p>
 *
 * <p>The tests inject small limits through the package-private constructor so they never have to
 * build 20-million-character strings.</p>
 */
public class IndexDocumentConstraintsTest {

    /** Max string length 10, max nesting depth 3. */
    private final IndexDocumentConstraints constraints = new IndexDocumentConstraints(
            StreamReadConstraints.builder().maxStringLength(10).maxNestingDepth(3).build());

    /**
     * Given Scenario: A top-level string is one character over the limit.
     * Expected Result: A string-length violation that names the key and reports the real length
     * and the limit.
     */
    @Test
    public void test_topLevelStringOverLimit_isRejectedWithKeyAndRealLength() {
        final Map<String, Object> document = new LinkedHashMap<>();
        document.put("title", "ok");
        document.put("news.body", "x".repeat(11));

        final Optional<Violation> violation = constraints.check(document);

        assertTrue(violation.isPresent());
        assertEquals(IndexDocumentConstraints.Kind.STRING_LENGTH, violation.get().kind());
        assertEquals("news.body", violation.get().fieldPath());
        assertEquals(11, violation.get().actual());
        assertEquals(10, violation.get().limit());
    }

    /**
     * Given Scenario: An oversized string sits inside a map, inside a list, inside a map.
     * Expected Result: The violation reports the full path to that value.
     */
    @Test
    public void test_nestedStringOverLimit_isRejectedWithFullPath() {
        final Map<String, Object> inner = Map.of("value", "y".repeat(25));
        final Map<String, Object> document = Map.of("relations", List.of("short", inner));

        final Optional<Violation> violation = constraints.check(document);

        assertTrue(violation.isPresent());
        assertEquals("relations[1].value", violation.get().fieldPath());
        assertEquals(25, violation.get().actual());
    }

    /**
     * Given Scenario: Only {@code catchall} — which concatenates every field value — is over the
     * limit, while each individual field is under it.
     * Expected Result: The document is rejected on {@code catchall}. This is why the check must
     * run on the final document map and not on the authored fields.
     */
    @Test
    public void test_catchallOverLimit_isRejectedEvenWhenEachFieldFits() {
        final Map<String, Object> document = new LinkedHashMap<>();
        document.put("news.body", "a".repeat(6));
        document.put("news.summary", "b".repeat(6));
        document.put("catchall", "a".repeat(6) + " " + "b".repeat(6));

        final Optional<Violation> violation = constraints.check(document);

        assertTrue(violation.isPresent());
        assertEquals("catchall", violation.get().fieldPath());
        assertEquals(13, violation.get().actual());
    }

    /**
     * Given Scenario: Maps nested one level deeper than the depth limit.
     * Expected Result: A nesting-depth violation.
     */
    @Test
    public void test_nestingDeeperThanLimit_isRejected() {
        // depth 1 (document) -> 2 -> 3 -> 4
        final Map<String, Object> document = Map.of("a", Map.of("b", Map.of("c", Map.of("d", "x"))));

        final Optional<Violation> violation = constraints.check(document);

        assertTrue(violation.isPresent());
        assertEquals(IndexDocumentConstraints.Kind.NESTING_DEPTH, violation.get().kind());
        assertEquals(4, violation.get().actual());
        assertEquals(3, violation.get().limit());
    }

    /**
     * Given Scenario: A string exactly at the limit and nesting exactly at the depth limit.
     * Expected Result: No violation — Jackson only rejects values strictly over its limits.
     */
    @Test
    public void test_valuesExactlyAtTheLimits_pass() {
        final Map<String, Object> document = Map.of(
                "news.body", "z".repeat(10),
                "a", Map.of("b", Map.of("c", "x")));

        assertFalse(constraints.check(document).isPresent());
    }

    /**
     * Given Scenario: Null values and non-string scalars.
     * Expected Result: They are ignored, not treated as violations.
     */
    @Test
    public void test_nullsAndNonStrings_areIgnored() {
        final Map<String, Object> document = new LinkedHashMap<>();
        document.put("empty", null);
        document.put("count", 123456789012L);
        document.put("flag", Boolean.TRUE);

        assertFalse(constraints.check(document).isPresent());
    }

    /**
     * Given Scenario: The production instance built with the no-arg constructor.
     * Expected Result: It enforces Jackson's effective default string limit, so it tracks the
     * value the engine clients' parsers will actually apply.
     */
    @Test
    public void test_defaultInstance_usesJacksonEffectiveDefaults() {
        assertEquals(StreamReadConstraints.defaults().getMaxStringLength(),
                new IndexDocumentConstraints().maxStringLength());
    }
}
