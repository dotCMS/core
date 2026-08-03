package com.dotcms.content.index.migration;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        // Content WORKING/LIVE are mandatory in every pre-OpenSearch-only phase: they are the source
        // that gets mirrored to OpenSearch, so a missing slot (pointer unset, or its Elasticsearch copy
        // gone) means there is nothing to migrate — a hard no-go, independent of the sync check, which
        // would otherwise pass vacuously when there are no active indices at all. Site Search is an open
        // set that may legitimately be empty, so it is not required here.
        final List<String> missingContent = requiredContentBlockers(content);

        final boolean safeToAdvance;
        final String summary;
        final List<String> blockers = new ArrayList<>();

        if (phase.isMigrationComplete()) {
            safeToAdvance = true; // no phase beyond 3
            summary = "Phase 3 (OpenSearch only) — the final phase, nothing to advance to. "
                    + (esBehindAnywhere
                        ? "WARNING: OpenSearch holds content Elasticsearch does not; a downgrade would "
                                + "hide it until a full reindex."
                        : "No index shows Elasticsearch behind OpenSearch; still verify before any "
                                + "downgrade.");
        } else if (phase.isMigrationNotStarted()) {
            // Phase 0: OpenSearch counterparts are built later (during dual-write), so their absence is
            // expected and NOT a blocker; only the mandatory Elasticsearch content pair is required.
            blockers.addAll(missingContent);
            safeToAdvance = blockers.isEmpty();
            summary = safeToAdvance
                    ? "Phase 0 (Elasticsearch only). OpenSearch counterparts are built during the "
                            + "dual-write phases, so there is nothing to reconcile yet. Safe to advance "
                            + "to Phase 1."
                    : String.format("Not safe to advance from Phase 0: %s to resolve first (see the "
                            + "blockers list). Dual-write needs an active Elasticsearch content index "
                            + "to mirror from.", plural(blockers.size(), "blocker"));
        } else {
            // Phases 1/2 (dual-write): require the mandatory content pair AND every mirror in sync. Drop
            // out-of-sync rows already reported as missing content (an ES-missing content slot surfaces
            // both as a missing-source blocker and as MISSING_COUNTERPART) so it is not reported twice.
            blockers.addAll(missingContent);
            outOfSync.stream()
                    .filter(s -> !(isContent(s.kind()) && !s.es().exists()))
                    .forEach(s -> blockers.add(String.format("%s '%s': %s", s.kind(), s.indexName(),
                            s.recommendation())));
            safeToAdvance = blockers.isEmpty();
            summary = safeToAdvance
                    ? "All mirrors are in sync. Safe to advance toward the OpenSearch-only phase."
                    : String.format("Not safe to advance: %s to resolve first (see the blockers list). "
                            + "Phase 3 serves reads from OpenSearch only, so every index must be present "
                            + "and in sync before promoting.", plural(blockers.size(), "blocker"));
        }

        final MigrationReadiness.PhaseInfo phaseInfo = new MigrationReadiness.PhaseInfo(
                phase.ordinal(), phase.name(), readEngine(phase), writeEngines(phase),
                phase.isDualWrite());
        final MigrationReadiness.Verdict verdict = new MigrationReadiness.Verdict(
                safeToAdvance, !esBehindAnywhere, outOfSync.size(), summary, blockers);

        // Content is keyed by slot (WORKING/LIVE — a fixed pair, so a keyed object reads naturally);
        // Site Search stays a list (an open set with no natural key). LinkedHashMap keeps the
        // reconciler's order for a stable response.
        final Map<String, MirrorStatus> contentBySlot = new LinkedHashMap<>();
        for (final MirrorStatus s : content) {
            contentBySlot.put(contentSlot(s.kind()), s);
        }
        return new MigrationReadiness(clusterIdSupplier.get(), phaseInfo, contentBySlot,
                siteSearch, verdict);
    }

    private static String contentSlot(final IndexKind kind) {
        return kind == IndexKind.CONTENT_WORKING ? "WORKING" : "LIVE";
    }

    /**
     * Blockers for the mandatory content pair: WORKING and LIVE must each have a set pointer and an
     * existing Elasticsearch copy (the migration source). Returns one message per missing/empty slot;
     * an empty list means both are present. This is what stops a "no active content indices" state from
     * passing the readiness check vacuously (an empty status list would otherwise leave nothing to flag).
     */
    private static List<String> requiredContentBlockers(final List<MirrorStatus> content) {
        final List<String> out = new ArrayList<>(2);
        for (final IndexKind kind : List.of(IndexKind.CONTENT_WORKING, IndexKind.CONTENT_LIVE)) {
            final String slot = contentSlot(kind);
            final MirrorStatus status = content.stream()
                    .filter(s -> s.kind() == kind).findFirst().orElse(null);
            if (status == null) {
                out.add(String.format("No active %s content index — Elasticsearch has no %s index to "
                        + "migrate. Reindex to (re)create it before changing the phase.",
                        slot, slot.toLowerCase()));
            } else if (!status.es().exists()) {
                out.add(String.format("The active %s content index '%s' has no Elasticsearch copy — "
                        + "reindex to rebuild it before changing the phase.", slot, status.indexName()));
            }
        }
        return out;
    }

    private static boolean isContent(final IndexKind kind) {
        return kind == IndexKind.CONTENT_WORKING || kind == IndexKind.CONTENT_LIVE;
    }

    /** {@code "1 blocker"} / {@code "2 blockers"} — count with a correctly pluralized noun. */
    private static String plural(final int count, final String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
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
