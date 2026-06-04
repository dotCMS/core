package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.isMigrationComplete;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationNotStarted;
import static com.dotcms.content.index.IndexConfigHelper.isReadEnabled;

import static com.dotcms.content.index.IndexConfigHelper.logShadowWriteFailure;

import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Phase-aware, generic two-provider router for the ES → OS migration.
 *
 * <h2>Routing table</h2>
 * <pre>
 * Phase                     | Read provider | Write providers
 * --------------------------|---------------|-----------------
 * 0 — not started           | ES            | [ES]
 * 1 — dual-write, ES reads  | ES            | [ES, OS]
 * 2 — dual-write, OS reads  | OS            | [ES, OS]
 * 3 — OS only               | OS            | [OS]
 * </pre>
 *
 * <h2>Error handling</h2>
 *
 * <h3>Writes (dual-write phases 1 and 2)</h3>
 * <p>All write providers are <em>always</em> called — a failure in one provider never skips
 * the other. The handling is asymmetric:</p>
 * <ul>
 *   <li><strong>Primary provider failure</strong> — exception is <em>re-thrown</em> after
 *       the shadow has been called, so callers observe the failure.</li>
 *   <li><strong>Shadow provider failure</strong> (OS in phases 1 and 2) — exception is
 *       <em>swallowed</em> and logged at {@code WARN}. The shadow index may drift, but the
 *       business operation and the primary index are unaffected.</li>
 * </ul>
 *
 * <h3>Reads (Phase 2 only — OS reads with ES still active)</h3>
 * <p>{@link #read} and {@link #readChecked} apply an automatic <strong>ES fallback</strong>
 * in Phase 2: if OS throws an exception the error is logged at {@code ERROR} and the read
 * is retried against ES. This keeps reads correct even if the OS shadow index is temporarily
 * unavailable or inconsistent. In Phase 3 there is no fallback — ES is decommissioned.</p>
 *
 * <h2>Typical usage in a router class</h2>
 * <pre>{@code
 * private final PhaseRouter<IndexAPI> router =
 *         new PhaseRouter<>(esImpl, osImpl);
 *
 * // Read — single provider
 * @Override public boolean indexExists(String name) {
 *     return router.read(impl -> impl.indexExists(name));
 * }
 *
 * // Write void — fan-out
 * @Override public void closeIndex(String name) {
 *     router.write(impl -> impl.closeIndex(name));
 * }
 *
 * // Write boolean — primary result; shadow result is ignored
 * @Override public boolean delete(String name) {
 *     return router.writeBoolean(impl -> impl.delete(name));
 * }
 *
 * // Write with return value — call all, return read-provider result
 * @Override public CreateIndexStatus createIndex(String name, int shards) throws IOException {
 *     return router.writeReturning(impl -> impl.createIndex(name, shards));
 * }
 *
 * // Checked read
 * @Override public Status getIndexStatus(String name) throws DotDataException {
 *     return router.readChecked(impl -> impl.getIndexStatus(name));
 * }
 *
 * // Checked write void
 * @Override public void clearIndex(String name) throws IOException, DotDataException {
 *     router.writeChecked(impl -> impl.clearIndex(name));
 * }
 * }</pre>
 *
 * <h2>Exceptions that do NOT fit the uniform pattern</h2>
 * <p>Some methods require custom logic outside this router:</p>
 * <ul>
 *   <li><strong>Aggregating reads</strong> ({@code listIndices}, {@code getClosedIndexes},
 *       {@code getClusterHealth}) — in dual-write phases results from both providers must be
 *       <em>merged</em>, not just selected. Implement inline in the router class using
 *       {@link #readProvider()} and {@link #writeProviders()} directly.</li>
 *   <li><strong>Flush-and-return</strong> ({@code flushCaches}) — writes to all providers
 *       but returns the read-provider's result. Use
 *       {@link #writeReturning(ThrowingFunction)} after fan-out.</li>
 * </ul>
 *
 * @param <T> the provider interface type (e.g. {@code IndexAPI})
 * @author Fabrizio Araya
 */
public final class PhaseRouter<T> {

    // -------------------------------------------------------------------------
    // Checked functional interfaces (lambdas that can throw checked exceptions)
    // -------------------------------------------------------------------------

    /** Like {@link Function} but allows checked exceptions. */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /** Like {@link Consumer} but allows checked exceptions. */
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final T esImpl;
    private final T osImpl;

    public PhaseRouter(final T esImpl, final T osImpl) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
    }

    public T esImpl() {
        return esImpl;
    }

    public T osImpl() {
        return osImpl;
    }

    // -------------------------------------------------------------------------
    // Phase helpers — public so that callers can build aggregation logic
    // -------------------------------------------------------------------------

    /**
     * The single provider that serves READ operations in the current phase.
     * Phases 0/1 → ES; phases 2/3 → OS.
     */
    public T readProvider() {
        return isReadEnabled() ? osImpl : esImpl;
    }

    /**
     * Returns {@code true} when the system is in Phase 2 (OS reads, ES still active).
     *
     * <p>This is the only phase where a read fallback to ES is both meaningful and safe:
     * OS is the preferred read source, but ES has not yet been decommissioned.</p>
     */
    private boolean isPhase2() {
        return isReadEnabled() && !isMigrationComplete();
    }

    /**
     * All providers that should receive WRITE operations in the current phase.
     * Phase 0 → [ES]; phases 1/2 → [ES, OS]; phase 3 → [OS].
     */
    public List<T> writeProviders() {
        if (isMigrationNotStarted()) {
            return List.of(esImpl);
        }
        if (isMigrationComplete()) {
            return List.of(osImpl);
        }
        return List.of(esImpl, osImpl);
    }

    // -------------------------------------------------------------------------
    // Standard (unchecked) delegators
    // -------------------------------------------------------------------------

    /**
     * Delegates a read to the current read provider with automatic ES fallback in Phase 2.
     *
     * <p><strong>Phase 2 only</strong>: OS is the read provider but ES is still active.
     * If OS throws a runtime exception the error is logged at {@code ERROR} level and the
     * read is retried against ES, so a transient OS failure never surfaces to the caller.
     * In all other phases the call is forwarded to the read provider without a safety net:
     * Phase 0/1 read from ES (no fallback needed); Phase 3 reads from OS (ES decommissioned).</p>
     *
     * @param fn  must not throw checked exceptions; use {@link #readChecked} otherwise
     * @return    result from the read provider (or ES fallback in Phase 2)
     */
    public <R> R read(final Function<T, R> fn) {
        if (!isPhase2()) {
            return fn.apply(readProvider());
        }
        try {
            return fn.apply(osImpl);
        } catch (final RuntimeException e) {
            Logger.error(PhaseRouter.class,
                    "OS read failed in Phase 2 — falling back to ES. "
                    + "OS index may be stale or unavailable. Cause: " + e.getMessage(), e);
            return fn.apply(esImpl);
        }
    }

    /**
     * Fans a void write out to all current write providers.
     *
     * <p>In dual-write phases all providers are always called regardless of failures.
     * Primary provider failures are re-thrown; shadow failures are logged and swallowed.
     * Delegates to {@link #writeChecked} — {@link Consumer} lambdas cannot throw checked
     * exceptions so the delegation is always safe.</p>
     *
     * @param action must not throw checked exceptions; use {@link #writeChecked} otherwise
     */
    public void write(final Consumer<T> action) {
        try {
            writeChecked(action::accept);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e); // unreachable: Consumer<T> cannot throw checked exceptions
        }
    }

    /**
     * Fans a boolean write out to all current write providers and returns the primary
     * provider's result.
     *
     * <p>In dual-write phases all providers are always called regardless of failures.
     * Shadow failures are logged and swallowed; the return value reflects only the
     * primary provider's result. Primary failures are re-thrown.</p>
     *
     * @param fn must not throw checked exceptions
     */
    public boolean writeBoolean(final Function<T, Boolean> fn) {
        final List<T> providers = writeProviders();
        if (providers.size() == 1) {
            return fn.apply(providers.get(0));
        }
        // Dual-write: call every provider; only primary result is returned
        final T primary = readProvider();
        boolean primaryResult = false; // safe default: assume failure until primary confirms success
        RuntimeException primaryEx = null;
        for (final T impl : providers) {
            try {
                final boolean result = fn.apply(impl);
                if (impl == primary) {
                    primaryResult = result;
                }
                // shadow boolean result is discarded — shadow is fire-and-forget
            } catch (RuntimeException e) {
                if (impl == primary) {
                    primaryEx = e;
                } else {
                    logShadowWriteFailure(PhaseRouter.class,
                            "Shadow write failed (fire-and-forget in dual-write phase): "
                            + e.getMessage(), e);
                }
            }
        }
        if (primaryEx != null) {
            throw primaryEx;
        }
        return primaryResult;
    }

    /**
     * Fans a value-returning write to all current write providers and returns the result
     * from the <em>read provider</em>, keeping the returned value consistent with what the
     * caller would observe on a subsequent read.
     *
     * <p>In dual-write phases all providers are always called regardless of failures.
     * Shadow failures are logged and swallowed; primary failures are re-thrown.
     * Delegates to {@link #writeReturningChecked} — {@link Function} lambdas cannot throw
     * checked exceptions so the delegation is always safe.</p>
     *
     * @param fn must not throw checked exceptions; use {@link #writeReturningChecked} otherwise
     */
    public <R> R writeReturning(final Function<T, R> fn) {
        try {
            return writeReturningChecked(fn::apply);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e); // unreachable: Function<T,R> cannot throw checked exceptions
        }
    }

    // -------------------------------------------------------------------------
    // Checked delegators — same semantics, allow lambdas with checked exceptions
    // -------------------------------------------------------------------------

    /**
     * Delegates a checked read to the current read provider with automatic ES fallback in Phase 2.
     *
     * <p>Same fallback semantics as {@link #read}: in Phase 2 an OS exception is caught,
     * logged at {@code ERROR}, and the read is retried against ES.
     * In Phase 0/1 reads from ES; in Phase 3 reads from OS — no fallback in either case.</p>
     *
     * @throws Exception the exception thrown by the fallback provider (ES), if both fail
     */
    public <R> R readChecked(final ThrowingFunction<T, R> fn) throws Exception {
        if (!isPhase2()) {
            return fn.apply(readProvider());
        }
        try {
            return fn.apply(osImpl);
        } catch (final Exception e) {
            Logger.error(PhaseRouter.class,
                    "OS read failed in Phase 2 — falling back to ES. "
                    + "OS index may be stale or unavailable. Cause: " + e.getMessage(), e);
            return fn.apply(esImpl);
        }
    }

    /**
     * Fans a checked void write out to all current write providers.
     *
     * <p>In dual-write phases all providers are always called regardless of failures.
     * Primary provider failures are re-thrown after the shadow has been called;
     * shadow failures are logged and swallowed.</p>
     *
     * @throws Exception the checked exception thrown by the primary provider, if any
     */
    public void writeChecked(final ThrowingConsumer<T> action) throws Exception {
        final List<T> providers = writeProviders();
        if (providers.size() == 1) {
            action.accept(providers.get(0));
            return;
        }
        // Dual-write: call every provider; shadow failures are fire-and-forget
        final T primary = readProvider();
        Exception primaryEx = null;
        for (final T impl : providers) {
            try {
                action.accept(impl);
            } catch (Exception e) {
                if (impl == primary) {
                    primaryEx = e;  // record — shadow must still be called
                } else {
                    logShadowWriteFailure(PhaseRouter.class,
                            "Shadow write failed (fire-and-forget in dual-write phase): "
                            + e.getMessage(), e);
                }
            }
        }
        if (primaryEx != null) {
            throw primaryEx;
        }
    }

    /**
     * Fans a checked value-returning write to all current write providers and returns
     * the result from the read provider.
     *
     * <p>In dual-write phases all providers are always called regardless of failures.
     * Shadow failures are logged and swallowed; primary failures are re-thrown.</p>
     *
     * @throws Exception the checked exception thrown by the primary provider, if any
     */
    public <R> R writeReturningChecked(final ThrowingFunction<T, R> fn) throws Exception {
        if (isMigrationNotStarted()) {
            return fn.apply(esImpl);
        }
        if (isMigrationComplete()) {
            return fn.apply(osImpl);
        }
        // Dual-write: call both, return read-provider's result; shadow failures are fire-and-forget
        final T primary = readProvider();
        final T shadow  = primary == esImpl ? osImpl : esImpl;
        R primaryResult = null;
        Exception primaryEx = null;
        try {
            primaryResult = fn.apply(primary);
        } catch (Exception e) {
            primaryEx = e;
        }
        try {
            fn.apply(shadow);
        } catch (Exception e) {
            logShadowWriteFailure(PhaseRouter.class,
                    "Shadow write failed (fire-and-forget in dual-write phase): "
                    + e.getMessage(), e);
        }
        if (primaryEx != null) {
            throw primaryEx;
        }
        return primaryResult;
    }
}
