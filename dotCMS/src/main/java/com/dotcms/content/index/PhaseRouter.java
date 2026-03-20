package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.isMigrationComplete;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationNotStarted;
import static com.dotcms.content.index.IndexConfigHelper.isReadEnabled;

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
 * // Write boolean — AND of all providers
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
     * Delegates a read to the single current read provider.
     *
     * @param fn  must not throw checked exceptions; use {@link #readChecked} otherwise
     * @return    result from the read provider
     */
    public <R> R read(final Function<T, R> fn) {
        return fn.apply(readProvider());
    }

    /**
     * Fans a void write out to all current write providers.
     *
     * @param action must not throw checked exceptions; use {@link #writeChecked} otherwise
     */
    public void write(final Consumer<T> action) {
        for (final T impl : writeProviders()) {
            action.accept(impl);
        }
    }

    /**
     * Fans a boolean write out to all current write providers and returns the AND of
     * all results — {@code false} if any provider signals failure.
     *
     * @param fn must not throw checked exceptions
     */
    public boolean writeBoolean(final Function<T, Boolean> fn) {
        boolean result = true;
        for (final T impl : writeProviders()) {
            result &= fn.apply(impl);
        }
        return result;
    }

    /**
     * Fans a value-returning write to all current write providers and returns the result
     * from the <em>read provider</em>, keeping the returned value consistent with what the
     * caller would observe on a subsequent read.
     *
     * <p>In single-provider phases only one call is made (no overhead).</p>
     *
     * @param fn must not throw checked exceptions; use {@link #writeReturningChecked} otherwise
     */
    public <R> R writeReturning(final Function<T, R> fn) {
        if (isMigrationNotStarted()) {
            return fn.apply(esImpl);
        }
        if (isMigrationComplete()) {
            return fn.apply(osImpl);
        }
        // Dual-write: call both, return read-provider's result
        final R esResult = fn.apply(esImpl);
        final R osResult = fn.apply(osImpl);
        return isReadEnabled() ? osResult : esResult;
    }

    // -------------------------------------------------------------------------
    // Checked delegators — same semantics, allow lambdas with checked exceptions
    // -------------------------------------------------------------------------

    /**
     * Delegates a checked read to the single current read provider.
     *
     * @throws Exception any checked exception thrown by {@code fn}
     */
    public <R> R readChecked(final ThrowingFunction<T, R> fn) throws Exception {
        return fn.apply(readProvider());
    }

    /**
     * Fans a checked void write out to all current write providers.
     *
     * @throws Exception any checked exception thrown by {@code action}
     */
    public void writeChecked(final ThrowingConsumer<T> action) throws Exception {
        for (final T impl : writeProviders()) {
            action.accept(impl);
        }
    }

    /**
     * Fans a checked value-returning write to all current write providers and returns
     * the result from the read provider.
     *
     * @throws Exception any checked exception thrown by {@code fn}
     */
    public <R> R writeReturningChecked(final ThrowingFunction<T, R> fn) throws Exception {
        if (isMigrationNotStarted()) {
            return fn.apply(esImpl);
        }
        if (isMigrationComplete()) {
            return fn.apply(osImpl);
        }
        // Dual-write: call both, return read-provider's result
        final R esResult = fn.apply(esImpl);
        final R osResult = fn.apply(osImpl);
        return isReadEnabled() ? osResult : esResult;
    }
}