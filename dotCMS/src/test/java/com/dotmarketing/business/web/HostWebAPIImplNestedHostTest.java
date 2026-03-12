package com.dotmarketing.business.web;

import com.dotmarketing.portlets.contentlet.business.HostResolutionResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Documents and verifies the contract between {@link HostWebAPIImpl} and its collaborators
 * ({@link com.dotmarketing.portlets.contentlet.business.NestedHostPatternCache} and
 * {@link HostResolutionResult}) as used by the nested-host URL resolution pipeline.
 *
 * <p>These tests do <em>not</em> instantiate {@link HostWebAPIImpl} itself (which requires a
 * running dotCMS container) but instead verify the observable specification of the result objects
 * that drive the resolution logic inside
 * {@code HostWebAPIImpl.applyNestedHostResolution()}:</p>
 * <ol>
 *   <li>When {@link HostResolutionResult#isNested()} is {@code true} the caller sets
 *       {@link com.dotmarketing.filters.Constants#CMS_FILTER_URI_OVERRIDE} on the request and
 *       swaps the host to the nested one.</li>
 *   <li>When {@link HostResolutionResult#isNested()} is {@code false} the caller leaves the
 *       request and top-level host untouched.</li>
 * </ol>
 *
 * <p>Full end-to-end routing through CMSFilter is covered by integration tests.</p>
 */
public class HostWebAPIImplNestedHostTest {

    private static final String TOP_HOST_ID    = "top-level-uuid-1111";
    private static final String NESTED_HOST_ID = "nested-uuid-2222";

    // -------------------------------------------------------------------------
    // HostResolutionResult contract — shapes the conditional in applyNestedHostResolution()
    // -------------------------------------------------------------------------

    /**
     * A nested {@link HostResolutionResult} must advertise itself as nested, expose the matched
     * host UUID, and provide the remaining URI stripped of the path prefix.
     *
     * <p>This is exactly what {@code applyNestedHostResolution()} inspects before setting the
     * {@code CMS_FILTER_URI_OVERRIDE} attribute and returning the nested host.</p>
     */
    @Test
    public void nestedResult_isNested_true_exposesHostIdAndRemainingUri() {
        final HostResolutionResult result =
                HostResolutionResult.nested(NESTED_HOST_ID, "/about.html");

        assertTrue("isNested must be true",  result.isNested());
        assertEquals(NESTED_HOST_ID,         result.getResolvedHostId());
        assertEquals("/about.html",          result.getRemainingUri());
    }

    /**
     * A top-level {@link HostResolutionResult} must advertise itself as NOT nested, return a null
     * host ID, and preserve the original URI.
     *
     * <p>This is the guard condition that prevents {@code applyNestedHostResolution()} from
     * modifying the request when the URI belongs to the top-level host.</p>
     */
    @Test
    public void topLevelResult_isNested_false_preservesOriginalUri() {
        final HostResolutionResult result = HostResolutionResult.topLevel("/regular/page.html");

        assertFalse("isNested must be false", result.isNested());
        assertNull("resolvedHostId must be null", result.getResolvedHostId());
        assertEquals("/regular/page.html",   result.getRemainingUri());
    }

    /**
     * When the remaining URI would be {@code null} or empty (the URI exactly matched the prefix),
     * the result must normalise it to {@code "/"} so the nested host serves its index page.
     */
    @Test
    public void nestedResult_nullRemainingUri_normalisedToRootSlash() {
        final HostResolutionResult result = HostResolutionResult.nested(NESTED_HOST_ID, null);
        assertEquals("null remaining URI must be normalised to '/'", "/", result.getRemainingUri());
    }

    @Test
    public void nestedResult_emptyRemainingUri_normalisedToRootSlash() {
        final HostResolutionResult result = HostResolutionResult.nested(NESTED_HOST_ID, "");
        assertEquals("empty remaining URI must be normalised to '/'", "/", result.getRemainingUri());
    }

    /**
     * {@code applyNestedHostResolution()} must not proceed when either the host ID or the URI is
     * blank — guard clauses in the method body must short-circuit before reaching the cache.
     * This test verifies that a top-level result with a null URI is represented correctly.
     */
    @Test
    public void topLevelResult_nullUri_preservedAsNull() {
        final HostResolutionResult result = HostResolutionResult.topLevel(null);

        assertFalse(result.isNested());
        assertNull(result.getRemainingUri());
    }
}
