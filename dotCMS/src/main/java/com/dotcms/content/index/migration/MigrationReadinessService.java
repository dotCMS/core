package com.dotcms.content.index.migration;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.content.index.migration.ContentIndexMirrorReconciler.ContentMirrors;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 *       safe with an explanatory summary, but only once the mandatory content pair was actually
 *       resolved; neither verdict may be asserted over zero measurements (issue #37635).</li>
 *   <li><b>Rollback</b> (downgrade): a downgrade ultimately routes reads back to Elasticsearch
 *       (Phases 0/1), so it is unsafe when any index's ES copy is missing, behind its OpenSearch
 *       counterpart, or when either count could not be measured — that delta (typically content written
 *       while OpenSearch served reads) would be silently missing until a full reindex. Derived from the
 *       same live counts, so no historical state is needed. Also unsafe when the mandatory content pair
 *       could not be resolved at all: with no rows to compare, "nothing shows Elasticsearch behind" is
 *       vacuously true, and a downgrade is the one decision this field exists to protect
 *       (issue #37635).</li>
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
        final ContentMirrors contentMirrors = contentReconciler.mirrors();
        final List<MirrorStatus> content = new ArrayList<>(contentMirrors.statuses());
        final List<MirrorStatus> siteSearch = new ArrayList<>(siteSearchReconciler.statuses());

        final List<MirrorStatus> all = new ArrayList<>(content.size() + siteSearch.size());
        all.addAll(content);
        all.addAll(siteSearch);

        final List<MirrorStatus> outOfSync = all.stream()
                .filter(s -> needsAttentionIn(phase, s))
                .collect(Collectors.toList());
        final boolean esBehindAnywhere = all.stream()
                .anyMatch(MigrationReadinessService::blocksRollback);

        // Content WORKING/LIVE are mandatory in EVERY phase: they are the active content store, so a
        // missing slot (pointer unset, or the copy on the engine that owns it gone) means there is
        // nothing to report on — a hard no-go, independent of the sync check, which would otherwise
        // pass vacuously when there are no active indices at all. Site Search is an open set that may
        // legitimately be empty, so it is not required here.
        final List<String> missingContent =
                requiredContentBlockers(content, phase, contentMirrors.unreadableReason());

        final boolean safeToAdvance;
        final String summary;
        final List<String> blockers = new ArrayList<>();

        if (phase.isMigrationComplete()) {
            // Phase 3 has no phase beyond it, so "safe to advance" is normally trivially true — but
            // only once something was actually measured. With the mandatory content pair missing there
            // are zero rows to reason about, and every field below would otherwise assert a clean bill
            // of health over zero measurements: outOfSyncCount reads 0 because nothing was counted and
            // the rollback verdict reads safe because no index could show Elasticsearch behind
            // OpenSearch (issue #37635). Report the gap instead of the vacuous all-clear.
            blockers.addAll(missingContent);
            safeToAdvance = blockers.isEmpty();
            summary = safeToAdvance
                    ? "Phase 3 (OpenSearch only) — the final phase, nothing to advance to. "
                        + (esBehindAnywhere
                            ? "WARNING: OpenSearch holds content Elasticsearch does not; a downgrade "
                                    + "would hide it until a full reindex."
                            : "No index shows Elasticsearch behind OpenSearch; still verify before any "
                                    + "downgrade.")
                    : String.format("Phase 3 (OpenSearch only), but this report measured nothing: %s "
                            + "to resolve first (see the blockers list). No conclusion below is "
                            + "supported by data — an outOfSyncCount of 0 here means nothing was "
                            + "counted, not that nothing is wrong, and a downgrade must not be "
                            + "attempted on this reading.", plural(blockers.size(), "blocker"));
        } else if (phase.isMigrationNotStarted()) {
            // Phase 0: OpenSearch counterparts are built later (during dual-write), so their absence is
            // expected and NOT a blocker; only the mandatory Elasticsearch content pair is required.
            blockers.addAll(missingContent);
            safeToAdvance = blockers.isEmpty();
            summary = safeToAdvance
                    ? "Phase 0 (Elasticsearch only). OpenSearch counterparts are built during the "
                            + "dual-write phases, so there is nothing to reconcile yet. Safe to advance "
                            + "to Phase 1."
                            // Anything still counted here is NOT a yet-to-be-built counterpart (those are
                            // filtered out above) — e.g. a leftover OpenSearch index with no Elasticsearch
                            // source. Not a blocker for starting dual-write, but say so rather than leave a
                            // non-zero count contradicting "nothing to reconcile yet".
                            + (outOfSync.isEmpty() ? ""
                                : String.format(" Note: %s from an earlier migration attempt %s left over "
                                        + "on OpenSearch; dual-write will overwrite them on the next "
                                        + "crawl/reindex.", plural(outOfSync.size(), "index", "indices"),
                                        outOfSync.size() == 1 ? "is" : "are"))
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
        // A downgrade routes reads back to Elasticsearch, so it is safe only when the report actually
        // saw the mandatory content pair. Without it, "no index shows Elasticsearch behind OpenSearch"
        // is vacuously true over an empty list — and the rollback verdict is the one decision that
        // reading exists to protect (issue #37635).
        final boolean safeToRollback = missingContent.isEmpty() && !esBehindAnywhere;
        final MigrationReadiness.Verdict verdict = new MigrationReadiness.Verdict(
                safeToAdvance, safeToRollback, outOfSync.size(), summary, blockers);

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

    /**
     * Whether an index needs operator attention <em>in this phase</em> — i.e. whether it belongs in
     * {@code outOfSyncCount}. Same as {@link MirrorStatus#needsAttention()} except in Phase 0, where the
     * OpenSearch counterparts have not been built yet (they are created during dual-write): a missing
     * OpenSearch copy there is the expected state, not something to reconcile, so counting it would
     * contradict the "nothing to reconcile yet" verdict a technician reads next to it. An OpenSearch copy
     * that exists while the Elasticsearch one does not — or a count drift between two existing copies —
     * is still unexpected in Phase 0 and stays reported.
     *
     * <p>Phase 3 is the mirror image: Elasticsearch no longer receives writes, so an Elasticsearch copy
     * that is missing, or measurably behind OpenSearch, is expected and not counted.</p>
     */
    private static boolean needsAttentionIn(final MigrationPhase phase, final MirrorStatus status) {
        if (!status.needsAttention()) {
            return false;
        }
        final boolean expectedMissingCounterpart =
                phase.isMigrationNotStarted() && status.es().exists() && !status.os().exists();
        // The mirror image at the other end of the migration: past Phase 3 Elasticsearch receives no
        // more writes, so its copy either being gone or lagging behind OpenSearch is the expected
        // steady state, not something to reconcile. The lag is the common case: the switchover keeps
        // the active Elasticsearch pointers, so while that cluster is still reachable its index is
        // measured, frozen at cutover, and every edit made since leaves it further behind. Without
        // this, a fully healthy Phase 3 install reports outOfSyncCount = 2 forever and the field stops
        // being usable as a health gauge in the phase it matters most. Both states are still
        // reported — by safeToRollback and the summary's downgrade warning, which are the fields that
        // own that fact, and which remain untouched here because they are derived from blocksRollback
        // rather than from this count. Elasticsearch AHEAD of OpenSearch, or an unmeasurable count
        // on either side, is not expected and stays counted.
        final boolean expectedFrozenEsCopy = phase.isMigrationComplete() && status.os().exists()
                && (!status.es().exists() || esBehindMeasurably(status));
        return !expectedMissingCounterpart && !expectedFrozenEsCopy;
    }

    /**
     * Whether both counts were measured and Elasticsearch holds fewer documents than OpenSearch.
     * A {@code -1} (unmeasurable) on either side is never read as "behind": it is an unknown, and the
     * drift verdict already treats it as needing attention.
     */
    private static boolean esBehindMeasurably(final MirrorStatus status) {
        final long esCount = status.es().docCount();
        final long osCount = status.os().docCount();
        return esCount >= 0 && osCount >= 0 && esCount < osCount;
    }

    /**
     * Whether this index makes a downgrade unsafe: a downgrade routes reads back to Elasticsearch, so the
     * Elasticsearch copy must exist and be at least as complete as its OpenSearch counterpart — otherwise
     * that delta (typically content written while OpenSearch served reads) is silently lost until a full
     * reindex. An <em>unmeasurable</em> count on either side (reported as {@code -1}) is treated as unsafe
     * rather than compared numerically: with {@code es=100, os=-1} a bare {@code 100 < -1} would read as
     * safe while OpenSearch may well be ahead — the same fail-safe stance the drift verdict takes.
     */
    private static boolean blocksRollback(final MirrorStatus status) {
        return !status.es().exists()
                || status.es().docCount() < 0 || status.os().docCount() < 0
                || status.es().docCount() < status.os().docCount();
    }

    private static String contentSlot(final IndexKind kind) {
        return kind == IndexKind.CONTENT_WORKING ? "WORKING" : "LIVE";
    }

    /**
     * Blockers for the mandatory content pair: WORKING and LIVE must each have a set pointer and an
     * existing copy on the engine that owns the content in this phase — Elasticsearch while it is still
     * the migration source (phases 0/1/2), OpenSearch once it is the only store left (phase 3). Returns
     * one message per missing/empty slot; an empty list means both are present.
     *
     * <p>This is what stops a "no active content indices" state from passing the readiness check
     * vacuously — an empty status list would otherwise leave nothing to flag, and every verdict derived
     * from it would be asserted over zero measurements (issues #36360 and #37635).</p>
     */
    private static List<String> requiredContentBlockers(final List<MirrorStatus> content,
            final MigrationPhase phase, final Optional<String> unreadableReason) {
        final boolean openSearchOwnsContent = phase.isMigrationComplete();
        final String engine = openSearchOwnsContent ? "OpenSearch" : "Elasticsearch";
        // A store that could not be read is NOT a store with no indices. Both leave the report with
        // no rows, but the operator action is the opposite — fix the read, do not reindex — and the
        // per-slot messages below would confidently prescribe the wrong one (issue #37635).
        if (unreadableReason.isPresent()) {
            return List.of(String.format("The active content indices could not be determined: %s. "
                    + "Nothing below was measured, so no conclusion in this report is supported by "
                    + "data. Resolve the read failure and re-run this check — do NOT reindex on the "
                    + "strength of this reading; it is not known whether the indices are missing.",
                    unreadableReason.get()));
        }
        final List<String> out = new ArrayList<>(2);
        for (final IndexKind kind : List.of(IndexKind.CONTENT_WORKING, IndexKind.CONTENT_LIVE)) {
            final String slot = contentSlot(kind);
            final MirrorStatus status = content.stream()
                    .filter(s -> s.kind() == kind).findFirst().orElse(null);
            if (status == null) {
                out.add(String.format("No active %s content index is registered — %s has no %s index "
                        + "to report on. Reindex to (re)create it before changing the phase.",
                        slot, engine, slot.toLowerCase()));
            } else if (openSearchOwnsContent ? !status.os().exists() : !status.es().exists()) {
                out.add(String.format("The active %s content index '%s' has no %s copy — reindex to "
                        + "rebuild it before changing the phase.", slot, status.indexName(), engine));
            }
        }
        return out;
    }

    private static boolean isContent(final IndexKind kind) {
        return kind == IndexKind.CONTENT_WORKING || kind == IndexKind.CONTENT_LIVE;
    }

    /** {@code "1 blocker"} / {@code "2 blockers"} — count with a correctly pluralized noun. */
    private static String plural(final int count, final String noun) {
        return plural(count, noun, noun + "s");
    }

    /** Same, for nouns whose plural is not formed by appending an {@code s} ({@code index}/{@code indices}). */
    private static String plural(final int count, final String singular, final String pluralForm) {
        return count + " " + (count == 1 ? singular : pluralForm);
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
