package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;

/**
 * Unit tests for {@link IndexTag}.
 *
 * <p>Verifies the vendor-marker contract: tagging is idempotent, untagging is the
 * exact inverse of tagging, detection ({@link IndexTag#isTagged}) only fires on the
 * owning vendor's marker, and the static helpers ({@link IndexTag#vendorOf},
 * {@link IndexTag#resolve}, {@link IndexTag#strip}) default untagged (legacy) names
 * to Elasticsearch. The {@code .os} suffix is treated as an opaque marker so the
 * tests stay valid if the literal ever changes.</p>
 */
public class IndexTagTest {

    private static final String RAW = "cluster_08abc3.working_20260406";

    // =========================================================================
    // tag(String) — applies the marker, idempotent, null-safe
    // =========================================================================

    /**
     * Given Scenario: A raw, untagged name is tagged with {@link IndexTag#OS}.
     * Expected Result: The OS marker is applied and {@link IndexTag#isTagged} reports it.
     */
    @Test
    public void test_tag_OS_appliesMarker() {
        final String tagged = IndexTag.OS.tag(RAW);

        assertEquals(RAW + IndexTag.OS.suffix, tagged);
        assertTrue(IndexTag.OS.isTagged(tagged));
    }

    /**
     * Given Scenario: A name already tagged with OS is tagged again.
     * Expected Result: It is returned unchanged — markers are never stacked.
     */
    @Test
    public void test_tag_OS_isIdempotent() {
        final String once = IndexTag.OS.tag(RAW);
        final String twice = IndexTag.OS.tag(once);

        assertEquals(once, twice);
    }

    /**
     * Given Scenario: {@link IndexTag#ES} (empty prefix and suffix) tags a name.
     * Expected Result: The name is returned unchanged — ES applies no marker.
     */
    @Test
    public void test_tag_ES_isNoOp() {
        assertEquals(RAW, IndexTag.ES.tag(RAW));
        assertFalse(IndexTag.ES.isTagged(RAW));
    }

    /**
     * Given Scenario: A null name is passed to {@link IndexTag#tag}.
     * Expected Result: Null is returned, no exception.
     */
    @Test
    public void test_tag_null_returnsNull() {
        assertNull(IndexTag.OS.tag(null));
    }

    // =========================================================================
    // untag(String) — exact inverse of tag (feedback: IndexTag.java:139)
    // =========================================================================

    /**
     * Given Scenario: A name carrying the OS marker is untagged.
     * Expected Result: The marker is removed, yielding the original raw name.
     */
    @Test
    public void test_untag_OS_removesMarker() {
        final String tagged = IndexTag.OS.tag(RAW);

        assertEquals(RAW, IndexTag.OS.untag(tagged));
    }

    /**
     * Given Scenario: tag then untag with OS on a raw name (round trip).
     * Expected Result: The original name is recovered exactly.
     */
    @Test
    public void test_untag_OS_roundTrip() {
        assertEquals(RAW, IndexTag.OS.untag(IndexTag.OS.tag(RAW)));
    }

    /**
     * Given Scenario: An untagged name is untagged with OS.
     * Expected Result: It is returned unchanged — untag is safe on untagged input.
     */
    @Test
    public void test_untag_OS_onUntaggedName_returnsUnchanged() {
        assertEquals(RAW, IndexTag.OS.untag(RAW));
    }

    /**
     * Given Scenario: An ES-untag (empty suffix) is applied to any name.
     * Expected Result: The name is returned unchanged — ES has no marker to strip.
     */
    @Test
    public void test_untag_ES_isNoOp() {
        final String osTagged = IndexTag.OS.tag(RAW);

        assertEquals(RAW, IndexTag.ES.untag(RAW));
        // ES must not strip another vendor's marker.
        assertEquals(osTagged, IndexTag.ES.untag(osTagged));
    }

    /**
     * Given Scenario: A null name is passed to {@link IndexTag#untag}.
     * Expected Result: Null is returned, no exception.
     */
    @Test
    public void test_untag_null_returnsNull() {
        assertNull(IndexTag.OS.untag(null));
    }

    // =========================================================================
    // isTagged(String) — owning-vendor detection only (feedback: IndexTag.java:157)
    // =========================================================================

    /**
     * Given Scenario: A name carrying the OS marker is checked.
     * Expected Result: {@link IndexTag#OS} reports tagged, {@link IndexTag#ES} does not.
     */
    @Test
    public void test_isTagged_OS_detectsOnlyItsOwnMarker() {
        final String tagged = IndexTag.OS.tag(RAW);

        assertTrue(IndexTag.OS.isTagged(tagged));
        assertFalse(IndexTag.ES.isTagged(tagged));
    }

    /**
     * Given Scenario: An untagged (legacy) name is checked for the OS marker.
     * Expected Result: Not tagged.
     */
    @Test
    public void test_isTagged_OS_onUntaggedName_isFalse() {
        assertFalse(IndexTag.OS.isTagged(RAW));
    }

    /**
     * Given Scenario: Any name is checked against {@link IndexTag#ES} (empty marker).
     * Expected Result: Always false — ES never matches because it has no marker.
     */
    @Test
    public void test_isTagged_ES_isAlwaysFalse() {
        assertFalse(IndexTag.ES.isTagged(RAW));
        assertFalse(IndexTag.ES.isTagged(IndexTag.OS.tag(RAW)));
        assertFalse(IndexTag.ES.isTagged(""));
    }

    /**
     * Given Scenario: A null name is checked.
     * Expected Result: Not tagged — null-safe.
     */
    @Test
    public void test_isTagged_null_isFalse() {
        assertFalse(IndexTag.OS.isTagged(null));
    }

    // =========================================================================
    // vendorOf(String) — distinguishes "tagged" from "no tag"
    // =========================================================================

    /**
     * Given Scenario: An OS-tagged name is inspected with {@link IndexTag#vendorOf}.
     * Expected Result: Optional containing {@link IndexTag#OS}.
     */
    @Test
    public void test_vendorOf_OSTagged_returnsOS() {
        assertEquals(Optional.of(IndexTag.OS), IndexTag.vendorOf(IndexTag.OS.tag(RAW)));
    }

    /**
     * Given Scenario: An untagged name is inspected with {@link IndexTag#vendorOf}.
     * Expected Result: Empty — no marker is present (unlike {@link IndexTag#resolve}).
     */
    @Test
    public void test_vendorOf_untagged_isEmpty() {
        assertEquals(Optional.empty(), IndexTag.vendorOf(RAW));
    }

    /**
     * Given Scenario: A null name is inspected with {@link IndexTag#vendorOf}.
     * Expected Result: Empty — null-safe.
     */
    @Test
    public void test_vendorOf_null_isEmpty() {
        assertEquals(Optional.empty(), IndexTag.vendorOf(null));
    }

    // =========================================================================
    // resolve(String) — routing default to ES
    // =========================================================================

    /**
     * Given Scenario: An OS-tagged name is resolved.
     * Expected Result: {@link IndexTag#OS}.
     */
    @Test
    public void test_resolve_OSTagged_returnsOS() {
        assertSame(IndexTag.OS, IndexTag.resolve(IndexTag.OS.tag(RAW)));
    }

    /**
     * Given Scenario: An untagged (legacy) name is resolved.
     * Expected Result: {@link IndexTag#ES} — untagged names route to Elasticsearch.
     */
    @Test
    public void test_resolve_untagged_defaultsToES() {
        assertSame(IndexTag.ES, IndexTag.resolve(RAW));
    }

    /**
     * Given Scenario: A null name is resolved.
     * Expected Result: {@link IndexTag#ES} — the safe routing default.
     */
    @Test
    public void test_resolve_null_defaultsToES() {
        assertSame(IndexTag.ES, IndexTag.resolve(null));
    }

    // =========================================================================
    // strip(String) — removes any known vendor marker
    // =========================================================================

    /**
     * Given Scenario: An OS-tagged name is stripped statically.
     * Expected Result: The raw name without any marker.
     */
    @Test
    public void test_strip_OSTagged_returnsRaw() {
        assertEquals(RAW, IndexTag.strip(IndexTag.OS.tag(RAW)));
    }

    /**
     * Given Scenario: An untagged name is stripped.
     * Expected Result: Returned unchanged — safe on already-raw names.
     */
    @Test
    public void test_strip_untagged_returnsUnchanged() {
        assertEquals(RAW, IndexTag.strip(RAW));
    }

    /**
     * Given Scenario: A null name is stripped.
     * Expected Result: Null — null-safe.
     */
    @Test
    public void test_strip_null_returnsNull() {
        assertNull(IndexTag.strip(null));
    }
}
