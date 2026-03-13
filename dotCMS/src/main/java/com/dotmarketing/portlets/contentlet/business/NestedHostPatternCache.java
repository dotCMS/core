package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * JVM-local cache that stores compiled URL-path patterns for all descendant (nested) hosts,
 * bucketed by their <strong>top-level host UUID</strong>.
 *
 * <h3>Purpose</h3>
 * <p>Provides fast path-prefix matching during HTTP request resolution without requiring a database
 * query per request.  Each bucket contains an ordered list of compiled {@link Pattern} entries for
 * every nested host that descends from the corresponding top-level host.  Entries are ordered
 * <em>longest-first</em> so that the most-specific pattern wins when multiple patterns could
 * match the same URI prefix.</p>
 *
 * <h3>Pattern format</h3>
 * <p>For a nested host with {@code parentPath = /en/} and {@code assetName = sub1} the compiled
 * pattern is:</p>
 * <pre>{@code ^/en/sub1(/.*)?$}</pre>
 *
 * <h3>Cache lifecycle</h3>
 * <ul>
 *   <li><em>Scope:</em> JVM-local — one instance per running dotCMS node.</li>
 *   <li><em>Population:</em> Lazy — the bucket for a top-level host is built on the first
 *       {@link #getPatterns(String)} call after an invalidation, using the
 *       {@code findDescendantHostIdentifiers} recursive CTE.</li>
 *   <li><em>Invalidation:</em> Triggered by {@code SAVE_SITE}, {@code UPDATE_SITE},
 *       {@code DELETE_SITE}, {@code PUBLISH_SITE}, {@code UN_PUBLISH_SITE}, and
 *       {@code ARCHIVE_SITE} events (all routed through
 *       {@link HostAPIImpl#flushAllCaches(com.dotmarketing.beans.Host)}).</li>
 *   <li><em>Cluster propagation:</em> PostgreSQL {@code LISTEN / NOTIFY} on the channel
 *       {@value #NOTIFY_CHANNEL}; each node rebuilds its bucket lazily after receiving
 *       the notification.</li>
 * </ul>
 *
 * <h3>Reparent invalidation</h3>
 * <p>When a host is reparented, a {@link HostReparentPayload} event is fired with both
 * {@code oldTopLevelHostId} and {@code newTopLevelHostId}.  Both buckets are invalidated by
 * calling {@link #invalidate(String)} twice.  When the host moves within the same tree both
 * fields are equal — the double-invalidation is harmless.</p>
 *
 * @see HostReparentPayload
 */
public class NestedHostPatternCache {

    /** PostgreSQL async-notification channel name used for cross-node cache invalidation. */
    static final String NOTIFY_CHANNEL = "nested_host_cache";

    /**
     * Special payload value broadcast when <em>all</em> buckets should be dropped (e.g. when the
     * top-level host for a nested host cannot be determined cheaply, such as during a host delete).
     */
    static final String INVALIDATE_ALL = "__all__";

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static volatile NestedHostPatternCache INSTANCE;

    /**
     * Returns the singleton instance.  Instantiated lazily on first access.
     *
     * @return the singleton {@code NestedHostPatternCache}
     */
    public static NestedHostPatternCache getInstance() {
        if (INSTANCE == null) {
            synchronized (NestedHostPatternCache.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NestedHostPatternCache();
                }
            }
        }
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * A single entry in the pattern list for a top-level host bucket.  Each entry represents one
     * nested (descendant) host and carries the compiled {@link Pattern}, the nested host's UUID,
     * and the literal path prefix that the pattern matches.
     */
    public static final class HostPatternEntry {

        /**
         * Compiled regex pattern for this nested host, e.g. {@code ^/en/sub1(/.*)?$}.
         */
        public final Pattern pattern;

        /**
         * UUID of the nested host that owns the path prefix represented by this entry.
         */
        public final String hostId;

        /**
         * Literal path prefix (without trailing slash) that the {@link #pattern} matches,
         * e.g. {@code /en/sub1}.  Used to strip the prefix from the incoming URI once a match is
         * found.
         */
        public final String pathPrefix;

        HostPatternEntry(final Pattern pattern, final String hostId, final String pathPrefix) {
            this.pattern    = pattern;
            this.hostId     = hostId;
            this.pathPrefix = pathPrefix;
        }

        @Override
        public String toString() {
            return "HostPatternEntry{hostId='" + hostId + "', pathPrefix='" + pathPrefix + "'}";
        }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Backing store: topLevelHostId &rarr; immutable, longest-first ordered list of entries.
     * A {@code null} value is never stored; a missing key means the bucket has not yet been
     * built (or was invalidated).
     */
    private final ConcurrentHashMap<String, List<HostPatternEntry>> cache =
            new ConcurrentHashMap<>();

    /**
     * Guards against starting the PostgreSQL {@code LISTEN} background thread more than once.
     */
    private final AtomicBoolean listenerStarted = new AtomicBoolean(false);

    /**
     * Package-private accessor for the raw backing map — <strong>visible for testing only</strong>.
     *
     * <p>Tests use this to inject pre-built buckets directly (bypassing the database) and to
     * assert on the cache state after invalidation calls.</p>
     *
     * @return the live backing {@link ConcurrentHashMap}; never {@code null}
     */
    ConcurrentHashMap<String, List<HostPatternEntry>> getCacheMap() {
        return cache;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the ordered (longest-first) list of {@link HostPatternEntry} objects for the given
     * top-level host.  If the bucket has not yet been built, or was previously invalidated, it is
     * populated synchronously from the database before being returned.
     *
     * @param topLevelHostId UUID of the top-level host whose bucket should be returned; must not be
     *                       {@code null}
     * @return immutable, never {@code null} list (may be empty if the top-level host has no nested
     *         descendants)
     */
    public List<HostPatternEntry> getPatterns(final String topLevelHostId) {
        return cache.computeIfAbsent(topLevelHostId, this::buildPatterns);
    }

    /**
     * Drops the cached bucket for the given top-level host.  The next call to
     * {@link #getPatterns(String)} for this host will rebuild the bucket from the database.
     *
     * <p>Also broadcasts a PostgreSQL {@code NOTIFY} so that all other JVM nodes in the cluster
     * drop their corresponding bucket.</p>
     *
     * @param topLevelHostId UUID of the top-level host whose bucket should be invalidated; a
     *                       {@code null} value is silently ignored
     */
    public void invalidate(final String topLevelHostId) {
        if (topLevelHostId == null) {
            return;
        }
        cache.remove(topLevelHostId);
        Logger.debug(this,
                "NestedHostPatternCache: invalidated bucket for top-level host " + topLevelHostId);
        broadcastInvalidation(topLevelHostId);
    }

    /**
     * Drops all cached buckets.  Intended for use when the top-level host for a changed host
     * cannot be determined cheaply (e.g. during a delete where the Identifier row is already gone),
     * or during a full cache flush.
     *
     * <p>Also broadcasts a PostgreSQL {@code NOTIFY} with the {@value #INVALIDATE_ALL} payload so
     * that all other JVM nodes in the cluster also drop all their buckets.</p>
     */
    public void invalidateAll() {
        cache.clear();
        Logger.debug(this, "NestedHostPatternCache: all buckets invalidated");
        broadcastInvalidation(INVALIDATE_ALL);
    }

    /**
     * Finds the most-specific nested host whose URL pattern matches the given full URI under the
     * specified top-level host.
     *
     * <p>Patterns are evaluated in <em>longest-prefix-first</em> order so that a deeper (more
     * specific) nested host always wins over a shallower ancestor when both patterns could match
     * the same URI prefix.  For example, given two nested hosts with prefixes {@code /en/sub1} and
     * {@code /en/sub1/child}, a request for {@code /en/sub1/child/page.html} matches
     * {@code /en/sub1/child} — not {@code /en/sub1}.</p>
     *
     * @param topLevelHostId UUID of the top-level host resolved from the incoming request's
     *                       server name; must not be {@code null}
     * @param fullUri        the full request URI including the leading {@code /}; must not be
     *                       {@code null}
     * @return an {@link Optional} containing the first matching {@link HostPatternEntry}, or
     *         {@link Optional#empty()} when no nested host matches (i.e. the URI belongs to the
     *         top-level host itself)
     */
    public Optional<HostPatternEntry> match(final String topLevelHostId, final String fullUri) {
        if (topLevelHostId == null || fullUri == null) {
            return Optional.empty();
        }
        for (final HostPatternEntry entry : getPatterns(topLevelHostId)) {
            if (entry.pattern.matcher(fullUri).matches()) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Convenience method that combines {@link #match(String, String)} and
     * {@link #stripPrefix(HostPatternEntry, String)} into a single call, returning a
     * {@link HostResolutionResult} that is ready for use by the URL-resolution pipeline.
     *
     * <p>When a nested host matches:</p>
     * <ul>
     *   <li>{@link HostResolutionResult#isNested()} returns {@code true}.</li>
     *   <li>{@link HostResolutionResult#getResolvedHostId()} returns the UUID of the matched
     *       nested host.</li>
     *   <li>{@link HostResolutionResult#getRemainingUri()} returns the URI after the
     *       nested-host path prefix has been stripped.</li>
     * </ul>
     *
     * <p>When no nested host matches, {@link HostResolutionResult#topLevel(String)} is returned
     * with the original {@code fullUri}.</p>
     *
     * @param topLevelHostId UUID of the top-level host resolved from the request's server name
     * @param fullUri        the full request URI (e.g. {@code /en/sub1/page.html})
     * @return a non-{@code null} {@link HostResolutionResult}; callers should check
     *         {@link HostResolutionResult#isNested()} to determine the outcome
     */
    public HostResolutionResult resolve(final String topLevelHostId, final String fullUri) {
        final Optional<HostPatternEntry> matched = match(topLevelHostId, fullUri);
        if (matched.isEmpty()) {
            return HostResolutionResult.topLevel(fullUri);
        }
        final HostPatternEntry entry = matched.get();
        final String remaining = stripPrefix(entry, fullUri);
        return HostResolutionResult.nested(entry.hostId, remaining);
    }

    /**
     * Derives the {@code remainingUri} by stripping the matched nested-host path prefix from
     * {@code fullUri}.
     *
     * <p>The {@link HostPatternEntry#pathPrefix} (e.g. {@code /en/sub1}) is removed from the
     * beginning of {@code fullUri}, yielding the host-relative URI that the nested host should
     * process.  If {@code fullUri} is exactly equal to the prefix (with or without a trailing
     * slash), the method returns {@code "/"} so the nested host's index page is served.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code fullUri = /en/sub1/page.html}, prefix {@code /en/sub1}
     *       &rarr; {@code /page.html}</li>
     *   <li>{@code fullUri = /en/sub1}, prefix {@code /en/sub1}
     *       &rarr; {@code /}</li>
     *   <li>{@code fullUri = /en/sub1/}, prefix {@code /en/sub1}
     *       &rarr; {@code /}</li>
     * </ul>
     *
     * @param entry   the matched {@link HostPatternEntry} obtained from
     *                {@link #match(String, String)}; must not be {@code null}
     * @param fullUri the original full request URI; must not be {@code null}
     * @return the remaining URI after stripping the nested-host prefix; never {@code null},
     *         never empty — falls back to {@code "/"} when nothing remains after stripping
     */
    public static String stripPrefix(final HostPatternEntry entry, final String fullUri) {
        final String prefix = entry.pathPrefix; // e.g. /en/sub1
        if (fullUri.length() <= prefix.length()) {
            // fullUri exactly equals the prefix (or shorter, defensive)
            return "/";
        }
        final String remaining = fullUri.substring(prefix.length());
        // remaining is either "/" or "/<path>"; never empty at this point because the
        // pattern requires (/.*)?  — but guard anyway
        return remaining.isEmpty() ? "/" : remaining;
    }

    // -------------------------------------------------------------------------
    // Lazy listener startup
    // -------------------------------------------------------------------------

    /**
     * Ensures the PostgreSQL {@code LISTEN} background thread has been started.  Called by
     * {@link #broadcastInvalidation(String)} on the first invalidation event so that the thread
     * is never started until the database is fully available.
     */
    void ensureListenerRunning() {
        if (!listenerStarted.compareAndSet(false, true)) {
            return;
        }
        final Thread listenerThread =
                new Thread(this::runListener, "NestedHostPatternCache-PgListener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        Logger.info(this, "NestedHostPatternCache: PostgreSQL LISTEN thread requested");
    }

    // -------------------------------------------------------------------------
    // Pattern construction
    // -------------------------------------------------------------------------

    /**
     * Builds the pattern list for one top-level host from the database.  Called by
     * {@link #getPatterns(String)} on a cache miss via {@link ConcurrentHashMap#computeIfAbsent}.
     *
     * @param topLevelHostId the top-level host UUID whose bucket should be populated
     * @return immutable list of entries ordered longest-first; never {@code null}
     */
    private List<HostPatternEntry> buildPatterns(final String topLevelHostId) {
        Logger.debug(this,
                "NestedHostPatternCache: building pattern bucket for top-level host "
                        + topLevelHostId);

        List<Identifier> descendants;
        try {
            descendants = FactoryLocator.getIdentifierFactory()
                    .findDescendantHostIdentifiers(topLevelHostId);
        } catch (final Exception e) {
            Logger.error(this,
                    "NestedHostPatternCache: could not load descendants for host "
                            + topLevelHostId + ": " + e.getMessage(), e);
            return Collections.emptyList();
        }

        final List<HostPatternEntry> entries = new ArrayList<>(descendants.size());
        for (final Identifier id : descendants) {
            final String pathPrefix = buildPathPrefix(id);
            if (pathPrefix == null) {
                Logger.debug(this,
                        "NestedHostPatternCache: skipping identifier with null/empty "
                                + "pathPrefix: " + id.getId());
                continue;
            }
            // Pattern: ^<pathPrefix>(/.*)?$  — prefix is quoted to prevent regex injection
            final Pattern p = Pattern.compile(
                    "^" + Pattern.quote(pathPrefix) + "(/.*)?$");
            entries.add(new HostPatternEntry(p, id.getId(), pathPrefix));
        }

        // Sort longest-first: the most-specific (deepest) pattern must be tried first.
        // When two prefixes have the same length, a stable secondary ordering (alphabetical)
        // ensures deterministic behaviour in tests and production alike.
        entries.sort(LONGEST_PREFIX_FIRST);

        Logger.debug(this,
                "NestedHostPatternCache: built " + entries.size()
                        + " pattern(s) for top-level host " + topLevelHostId);

        return Collections.unmodifiableList(entries);
    }

    /**
     * Constructs the literal path prefix for a descendant host {@link Identifier}.
     *
     * <p>Given {@code parentPath = /en/} and {@code assetName = sub1} the result is
     * {@code /en/sub1} (no trailing slash).</p>
     *
     * @param id the {@link Identifier} row for a nested host
     * @return the path prefix (without trailing slash), or {@code null} if either
     *         {@code parentPath} or {@code assetName} is absent
     */
    static String buildPathPrefix(final Identifier id) {
        final String parentPath = id.getParentPath();
        final String assetName  = id.getAssetName();
        if (parentPath == null || assetName == null || assetName.isEmpty()) {
            return null;
        }
        // parentPath comes as /en/ or /  — strip the trailing slash before concatenating
        final String stripped = parentPath.endsWith("/")
                ? parentPath.substring(0, parentPath.length() - 1)
                : parentPath;
        return stripped + "/" + assetName;
    }

    // -------------------------------------------------------------------------
    // PostgreSQL NOTIFY (broadcast)
    // -------------------------------------------------------------------------

    /**
     * Broadcasts a cache-invalidation notification to all cluster nodes via PostgreSQL
     * {@code NOTIFY}.
     *
     * <p>The notification is sent using a <em>fresh</em> JDBC connection obtained directly from
     * the data-source (not from the thread-local connection managed by HibernateUtil) so that
     * the {@code pg_notify} call is not bound to — or delayed by — any ongoing transaction on
     * the calling thread.</p>
     *
     * <p>Failures are logged as warnings but do not propagate: a missed broadcast will only cause
     * slightly stale patterns on remote nodes until their next on-demand rebuild.</p>
     *
     * @param payload the notification payload: a top-level host UUID or {@value #INVALIDATE_ALL}
     */
    private void broadcastInvalidation(final String payload) {
        ensureListenerRunning();
        try (final Connection conn = DbConnectionFactory.getDataSource().getConnection();
             final Statement  st   = conn.createStatement()) {
            // Use parameterised-style escaping: single quotes in payload are doubled
            st.execute("SELECT pg_notify('"
                    + NOTIFY_CHANNEL + "', '"
                    + payload.replace("'", "''") + "')");
        } catch (final Exception e) {
            // Non-fatal: remote nodes will rebuild lazily; local node was already invalidated above
            Logger.warn(this,
                    "NestedHostPatternCache: could not send NOTIFY for payload '" + payload
                            + "': " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // PostgreSQL LISTEN background thread
    // -------------------------------------------------------------------------

    /**
     * Blocking loop executed by the background daemon thread.  Maintains a dedicated raw JDBC
     * connection, registers a PostgreSQL {@code LISTEN} subscription on
     * {@value #NOTIFY_CHANNEL}, and processes incoming {@code NOTIFY} messages by calling
     * {@link #handleNotification(String)}.
     *
     * <p>If the connection is lost the loop waits {@value #RECONNECT_DELAY_MS} ms then
     * attempts to reconnect.  The thread exits cleanly when interrupted.</p>
     */
    private void runListener() {
        Logger.info(this,
                "NestedHostPatternCache: PostgreSQL LISTEN thread started on channel "
                        + NOTIFY_CHANNEL);
        while (!Thread.currentThread().isInterrupted()) {
            try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
                conn.setAutoCommit(true);
                try (final Statement st = conn.createStatement()) {
                    st.execute("LISTEN " + NOTIFY_CHANNEL);
                }
                Logger.debug(this,
                        "NestedHostPatternCache: LISTEN registered on channel " + NOTIFY_CHANNEL);

                final PGConnection pgConn = conn.unwrap(PGConnection.class);

                while (!Thread.currentThread().isInterrupted()) {
                    // Keep-alive ping; also causes the driver to flush pending notifications
                    try (final Statement keepAlive = conn.createStatement()) {
                        keepAlive.execute("SELECT 1");
                    }
                    // getNotifications(timeoutMs) blocks up to timeoutMs waiting for notifications
                    final PGNotification[] notifications = pgConn.getNotifications(POLL_TIMEOUT_MS);
                    if (notifications != null) {
                        for (final PGNotification n : notifications) {
                            handleNotification(n.getParameter());
                        }
                    }
                }
            } catch (final Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    // interrupted during getConnection() or another blocking call — exit cleanly
                    break;
                }
                Logger.warn(this,
                        "NestedHostPatternCache: LISTEN connection error; will reconnect in "
                                + RECONNECT_DELAY_MS + " ms: " + e.getMessage());
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Logger.info(this, "NestedHostPatternCache: PostgreSQL LISTEN thread stopped");
    }

    /**
     * Handles a single received {@code NOTIFY} payload by evicting the appropriate cache
     * bucket(s).
     *
     * @param payload the raw notification parameter; may be {@code null} for a bare {@code NOTIFY}
     */
    private void handleNotification(final String payload) {
        if (payload == null) {
            return;
        }
        if (INVALIDATE_ALL.equals(payload)) {
            cache.clear();
            Logger.debug(this, "NestedHostPatternCache: all buckets invalidated via NOTIFY");
        } else {
            cache.remove(payload);
            Logger.debug(this,
                    "NestedHostPatternCache: bucket invalidated via NOTIFY: " + payload);
        }
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * How long (in milliseconds) the {@code LISTEN} thread polls for notifications before sending
     * the next keep-alive ping.
     */
    private static final int POLL_TIMEOUT_MS = 30_000;

    /**
     * How long (in milliseconds) to wait before attempting to reconnect after a
     * {@code LISTEN} connection error.
     */
    private static final int RECONNECT_DELAY_MS = 5_000;

    /**
     * Comparator that orders {@link HostPatternEntry} instances by specificity:
     * <ol>
     *   <li><b>Primary key — descending prefix length:</b> a longer
     *       {@link HostPatternEntry#pathPrefix} means a deeper, more-specific nested host.
     *       For example, {@code /en/sub1/child} (15 chars) sorts before {@code /en/sub1}
     *       (8 chars), ensuring a request for {@code /en/sub1/child/page.html} is attributed
     *       to the {@code child} host rather than its parent {@code sub1}.</li>
     *   <li><b>Secondary key — ascending alphabetical order:</b> a stable tie-breaker applied
     *       when two prefixes have identical lengths.  Two sibling hosts at the same depth
     *       cannot both match the same URI (their prefixes differ), so the secondary key only
     *       affects test determinism and has no impact on correctness in production.</li>
     * </ol>
     */
    static final Comparator<HostPatternEntry> LONGEST_PREFIX_FIRST = (a, b) -> {
        final int lengthDiff = b.pathPrefix.length() - a.pathPrefix.length();
        return lengthDiff != 0 ? lengthDiff : a.pathPrefix.compareTo(b.pathPrefix);
    };
}
