package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Stateless service that determines the effective dotCMS host for an incoming HTTP request by
 * applying a <em>cache-first</em>, two-step resolution strategy:
 *
 * <ol>
 *   <li><b>Step 1 – Pattern check (cache-first):</b> The {@link NestedHostPatternCache} is
 *       consulted first.  If the request URI matches a nested-host path prefix registered for
 *       the given top-level host, the nested host's UUID and the stripped URI are returned
 *       immediately — no additional database query is needed for this step.</li>
 *   <li><b>Step 2 – Fallback to top-level host:</b> When no pattern matches, a
 *       {@link HostResolutionResult} representing the unchanged top-level host is returned.</li>
 * </ol>
 *
 * <h3>Why cache-first matters</h3>
 * <p>Each HTTP request must resolve the effective host before any content or permission lookup
 * can proceed.  By checking the {@link NestedHostPatternCache} first — which is entirely
 * in-memory and rebuilt lazily from the database — the hot path for nested-host requests
 * incurs <em>zero</em> additional database round-trips.  Only the initial cache miss (first
 * request after a cache invalidation) triggers a database query to rebuild the pattern bucket.</p>
 *
 * <h3>Callers</h3>
 * <p>Callers are responsible for resolving the top-level host ID from the HTTP
 * {@code Host} header before invoking this resolver (typically via
 * {@link com.dotmarketing.portlets.contentlet.business.HostAPI#resolveHostName}).
 * This resolver only handles the path-segment mapping from the top-level host
 * down to a nested host — it does <em>not</em> perform DNS or alias lookups.</p>
 *
 * <h3>Thread safety</h3>
 * <p>All public methods are stateless (they carry no per-request mutable state) and are safe
 * to call from multiple threads concurrently.  The singleton instance is initialised with
 * double-checked locking and the underlying {@link NestedHostPatternCache} is itself
 * thread-safe.</p>
 *
 * @see NestedHostPatternCache
 * @see HostResolutionResult
 */
public class HostResolver {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static volatile HostResolver INSTANCE;

    /**
     * Returns the singleton {@code HostResolver}.  Instantiated lazily on first access using
     * the production {@link NestedHostPatternCache#getInstance()} singleton.
     *
     * @return the singleton {@code HostResolver}; never {@code null}
     */
    public static HostResolver getInstance() {
        if (INSTANCE == null) {
            synchronized (HostResolver.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HostResolver();
                }
            }
        }
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final NestedHostPatternCache patternCache;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Production constructor.  Uses the singleton {@link NestedHostPatternCache}.
     */
    public HostResolver() {
        this(NestedHostPatternCache.getInstance());
    }

    /**
     * Testing constructor.  Accepts a pre-populated or mock {@link NestedHostPatternCache} so
     * that unit tests can exercise the resolver without a live database connection.
     *
     * @param patternCache the cache to delegate pattern matching to; must not be {@code null}
     */
    HostResolver(final NestedHostPatternCache patternCache) {
        if (patternCache == null) {
            throw new IllegalArgumentException("patternCache must not be null");
        }
        this.patternCache = patternCache;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolves the effective host and URI for an incoming request using the cache-first strategy.
     *
     * <p>The resolution proceeds as follows:</p>
     * <ol>
     *   <li>Guard checks: if {@code topLevelHostId} or {@code requestUri} is blank, a
     *       {@link HostResolutionResult#topLevel(String)} result is returned immediately.</li>
     *   <li><b>Pattern check (cache-first):</b> {@link NestedHostPatternCache#resolve} is
     *       called with the supplied arguments.  The cache returns the most-specific matching
     *       nested-host entry (longest-prefix wins) if one exists.</li>
     *   <li>If a nested host was matched, the returned {@link HostResolutionResult} has
     *       {@link HostResolutionResult#isNested()} {@code == true}, the UUID of the matched
     *       nested host, and the URI with the nested-host path prefix stripped.</li>
     *   <li>If no nested host was matched, the returned result has
     *       {@link HostResolutionResult#isNested()} {@code == false} and the original
     *       {@code requestUri} unchanged.</li>
     * </ol>
     *
     * <p>This method performs <em>no</em> database access on a cache hit.  A cache miss (i.e.
     * the bucket for {@code topLevelHostId} has not yet been built or was recently invalidated)
     * causes {@link NestedHostPatternCache#getPatterns(String)} to rebuild the bucket from the
     * database before matching proceeds.  On all subsequent requests the bucket is served
     * entirely from memory.</p>
     *
     * @param topLevelHostId UUID of the top-level host resolved from the HTTP {@code Host}
     *                       header; a blank or {@code null} value causes an immediate
     *                       {@code topLevel} result to be returned
     * @param requestUri     the full request URI including the leading {@code /} (e.g.
     *                       {@code /en/sub1/page.html}); a blank or {@code null} value causes
     *                       an immediate {@code topLevel} result to be returned
     * @return a non-{@code null} {@link HostResolutionResult}; callers should check
     *         {@link HostResolutionResult#isNested()} to determine the outcome
     */
    public HostResolutionResult resolve(final String topLevelHostId, final String requestUri) {

        // Guard: blank top-level host ID — nothing to match against
        if (!UtilMethods.isSet(topLevelHostId)) {
            Logger.debug(HostResolver.class,
                    () -> "HostResolver.resolve: blank topLevelHostId — returning topLevel result");
            return HostResolutionResult.topLevel(requestUri);
        }

        // Guard: blank request URI — nothing to match
        if (!UtilMethods.isSet(requestUri)) {
            Logger.debug(HostResolver.class,
                    () -> "HostResolver.resolve: blank requestUri — returning topLevel result "
                            + "for topLevelHostId=" + topLevelHostId);
            return HostResolutionResult.topLevel(requestUri);
        }

        // Pattern check BEFORE any further (DB-backed) resolution.
        // The NestedHostPatternCache is entirely in-memory on a cache hit; a miss rebuilds
        // the bucket lazily from the database.
        final HostResolutionResult result = patternCache.resolve(topLevelHostId, requestUri);

        if (Logger.isDebugEnabled(HostResolver.class)) {
            if (result.isNested()) {
                Logger.debug(HostResolver.class,
                        "HostResolver.resolve: nested host matched"
                                + " topLevelHostId=" + topLevelHostId
                                + " requestUri=" + requestUri
                                + " resolvedHostId=" + result.getResolvedHostId()
                                + " remainingUri=" + result.getRemainingUri());
            } else {
                Logger.debug(HostResolver.class,
                        "HostResolver.resolve: no nested host matched"
                                + " topLevelHostId=" + topLevelHostId
                                + " requestUri=" + requestUri
                                + " — using top-level host");
            }
        }

        return result;
    }
}
