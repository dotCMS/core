package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Identifier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link NestedHostPatternCache} focusing on the longest-first pattern ordering
 * and matching algorithm.
 *
 * <p>These tests exercise the pure algorithmic behaviour without any database access.  They verify:
 * <ul>
 *   <li>The {@link NestedHostPatternCache.HostPatternEntry} construction and {@code pathPrefix}
 *       derivation.</li>
 *   <li>The {@link NestedHostPatternCache#LONGEST_PREFIX_FIRST} comparator produces the correct
 *       ordering so that deeper (more-specific) hosts are matched before shallower ancestors.</li>
 *   <li>The {@link NestedHostPatternCache#match(String, String)} method returns the most-specific
 *       matching entry.</li>
 *   <li>The {@link NestedHostPatternCache#stripPrefix(NestedHostPatternCache.HostPatternEntry, String)}
 *       method correctly derives the remaining URI.</li>
 *   <li>The {@link NestedHostPatternCache#buildPathPrefix(Identifier)} helper converts
 *       {@code parentPath + assetName} into the expected prefix.</li>
 * </ul>
 */
public class NestedHostPatternCacheTest {

    // -------------------------------------------------------------------------
    // buildPathPrefix tests
    // -------------------------------------------------------------------------

    @Test
    public void buildPathPrefix_rootParent_returnsLeadingSlashAndName() {
        final Identifier id = identifierWithPaths("/", "myhost");
        assertEquals("/myhost", NestedHostPatternCache.buildPathPrefix(id));
    }

    @Test
    public void buildPathPrefix_nestedParent_returnsCombinedPath() {
        final Identifier id = identifierWithPaths("/en/", "sub1");
        assertEquals("/en/sub1", NestedHostPatternCache.buildPathPrefix(id));
    }

    @Test
    public void buildPathPrefix_deeplyNestedParent_returnsCombinedPath() {
        final Identifier id = identifierWithPaths("/en/sub1/", "child");
        assertEquals("/en/sub1/child", NestedHostPatternCache.buildPathPrefix(id));
    }

    @Test
    public void buildPathPrefix_parentPathWithNoTrailingSlash_stillWorks() {
        // parentPath should always have a trailing slash, but guard against edge cases
        final Identifier id = identifierWithPaths("/en/sub1", "child");
        assertEquals("/en/sub1/child", NestedHostPatternCache.buildPathPrefix(id));
    }

    @Test
    public void buildPathPrefix_nullParentPath_returnsNull() {
        final Identifier id = identifierWithPaths(null, "myhost");
        // null parentPath → null result (the entry should be skipped)
        assertEquals(null, NestedHostPatternCache.buildPathPrefix(id));
    }

    @Test
    public void buildPathPrefix_emptyAssetName_returnsNull() {
        final Identifier id = identifierWithPaths("/en/", "");
        assertEquals(null, NestedHostPatternCache.buildPathPrefix(id));
    }

    // -------------------------------------------------------------------------
    // LONGEST_PREFIX_FIRST comparator tests
    // -------------------------------------------------------------------------

    @Test
    public void longestPrefixFirst_deeperHostSortsBefore_shallowerHost() {
        final NestedHostPatternCache.HostPatternEntry shallow =
                makeEntry("/en/sub1", "host-shallow");
        final NestedHostPatternCache.HostPatternEntry deep   =
                makeEntry("/en/sub1/child", "host-deep");

        final List<NestedHostPatternCache.HostPatternEntry> list = new ArrayList<>(
                Arrays.asList(shallow, deep));
        list.sort(NestedHostPatternCache.LONGEST_PREFIX_FIRST);

        assertEquals("deep host must be first", "host-deep",   list.get(0).hostId);
        assertEquals("shallow host must be second", "host-shallow", list.get(1).hostId);
    }

    @Test
    public void longestPrefixFirst_threeLevelHierarchy_correctOrder() {
        final NestedHostPatternCache.HostPatternEntry l1 = makeEntry("/en",         "host-l1");
        final NestedHostPatternCache.HostPatternEntry l2 = makeEntry("/en/sub1",    "host-l2");
        final NestedHostPatternCache.HostPatternEntry l3 = makeEntry("/en/sub1/c",  "host-l3");

        final List<NestedHostPatternCache.HostPatternEntry> list = new ArrayList<>(
                Arrays.asList(l1, l3, l2)); // intentionally scrambled
        list.sort(NestedHostPatternCache.LONGEST_PREFIX_FIRST);

        assertEquals("l3 first",  "host-l3", list.get(0).hostId);
        assertEquals("l2 second", "host-l2", list.get(1).hostId);
        assertEquals("l1 third",  "host-l1", list.get(2).hostId);
    }

    @Test
    public void longestPrefixFirst_equalLengthSiblings_alphabeticalOrder() {
        // Two sibling hosts with equal-length prefixes get a stable alphabetical order
        final NestedHostPatternCache.HostPatternEntry fr =
                makeEntry("/fr/sub1", "host-fr");
        final NestedHostPatternCache.HostPatternEntry en =
                makeEntry("/en/sub1", "host-en");

        final List<NestedHostPatternCache.HostPatternEntry> list = new ArrayList<>(
                Arrays.asList(fr, en));
        list.sort(NestedHostPatternCache.LONGEST_PREFIX_FIRST);

        // Both prefixes are 8 chars; alphabetically "/en/sub1" < "/fr/sub1"
        assertEquals("en sorts first alphabetically", "host-en", list.get(0).hostId);
        assertEquals("fr sorts second",               "host-fr", list.get(1).hostId);
    }

    @Test
    public void longestPrefixFirst_alreadySorted_unchangedOrder() {
        final NestedHostPatternCache.HostPatternEntry deep   = makeEntry("/en/sub1/child", "d");
        final NestedHostPatternCache.HostPatternEntry shallow = makeEntry("/en/sub1",      "s");

        final List<NestedHostPatternCache.HostPatternEntry> list = new ArrayList<>(
                Arrays.asList(deep, shallow));
        list.sort(NestedHostPatternCache.LONGEST_PREFIX_FIRST);

        assertEquals("deep still first", "d", list.get(0).hostId);
        assertEquals("shallow still second", "s", list.get(1).hostId);
    }

    // -------------------------------------------------------------------------
    // Pattern matching tests (using an in-memory NestedHostPatternCache)
    // -------------------------------------------------------------------------

    /**
     * Verifies that when two patterns could both match a URI, the longer (deeper) pattern is
     * returned by {@link NestedHostPatternCache#match}.
     */
    @Test
    public void match_deeperPatternWinsOverShallowerAncestor() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        // Pre-populate with a manually-built bucket, bypassing the DB
        final NestedHostPatternCache.HostPatternEntry shallowEntry =
                makeEntry("/en/sub1", "id-shallow");
        final NestedHostPatternCache.HostPatternEntry deepEntry =
                makeEntry("/en/sub1/child", "id-deep");

        injectBucket(cache, "top-host-id",
                sorted(Arrays.asList(shallowEntry, deepEntry)));

        final Optional<NestedHostPatternCache.HostPatternEntry> result =
                cache.match("top-host-id", "/en/sub1/child/page.html");

        assertTrue("A match should be found", result.isPresent());
        assertEquals("The deep (more-specific) host must win",
                "id-deep", result.get().hostId);
    }

    @Test
    public void match_shallowPatternMatchesWhenDeepDoesNot() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry shallowEntry =
                makeEntry("/en/sub1", "id-shallow");
        final NestedHostPatternCache.HostPatternEntry deepEntry =
                makeEntry("/en/sub1/child", "id-deep");

        injectBucket(cache, "top-host-id",
                sorted(Arrays.asList(shallowEntry, deepEntry)));

        // /en/sub1/other matches /en/sub1 but NOT /en/sub1/child
        final Optional<NestedHostPatternCache.HostPatternEntry> result =
                cache.match("top-host-id", "/en/sub1/other/page.html");

        assertTrue("Shallow match should be found", result.isPresent());
        assertEquals("Should resolve to the shallow host", "id-shallow", result.get().hostId);
    }

    @Test
    public void match_noPatternMatch_returnsEmpty() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id-sub1");
        injectBucket(cache, "top-host-id", sorted(Arrays.asList(entry)));

        final Optional<NestedHostPatternCache.HostPatternEntry> result =
                cache.match("top-host-id", "/fr/other/page.html");

        assertFalse("No match should be found for a different prefix", result.isPresent());
    }

    @Test
    public void match_uriEqualsPrefix_returnsEntry() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id-sub1");
        injectBucket(cache, "top-host-id", sorted(Arrays.asList(entry)));

        // URI exactly equals the prefix
        final Optional<NestedHostPatternCache.HostPatternEntry> result =
                cache.match("top-host-id", "/en/sub1");

        assertTrue("Exact prefix URI should match", result.isPresent());
        assertEquals("id-sub1", result.get().hostId);
    }

    @Test
    public void match_uriEqualsPrefixWithTrailingSlash_returnsEntry() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id-sub1");
        injectBucket(cache, "top-host-id", sorted(Arrays.asList(entry)));

        final Optional<NestedHostPatternCache.HostPatternEntry> result =
                cache.match("top-host-id", "/en/sub1/");

        assertTrue("Prefix with trailing slash should match", result.isPresent());
    }

    @Test
    public void match_nullTopLevelHostId_returnsEmpty() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        assertFalse(cache.match(null, "/en/sub1/page.html").isPresent());
    }

    @Test
    public void match_nullUri_returnsEmpty() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        assertFalse(cache.match("top-host-id", null).isPresent());
    }

    @Test
    public void match_emptyBucket_returnsEmpty() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        injectBucket(cache, "top-host-id", new ArrayList<>());

        assertFalse(cache.match("top-host-id", "/en/sub1/page.html").isPresent());
    }

    // -------------------------------------------------------------------------
    // stripPrefix tests
    // -------------------------------------------------------------------------

    @Test
    public void stripPrefix_normalUri_stripsCorrectly() {
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id");
        assertEquals("/page.html",
                NestedHostPatternCache.stripPrefix(entry, "/en/sub1/page.html"));
    }

    @Test
    public void stripPrefix_uriEqualsPrefix_returnsRootSlash() {
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id");
        assertEquals("/", NestedHostPatternCache.stripPrefix(entry, "/en/sub1"));
    }

    @Test
    public void stripPrefix_uriEqualsPrefixPlusSlash_returnsRootSlash() {
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id");
        assertEquals("/", NestedHostPatternCache.stripPrefix(entry, "/en/sub1/"));
    }

    @Test
    public void stripPrefix_deepNestedPath_stripsCorrectly() {
        final NestedHostPatternCache.HostPatternEntry entry =
                makeEntry("/en/sub1/child", "id");
        assertEquals("/index.html",
                NestedHostPatternCache.stripPrefix(entry, "/en/sub1/child/index.html"));
    }

    @Test
    public void stripPrefix_rootLevelHost_stripsCorrectly() {
        // A host directly under the root of the top-level host (parentPath = "/")
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/myhost", "id");
        assertEquals("/contact.html",
                NestedHostPatternCache.stripPrefix(entry, "/myhost/contact.html"));
    }

    // -------------------------------------------------------------------------
    // resolve() tests  (the URL-resolution pipeline entry point)
    // -------------------------------------------------------------------------

    /**
     * When a nested host pattern matches the URI, {@link NestedHostPatternCache#resolve} must
     * return a {@link HostResolutionResult} with {@code isNested() == true} and the correct
     * remaining URI.
     */
    @Test
    public void resolve_matchedEntry_returnsNestedResult() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id-sub1");
        injectBucket(cache, "top-host-id", sorted(Arrays.asList(entry)));

        final HostResolutionResult result = cache.resolve("top-host-id", "/en/sub1/page.html");

        assertTrue("Result should be a nested match", result.isNested());
        assertEquals("Resolved host ID should match", "id-sub1", result.getResolvedHostId());
        assertEquals("Remaining URI should be the stripped path", "/page.html", result.getRemainingUri());
    }

    @Test
    public void resolve_noMatch_returnsTopLevelResult() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id-sub1");
        injectBucket(cache, "top-host-id", sorted(Arrays.asList(entry)));

        final HostResolutionResult result = cache.resolve("top-host-id", "/other/page.html");

        assertFalse("Result should not be nested when no pattern matches", result.isNested());
        assertNull("No resolved host ID for a top-level result", result.getResolvedHostId());
        assertEquals("Original URI should be preserved", "/other/page.html", result.getRemainingUri());
    }

    @Test
    public void resolve_deeperPatternWinsOverShallower() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry shallow = makeEntry("/en/sub1",       "id-shallow");
        final NestedHostPatternCache.HostPatternEntry deep    = makeEntry("/en/sub1/child", "id-deep");
        injectBucket(cache, "top-host-id", sorted(Arrays.asList(shallow, deep)));

        final HostResolutionResult result = cache.resolve("top-host-id", "/en/sub1/child/about.html");

        assertTrue(result.isNested());
        assertEquals("Deeper (more-specific) host must win", "id-deep", result.getResolvedHostId());
        assertEquals("Remaining URI after deep prefix", "/about.html", result.getRemainingUri());
    }

    @Test
    public void resolve_uriEqualsPrefix_remainingUriIsRoot() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id-sub1");
        injectBucket(cache, "top-host-id", sorted(Arrays.asList(entry)));

        final HostResolutionResult result = cache.resolve("top-host-id", "/en/sub1");

        assertTrue(result.isNested());
        assertEquals("When URI equals the prefix, remaining URI should be '/'", "/", result.getRemainingUri());
    }

    @Test
    public void resolve_nullTopLevelHostId_returnsTopLevelResult() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final HostResolutionResult result = cache.resolve(null, "/en/sub1/page.html");

        assertFalse(result.isNested());
        assertEquals("/en/sub1/page.html", result.getRemainingUri());
    }

    @Test
    public void resolve_nullUri_returnsTopLevelResult() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final HostResolutionResult result = cache.resolve("top-host-id", null);

        assertFalse(result.isNested());
        assertNull(result.getRemainingUri());
    }

    // -------------------------------------------------------------------------
    // Cache invalidation tests
    // -------------------------------------------------------------------------

    @Test
    public void invalidate_removesSpecificBucket() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id");
        injectBucket(cache, "host-a", sorted(Arrays.asList(entry)));
        injectBucket(cache, "host-b", sorted(Arrays.asList(entry)));

        cache.invalidate("host-a");

        // host-a bucket is gone (will return empty on next getPatterns since no DB is available)
        assertFalse("host-a bucket should be invalidated",
                cache.getCacheMap().containsKey("host-a"));
        // host-b bucket is untouched
        assertTrue("host-b bucket should remain",
                cache.getCacheMap().containsKey("host-b"));
    }

    @Test
    public void invalidateAll_removesAllBuckets() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        final NestedHostPatternCache.HostPatternEntry entry = makeEntry("/en/sub1", "id");
        injectBucket(cache, "host-a", sorted(Arrays.asList(entry)));
        injectBucket(cache, "host-b", sorted(Arrays.asList(entry)));

        cache.invalidateAll();

        assertTrue("Cache map should be empty after invalidateAll",
                cache.getCacheMap().isEmpty());
    }

    @Test
    public void invalidate_nullId_doesNotThrow() {
        final NestedHostPatternCache cache = new NestedHostPatternCache();
        // Must not throw
        cache.invalidate(null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static NestedHostPatternCache.HostPatternEntry makeEntry(
            final String pathPrefix, final String hostId) {
        final Pattern pattern = Pattern.compile(
                "^" + Pattern.quote(pathPrefix) + "(/.*)?$");
        return new NestedHostPatternCache.HostPatternEntry(pattern, hostId, pathPrefix);
    }

    private static Identifier identifierWithPaths(final String parentPath,
                                                   final String assetName) {
        final Identifier id = new Identifier();
        id.setParentPath(parentPath);
        id.setAssetName(assetName);
        id.setId("test-id-" + assetName);
        return id;
    }

    /**
     * Sorts a list of entries using {@link NestedHostPatternCache#LONGEST_PREFIX_FIRST} and
     * returns it as an unmodifiable list — the same transformation that
     * {@code buildPatterns()} performs.
     */
    private static List<NestedHostPatternCache.HostPatternEntry> sorted(
            final List<NestedHostPatternCache.HostPatternEntry> list) {
        final List<NestedHostPatternCache.HostPatternEntry> copy = new ArrayList<>(list);
        copy.sort(NestedHostPatternCache.LONGEST_PREFIX_FIRST);
        return java.util.Collections.unmodifiableList(copy);
    }

    /**
     * Directly inserts a pre-built bucket into the cache's backing map, bypassing the DB-backed
     * {@code buildPatterns()} method.  This allows unit tests to exercise {@link #match} without
     * a live database connection.
     */
    private static void injectBucket(
            final NestedHostPatternCache cache,
            final String topLevelHostId,
            final List<NestedHostPatternCache.HostPatternEntry> entries) {
        cache.getCacheMap().put(topLevelHostId, entries);
    }
}
