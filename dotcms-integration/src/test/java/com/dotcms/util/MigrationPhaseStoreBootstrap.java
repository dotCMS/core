package com.dotcms.util;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

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
}
