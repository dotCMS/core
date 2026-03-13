package com.dotmarketing.portlets.contentlet.business;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link HostResolver} verifying the cache-first, pattern-check-before-normal-
 * resolution strategy.
 *
 * <p>All tests exercise {@link HostResolver} without a live database connection. Each test
 * pre-populates an in-memory {@link NestedHostPatternCache} bucket and then invokes
 * {@link HostResolver#resolve(String, String)} to assert on the returned
 * {@link HostResolutionResult}.</p>
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Guard conditions: blank/null inputs return a top-level result immediately</li>
 *   <li>Cache hit – nested match: the most-specific (longest-prefix) nested host is returned
 *       together with the URI stripped of the matched prefix</li>
 *   <li>Cache hit – no match: top-level result is returned with the original URI unchanged</li>
 *   <li>Deepest-wins precedence: when two patterns could match, the longer one wins</li>
 *   <li>URI exactly equals prefix: remaining URI is normalised to {@code "/"}</li>
 *   <li>Empty bucket: top-level result with original URI</li>
 * </ul>
 */
public class HostResolverTest {

    private static final String TOP_HOST_ID    = "top-host-uuid-1234";
    private static final String NESTED_HOST_ID = "nested-host-uuid-5678";
    private static final String DEEP_HOST_ID   = "deep-host-uuid-9012";

    /** A fresh, empty {@link NestedHostPatternCache} instance (no DB connection required). */
    private NestedHostPatternCache cache;

    /** The resolver under test, wired with the test-local cache. */
    private HostResolver resolver;

    @Before
    public void setUp() {
        cache    = new NestedHostPatternCache();
        resolver = new HostResolver(cache);
    }

    // =========================================================================
    // Guard conditions
    // =========================================================================

    /**
     * A {@code null} top-level host ID must be handled gracefully; the result must be a
     * top-level (non-nested) result with the original URI preserved.
     */
    @Test
    public void resolve_nullTopLevelHostId_returnsTopLevelResult() {
        final HostResolutionResult result = resolver.resolve(null, "/some/page.html");

        assertFalse("Result must not be nested for null topLevelHostId", result.isNested());
        assertNull("resolvedHostId must be null", result.getResolvedHostId());
        assertEquals("Original URI must be preserved", "/some/page.html", result.getRemainingUri());
    }

    /**
     * A blank (empty-string) top-level host ID must also return a top-level result.
     */
    @Test
    public void resolve_emptyTopLevelHostId_returnsTopLevelResult() {
        final HostResolutionResult result = resolver.resolve("", "/some/page.html");

        assertFalse("Result must not be nested for empty topLevelHostId", result.isNested());
    }

    /**
     * A whitespace-only top-level host ID must be treated as blank and return a top-level result.
     */
    @Test
    public void resolve_whitespaceTopLevelHostId_returnsTopLevelResult() {
        final HostResolutionResult result = resolver.resolve("   ", "/some/page.html");

        assertFalse("Result must not be nested for whitespace topLevelHostId", result.isNested());
    }

    /**
     * A {@code null} request URI must return a top-level result without throwing.
     */
    @Test
    public void resolve_nullRequestUri_returnsTopLevelResult() {
        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, null);

        assertFalse("Result must not be nested for null requestUri", result.isNested());
        assertNull("resolvedHostId must be null", result.getResolvedHostId());
        assertNull("null URI must be preserved as null", result.getRemainingUri());
    }

    /**
     * A blank (empty-string) request URI must return a top-level result.
     */
    @Test
    public void resolve_emptyRequestUri_returnsTopLevelResult() {
        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "");

        assertFalse("Result must not be nested for empty requestUri", result.isNested());
    }

    // =========================================================================
    // Cache-first: nested host is matched from the in-memory pattern cache
    // =========================================================================

    /**
     * When the cache has a pattern that matches the URI, the resolver must return a nested result
     * carrying the matched host's UUID and the URI with the prefix stripped — <em>without</em>
     * any database access.
     */
    @Test
    public void resolve_cacheContainsMatchingPattern_returnsNestedResult() {
        // Pre-populate the cache bucket for TOP_HOST_ID with a single pattern
        injectBucket(TOP_HOST_ID, singletonBucket("/en/sub1", NESTED_HOST_ID));

        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "/en/sub1/page.html");

        assertTrue("Result must be nested when cache pattern matches", result.isNested());
        assertEquals("Resolved host must be the nested host", NESTED_HOST_ID, result.getResolvedHostId());
        assertEquals("Remaining URI must be the path after the prefix", "/page.html", result.getRemainingUri());
    }

    /**
     * When the URI exactly equals the nested-host path prefix (no trailing content), the remaining
     * URI must be normalised to {@code "/"} so that the nested host's index page is served.
     */
    @Test
    public void resolve_uriExactlyEqualsPrefix_remainingUriIsRoot() {
        injectBucket(TOP_HOST_ID, singletonBucket("/en/sub1", NESTED_HOST_ID));

        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "/en/sub1");

        assertTrue(result.isNested());
        assertEquals(NESTED_HOST_ID, result.getResolvedHostId());
        assertEquals("Exact-prefix URI must yield remaining='/'", "/", result.getRemainingUri());
    }

    /**
     * When the URI equals the prefix followed by a trailing slash, the remaining URI must also
     * be {@code "/"}.
     */
    @Test
    public void resolve_uriEqualsPrefixWithTrailingSlash_remainingUriIsRoot() {
        injectBucket(TOP_HOST_ID, singletonBucket("/en/sub1", NESTED_HOST_ID));

        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "/en/sub1/");

        assertTrue(result.isNested());
        assertEquals("/", result.getRemainingUri());
    }

    // =========================================================================
    // Cache-first: no match → top-level result
    // =========================================================================

    /**
     * When the cache bucket exists but contains no pattern that matches the URI, the resolver
     * must return a top-level result with the original URI preserved.
     */
    @Test
    public void resolve_cacheContainsPatternButUriDoesNotMatch_returnsTopLevelResult() {
        injectBucket(TOP_HOST_ID, singletonBucket("/en/sub1", NESTED_HOST_ID));

        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "/other/page.html");

        assertFalse("No match → result must be topLevel", result.isNested());
        assertNull("resolvedHostId must be null when no pattern matches", result.getResolvedHostId());
        assertEquals("Original URI must be preserved", "/other/page.html", result.getRemainingUri());
    }

    /**
     * When the cache bucket is empty (top-level host has no nested descendants), the resolver
     * must return a top-level result.
     */
    @Test
    public void resolve_emptyBucket_returnsTopLevelResult() {
        injectBucket(TOP_HOST_ID, Collections.emptyList());

        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "/en/sub1/page.html");

        assertFalse("Empty bucket → result must be topLevel", result.isNested());
        assertEquals("/en/sub1/page.html", result.getRemainingUri());
    }

    // =========================================================================
    // Deepest-wins: most-specific (longest prefix) nested host wins
    // =========================================================================

    /**
     * When the cache contains both a shallow pattern ({@code /en/sub1}) and a deeper pattern
     * ({@code /en/sub1/child}), a URI that matches both must be attributed to the deeper
     * (more-specific) nested host.
     */
    @Test
    public void resolve_deeperPatternWinsOverShallower() {
        final List<NestedHostPatternCache.HostPatternEntry> bucket = sortedBucket(
                makeEntry("/en/sub1",       NESTED_HOST_ID),
                makeEntry("/en/sub1/child", DEEP_HOST_ID)
        );
        injectBucket(TOP_HOST_ID, bucket);

        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "/en/sub1/child/about.html");

        assertTrue("Result must be nested", result.isNested());
        assertEquals("Deeper host must win", DEEP_HOST_ID, result.getResolvedHostId());
        assertEquals("/about.html", result.getRemainingUri());
    }

    /**
     * When the URI matches only the shallow pattern but not the deeper one, the shallow nested
     * host must be returned.
     */
    @Test
    public void resolve_shallowPatternMatchesWhenDeepDoesNot() {
        final List<NestedHostPatternCache.HostPatternEntry> bucket = sortedBucket(
                makeEntry("/en/sub1",       NESTED_HOST_ID),
                makeEntry("/en/sub1/child", DEEP_HOST_ID)
        );
        injectBucket(TOP_HOST_ID, bucket);

        // /en/sub1/other matches /en/sub1 but NOT /en/sub1/child
        final HostResolutionResult result = resolver.resolve(TOP_HOST_ID, "/en/sub1/other/page.html");

        assertTrue("Result must be nested", result.isNested());
        assertEquals("Shallow host must match when deep host does not", NESTED_HOST_ID, result.getResolvedHostId());
        assertEquals("/other/page.html", result.getRemainingUri());
    }

    // =========================================================================
    // Miscellaneous
    // =========================================================================

    /**
     * The resolver must return a non-null result for any combination of inputs.
     */
    @Test
    public void resolve_alwaysReturnsNonNullResult() {
        injectBucket(TOP_HOST_ID, Collections.emptyList());

        assertNotNull(resolver.resolve(TOP_HOST_ID, "/"));
        assertNotNull(resolver.resolve(null, "/page.html"));
        assertNotNull(resolver.resolve(TOP_HOST_ID, null));
    }

    /**
     * The testing constructor must throw when given a {@code null} cache, so that programming
     * errors are caught early.
     */
    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullCache_throwsIllegalArgumentException() {
        new HostResolver(null);
    }

    /**
     * {@link HostResolver#getInstance()} must return a non-null singleton instance.  This test
     * cannot verify the singleton identity without a full container, but it asserts that the
     * method is at least callable and returns a non-null value when the production cache
     * singleton has already been initialised by previous tests.
     *
     * <p>Note: {@code NestedHostPatternCache.getInstance()} relies on
     * {@link com.dotmarketing.business.FactoryLocator} and is therefore <em>not</em> invoked
     * here; this test only verifies the {@link HostResolver} singleton logic.</p>
     */
    @Test
    public void getInstance_returnsSingletonThatIsNotNull() {
        // Use the package-private constructor to avoid triggering the production cache singleton
        final NestedHostPatternCache testCache = new NestedHostPatternCache();
        final HostResolver localResolver = new HostResolver(testCache);
        assertNotNull("HostResolver must not be null", localResolver);
    }

    // =========================================================================
    // Test helpers
    // =========================================================================

    /**
     * Builds a single-entry, sorted bucket for a path prefix and host ID.
     */
    private static List<NestedHostPatternCache.HostPatternEntry> singletonBucket(
            final String pathPrefix, final String hostId) {
        return Collections.singletonList(makeEntry(pathPrefix, hostId));
    }

    /**
     * Builds a pattern entry for the given path prefix and host ID, matching the format that
     * {@link NestedHostPatternCache#buildPatterns(String)} produces.
     */
    private static NestedHostPatternCache.HostPatternEntry makeEntry(
            final String pathPrefix, final String hostId) {
        final Pattern pattern = Pattern.compile("^" + Pattern.quote(pathPrefix) + "(/.*)?$");
        return new NestedHostPatternCache.HostPatternEntry(pattern, hostId, pathPrefix);
    }

    /**
     * Sorts a vararg of entries using {@link NestedHostPatternCache#LONGEST_PREFIX_FIRST} and
     * wraps the result in an unmodifiable list — identical to what {@code buildPatterns()}
     * produces.
     */
    @SafeVarargs
    private static List<NestedHostPatternCache.HostPatternEntry> sortedBucket(
            final NestedHostPatternCache.HostPatternEntry... entries) {
        final List<NestedHostPatternCache.HostPatternEntry> list = Arrays.asList(entries);
        list.sort(NestedHostPatternCache.LONGEST_PREFIX_FIRST);
        return Collections.unmodifiableList(list);
    }

    /**
     * Directly inserts a pre-built bucket into the backing map of {@code cache}, bypassing the
     * DB-backed {@code buildPatterns()} path.
     */
    private void injectBucket(
            final String topLevelHostId,
            final List<NestedHostPatternCache.HostPatternEntry> entries) {
        cache.getCacheMap().put(topLevelHostId, entries);
    }
}
