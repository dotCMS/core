package com.dotcms.content.index.migration;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Composes the ES→OS migration-readiness report (issue #36360): current phase + the per-index mirror
 * status of both mirrored index families (content and Site Search) + an overall go/no-go verdict for
 * changing the phase. Read-only and stateless — the verdict is derived from live index state at
 * request time, nothing is persisted (see {@link MigrationReadiness}).
 *
 * <h4>Verdict semantics</h4>
 * <ul>
 *   <li><b>Advance</b> (toward OpenSearch-only): meaningful in the dual-write phases (1/2), where it
 *       is safe only when no index needs attention. In Phase 0 there is nothing to reconcile yet
 *       (counterparts are built during dual-write) and in Phase 3 there is no further phase — both report
 *       safe with an explanatory summary.</li>
 *   <li><b>Rollback</b> (downgrade): a downgrade ultimately routes reads back to Elasticsearch
 *       (Phases 0/1), so it is unsafe when any index's ES copy is behind its OpenSearch counterpart — that
 *       delta (typically content written while OpenSearch served reads) would be silently missing
 *       until a full reindex. Derived from the same live counts, so no historical state is needed.</li>
 * </ul>
 */
public class MigrationReadinessService {

    private final SiteSearchMirrorReconciler siteSearchReconciler;
    private final ContentIndexMirrorReconciler contentReconciler;
    private final Supplier<String> clusterIdSupplier;

    public MigrationReadinessService() {
        this(new SiteSearchMirrorReconciler(), new ContentIndexMirrorReconciler(),
                ClusterFactory::getClusterId);
    }

    @VisibleForTesting
    MigrationReadinessService(final SiteSearchMirrorReconciler siteSearchReconciler,
            final ContentIndexMirrorReconciler contentReconciler,
            final Supplier<String> clusterIdSupplier) {
        this.siteSearchReconciler = siteSearchReconciler;
        this.contentReconciler = contentReconciler;
        this.clusterIdSupplier = clusterIdSupplier;
    }

    /** Builds the readiness report for the current phase. */
    public MigrationReadiness evaluate() {
        final MigrationPhase phase = MigrationPhase.current();
        final List<MirrorStatus> content = new ArrayList<>(contentReconciler.statuses());
        final List<MirrorStatus> siteSearch = new ArrayList<>(siteSearchReconciler.statuses());

        final List<MirrorStatus> all = new ArrayList<>(content.size() + siteSearch.size());
        all.addAll(content);
        all.addAll(siteSearch);

        final List<MirrorStatus> outOfSync = all.stream()
                .filter(MirrorStatus::needsAttention)
                .collect(Collectors.toList());
        // A downgrade routes reads back to Elasticsearch; any index whose ES copy is missing or behind
        // its OpenSearch counterpart would lose that delta after the downgrade (a failed ES count is -1, which
        // is < any real OS count → flagged, fail-safe).
        final boolean esBehindAnywhere = all.stream()
                .anyMatch(s -> !s.es().exists() || s.es().docCount() < s.os().docCount());

        final boolean safeToAdvance;
        final String summary;
        final List<String> blockers = new ArrayList<>();

        if (phase.isMigrationNotStarted()) {
            safeToAdvance = true;
            summary = "Phase 0 (Elasticsearch only). OpenSearch counterparts are built during the dual-write "
                    + "phases, so there is nothing to reconcile yet. Safe to advance to Phase 1.";
        } else if (phase.isMigrationComplete()) {
            safeToAdvance = true; // no phase beyond 3
            summary = "Phase 3 (OpenSearch only) — the final phase, nothing to advance to. "
                    + (esBehindAnywhere
                        ? "WARNING: OpenSearch holds content Elasticsearch does not; a downgrade would "
                                + "hide it until a full reindex."
                        : "No index shows Elasticsearch behind OpenSearch; still verify before any "
                                + "downgrade.");
        } else {
            safeToAdvance = outOfSync.isEmpty();
            for (final MirrorStatus s : outOfSync) {
                blockers.add(String.format("%s '%s': %s", s.kind(), s.indexName(), s.recommendation()));
            }
            summary = safeToAdvance
                    ? "All mirrors are in sync. Safe to advance toward the OpenSearch-only phase."
                    : String.format("%d index(es) out of sync. Re-crawl/reindex them before promoting "
                            + "the phase — Phase 3 reads OpenSearch with no Elasticsearch fallback.",
                            outOfSync.size());
        }

        final MigrationReadiness.PhaseInfo phaseInfo = new MigrationReadiness.PhaseInfo(
                phase.ordinal(), phase.name(), readEngine(phase), writeEngines(phase),
                phase.isDualWrite());
        final MigrationReadiness.Verdict verdict = new MigrationReadiness.Verdict(
                safeToAdvance, !esBehindAnywhere, outOfSync.size(), summary, blockers);
        return new MigrationReadiness(clusterIdSupplier.get(), phaseInfo, verdict, content, siteSearch);
    }

    private static String readEngine(final MigrationPhase phase) {
        return phase.isReadEnabled() ? "OpenSearch" : "Elasticsearch";
    }

    private static List<String> writeEngines(final MigrationPhase phase) {
        if (phase.isMigrationNotStarted()) {
            return List.of("Elasticsearch");
        }
        if (phase.isMigrationComplete()) {
            return List.of("OpenSearch");
        }
        return List.of("Elasticsearch", "OpenSearch");
    }
}
