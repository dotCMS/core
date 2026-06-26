package com.dotcms.util;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.content.index.opensearch.OSSearchAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.Optional;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.indices.RefreshRequest;

/**
 * Phase-aware bootstrap for the search store(s) an integration test needs.
 *
 * <p>Integration tests do not go through the production startup path
 * ({@code InitServlet.init() -> ContentletIndexAPI.checkAndInitializeIndex() ->
 * IndexStartupValidator}), so an OpenSearch index set is never created — and an
 * unreachable OS service is never detected — unless a test bootstraps it explicitly. This
 * helper centralizes that bootstrap so any test can create / publish content into OpenSearch
 * exactly as it does into ElasticSearch, governed by the active {@link MigrationPhase}.</p>
 *
 * <h2>Fail-fast on a missing OpenSearch service</h2>
 * <p>For every phase that involves OpenSearch (phases 1, 2 and 3) this helper validates OS
 * reachability <strong>before</strong> creating the index set and throws a
 * {@link DotRuntimeException} with a clear message if the cluster does not answer. This is
 * intentionally stricter than production: production is lenient in phases 1–2 (it logs and falls
 * back to ES-only via {@code haltMigration()}), but a migration test that silently degraded to
 * ES would mask exactly the problem it exists to catch. Phase 0 (ES only) never touches OS.</p>
 *
 * <p>Reachability is checked with a plain {@code client.info()} call rather than the full
 * {@code IndexStartupValidator.validate()} on purpose: the latter also asserts endpoint
 * separation, which fails by design in the single-cluster {@code opensearch-upgrade} profile
 * where the ES and OS clients point at the same endpoint.</p>
 *
 * @author Fabrizzio Araya
 */
public final class MigrationPhaseStoreBootstrap {

    private MigrationPhaseStoreBootstrap() {
    }

    /**
     * Ensures the search store(s) required by the <em>currently active</em> migration phase exist
     * and, when the phase involves OpenSearch, that the OS service is reachable — failing hard
     * otherwise. After this returns, content created/published under the active phase lands in the
     * expected store(s).
     *
     * <p>Set the desired phase (e.g. {@code Config.setProperty(MigrationPhase.FLAG_KEY, ...)} or a
     * test {@code setPhase(...)} helper) before calling. Bootstrapping under
     * {@link MigrationPhase#PHASE_1_DUAL_WRITE_ES_READS} creates both the ES and the OS index sets,
     * which is the usual choice for a test that exercises both stores.</p>
     *
     * @throws DotRuntimeException if the active phase requires OpenSearch but the OS service is
     *                             unreachable, or if the index set cannot be initialized
     */
    public static void ensureStoresForCurrentPhase() {
        final MigrationPhase phase = MigrationPhase.current();

        if (!phase.isMigrationNotStarted()) {
            assertOpenSearchReachable(phase);
        }

        try {
            APILocator.getContentletIndexAPI().checkAndInitializeIndex();
        } catch (final Exception e) {
            throw new DotRuntimeException(
                    "Failed to initialize the search index set for migration phase " + phase
                            + ": " + e.getMessage(), e);
        }
        APILocator.getVersionedIndicesAPI().clearCache();

        Logger.info(MigrationPhaseStoreBootstrap.class,
                "Search stores bootstrapped for migration phase " + phase);
    }

    /**
     * Asserts that the OpenSearch service backing the given {@code phase} is reachable, throwing a
     * {@link DotRuntimeException} with an actionable message otherwise. A no-op for phases that do
     * not involve OpenSearch ({@link MigrationPhase#PHASE_0_MIGRATION_NOT_STARTED}).
     *
     * <p>Intended for the content create/publish path so a test that targets OpenSearch fails
     * immediately and clearly when the OS container is down, instead of much later with an opaque
     * "index not found" error.</p>
     *
     * @param phase the migration phase whose OpenSearch backend must be available
     * @throws DotRuntimeException if {@code phase} involves OpenSearch and the cluster is unreachable
     */
    public static void assertOpenSearchReachable(final MigrationPhase phase) {
        if (phase == null || phase.isMigrationNotStarted()) {
            return;
        }
        try {
            final OSClientProvider provider = CDIUtils.getBeanThrows(OSClientProvider.class);
            final String version = provider.getClient().info().version().number();
            Logger.debug(MigrationPhaseStoreBootstrap.class,
                    "OpenSearch reachable for migration phase " + phase + " — version " + version);
        } catch (final Exception e) {
            throw new DotRuntimeException(
                    "OpenSearch service is required for migration phase " + phase
                            + " but is not reachable: " + e.getMessage()
                            + ". Verify OS_ENDPOINTS and that the OpenSearch container is running.",
                    e);
        }
    }

    /**
     * Creates and registers a fresh OpenSearch content index set (working + live) <em>directly</em>,
     * returning the physical working-index name. The new index set becomes the default
     * {@link com.dotcms.content.index.VersionedIndices}, so the high-level read/write path resolves
     * it once a phase that routes to OpenSearch is active (e.g. {@code PHASE_3_OPENSEARCH_ONLY}).
     *
     * <p>This deliberately does <strong>not</strong> go through
     * {@link com.dotcms.content.elasticsearch.business.ContentletIndexAPI#checkAndInitializeIndex()}:
     * that path runs {@code IndexStartupValidator}, whose endpoint-separation check fails by design in
     * the single-cluster {@code opensearch-upgrade} profile (ES and OS share an endpoint). It mirrors
     * what the router's bootstrap does for the OS provider — create the physical content index (with
     * the content mapping) and register the names — minus the validator, so a test can bootstrap OS in
     * a single-cluster environment.</p>
     *
     * <p>Index names use the physical form ({@code toPhysicalName}) exactly as the router passes them
     * to the OS provider, so creation and registration stay symmetric (no {@code .os}-tag mismatch).</p>
     *
     * @return the physical (cluster-prefixed, {@code .os}-tagged) working-index name
     * @throws DotRuntimeException if the OS index set cannot be created or registered
     */
    public static String bootstrapOpenSearchStoreDirect() {
        final ContentletIndexOperationsOS opsOS =
                CDIUtils.getBeanThrows(ContentletIndexOperationsOS.class);

        final long ts = System.currentTimeMillis();
        final String physWorking = opsOS.toPhysicalName("working_" + ts);
        final String physLive     = opsOS.toPhysicalName("live_" + ts);

        try {
            opsOS.createContentIndex(physWorking, 1);
            opsOS.createContentIndex(physLive, 1);
        } catch (final Exception e) {
            throw new DotRuntimeException(
                    "Failed to create the OpenSearch content index set (working=" + physWorking
                            + ", live=" + physLive + "): " + e.getMessage(), e);
        }

        try {
            APILocator.getVersionedIndicesAPI().saveIndices(
                    VersionedIndicesImpl.builder().working(physWorking).live(physLive).build());
        } catch (final Exception e) {
            throw new DotRuntimeException(
                    "Failed to register the OpenSearch index set: " + e.getMessage(), e);
        }
        APILocator.getVersionedIndicesAPI().clearCache();

        Logger.info(MigrationPhaseStoreBootstrap.class,
                "OpenSearch content index set bootstrapped directly — working: " + physWorking);
        return physWorking;
    }

    /**
     * Opens a self-contained OpenSearch test store under {@code phase}, in one call:
     * <ol>
     *   <li>asserts OpenSearch is reachable (fail-fast),</li>
     *   <li>captures the prior migration phase and default {@link VersionedIndices} so they can be
     *       restored,</li>
     *   <li>bootstraps a fresh OS content index set ({@link #bootstrapOpenSearchStoreDirect()}),</li>
     *   <li>switches the active phase to {@code phase}.</li>
     * </ol>
     *
     * <p>Use it in {@code @Before} and {@link OpenSearchPhaseStore#close() close()} in {@code @After}
     * (or with try-with-resources). The returned handle also carries the placement-verification
     * helpers, so a test needs no per-test boilerplate or injected OpenSearch client:</p>
     *
     * <pre>
     *   store = MigrationPhaseStoreBootstrap.openForPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
     *   ...
     *   assertTrue(store.isIndexed(contentlet));
     *   ...
     *   store.close();   // restores phase + VersionedIndices
     * </pre>
     *
     * @param phase the migration phase to run under (must involve OpenSearch, i.e. not phase 0)
     * @return a live {@link OpenSearchPhaseStore} handle
     * @throws DotRuntimeException if OpenSearch is unreachable or the index set can't be bootstrapped
     */
    public static OpenSearchPhaseStore openForPhase(final MigrationPhase phase) {
        assertOpenSearchReachable(phase);

        final String priorPhase = Config.getStringProperty(MigrationPhase.FLAG_KEY, null);
        final Optional<VersionedIndices> priorIndices =
                Try.of(() -> APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices())
                   .getOrElse(Optional.empty());

        final String workingIndex = bootstrapOpenSearchStoreDirect();

        Config.setProperty(MigrationPhase.FLAG_KEY, String.valueOf(phase.ordinal()));

        return new OpenSearchPhaseStore(workingIndex, priorPhase, priorIndices);
    }

    /**
     * Live handle to an OpenSearch index set bootstrapped by {@link #openForPhase(MigrationPhase)}.
     * Bundles the working-index name, the document-placement checks, and the state restore so tests
     * stay free of bootstrap/teardown/verification boilerplate. Resolves the OpenSearch client
     * itself (via CDI), so the test does not need to inject one.
     */
    public static final class OpenSearchPhaseStore implements AutoCloseable {

        /** Poll budget for asynchronously-applied writes to become visible. */
        private static final int  AWAIT_ATTEMPTS = 50;
        private static final long AWAIT_SLEEP_MS = 100L;

        private final String workingIndex;
        private final String priorPhase;
        private final Optional<VersionedIndices> priorIndices;

        private OpenSearchPhaseStore(final String workingIndex, final String priorPhase,
                final Optional<VersionedIndices> priorIndices) {
            this.workingIndex = workingIndex;
            this.priorPhase   = priorPhase;
            this.priorIndices = priorIndices;
        }

        /** The physical (cluster-prefixed, {@code .os}-tagged) working-index name in use. */
        public String workingIndex() {
            return workingIndex;
        }

        /** {@code true} when the OS working index holds a document with the given {@code _id}. */
        public boolean osHasDoc(final String docId) {
            return Try.of(() -> client().count(CountRequest.of(
                            r -> r.index(workingIndex).query(q -> q.ids(i -> i.values(docId)))))
                            .count() > 0L)
                      .getOrElse(false);
        }

        /** Refreshes the cluster, then polls until the doc is visible or the budget is exhausted. */
        public boolean awaitDoc(final String docId) {
            for (int i = 0; i < AWAIT_ATTEMPTS; i++) {
                refresh();
                if (osHasDoc(docId)) {
                    return true;
                }
                if (!sleepBetweenAttempts()) {
                    return false;
                }
            }
            return osHasDoc(docId);
        }

        /**
         * Sleeps {@link #AWAIT_SLEEP_MS} between poll attempts. Returns {@code true} to keep polling,
         * or {@code false} if the thread was interrupted — in which case the interrupt flag is
         * restored so callers up the stack can react.
         */
        private boolean sleepBetweenAttempts() {
            try {
                Thread.sleep(AWAIT_SLEEP_MS);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        /** Convenience: {@code true} when the given contentlet's version document is in the OS index. */
        public boolean isIndexed(final Contentlet contentlet) {
            return awaitDoc(docId(contentlet));
        }

        /**
         * {@code true} when a query for {@code identifier}, executed through the OpenSearch-specific
         * search API ({@link OSSearchAPIImpl} — which always talks to OpenSearch, <strong>never</strong>
         * the phase router), returns at least one hit against the working index.
         *
         * <p>Complements the high-level {@code ContentletAPI.search} check: that one is phase-routed
         * (it could read ES or OS), whereas this proves the document is retrievable specifically
         * through the OpenSearch read path.</p>
         */
        public boolean osSearchFindsByIdentifier(final String identifier) {
            final String jsonQuery = "{\"query\":{\"term\":{\"identifier\":\"" + identifier + "\"}}}";
            final OSSearchAPIImpl osSearch = CDIUtils.getBeanThrows(OSSearchAPIImpl.class);
            refresh();
            return Try.of(() -> osSearch
                            .searchRaw(jsonQuery, false, APILocator.systemUser(), false)
                            .hits().getTotalHits().value() > 0L)
                      .getOrElse(false);
        }

        /** Refresh the whole cluster so freshly-written docs are searchable. */
        public void refresh() {
            Try.run(() -> client().indices().refresh(RefreshRequest.of(r -> r)))
               .onFailure(e -> Logger.warn(OpenSearchPhaseStore.class,
                       "refresh failed: " + e.getMessage()));
        }

        /** Restores the migration phase and default {@link VersionedIndices} captured at open time. */
        @Override
        public void close() {
            Config.setProperty(MigrationPhase.FLAG_KEY, priorPhase);
            priorIndices.ifPresent(v ->
                    Try.run(() -> APILocator.getVersionedIndicesAPI().saveIndices(v))
                       .onFailure(e -> Logger.warn(OpenSearchPhaseStore.class,
                               "close: restore OS versioned indices failed: " + e.getMessage())));
            APILocator.getVersionedIndicesAPI().clearCache();
        }

        private static OpenSearchClient client() {
            return CDIUtils.getBeanThrows(OSClientProvider.class).getClient();
        }

        /** The ES/OS document {@code _id} dotCMS assigns to an indexed contentlet version. */
        private static String docId(final Contentlet contentlet) {
            return contentlet.getIdentifier() + "_" + contentlet.getLanguageId()
                    + "_" + contentlet.getVariantId();
        }
    }
}
