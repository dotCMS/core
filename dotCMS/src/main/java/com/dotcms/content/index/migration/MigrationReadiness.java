package com.dotcms.content.index.migration;

import java.util.List;
import java.util.Map;

/**
 * Support-facing ES→OS migration-readiness report (issue #36360): a point-in-time snapshot that a
 * role-gated support technician reads <em>before</em> changing the migration phase, to see whether it
 * is safe and, if not, what to do. Read-only and stateless — every field is derived from live index
 * state at request time, nothing is persisted.
 *
 * @param clusterId         the dotCMS cluster id embedded in every physical index name
 *                          (the {@code <id>} of the {@code cluster_<id>.} prefix); identical for the
 *                          Elasticsearch and OpenSearch backends
 * @param phase      the current migration phase and which engine it reads/writes
 * @param content    the versioned content indices keyed by slot ({@code WORKING} / {@code LIVE}) — a
 *                   fixed pair, so a keyed object reads naturally
 * @param siteSearch the Site Search indices as a list — an open set with no natural key, so a list
 *                   (each entry carries its own {@code indexName})
 * @param verdict    the overall go/no-go for advancing and rolling back, with reasons
 */
public record MigrationReadiness(
        String clusterId,
        PhaseInfo phase,
        Map<String, MirrorStatus> content,
        List<MirrorStatus> siteSearch,
        Verdict verdict) {

    /**
     * @param current      the current phase ordinal (0–3)
     * @param name         the phase enum name (e.g. {@code PHASE_2_DUAL_WRITE_OS_READS})
     * @param readEngine   which engine currently serves reads ("Elasticsearch" or "OpenSearch")
     * @param writeEngines which engines currently receive writes
     * @param evaluable    whether a cross-engine comparison is meaningful for a forward phase change
     *                     (only the dual-write phases 1/2); when false the mirror lists are advisory
     *                     context, not a forward go/no-go
     */
    public record PhaseInfo(
            int current,
            String name,
            String readEngine,
            List<String> writeEngines,
            boolean evaluable) {}

    /**
     * @param safeToAdvance  whether it is safe to promote toward the OpenSearch-only phase
     * @param safeToRollback whether it is safe to downgrade — false when any index's Elasticsearch
     *                       copy is behind its OpenSearch counterpart, because a downgrade routes reads back
     *                       to Elasticsearch and would silently drop that delta until a reindex
     * @param outOfSyncCount how many indices need attention (missing counterpart or count drift)
     * @param summary        one human-readable sentence describing the overall state
     * @param blockers       per-index reasons that make advancing unsafe (empty when safe)
     */
    public record Verdict(
            boolean safeToAdvance,
            boolean safeToRollback,
            int outOfSyncCount,
            String summary,
            List<String> blockers) {}
}
