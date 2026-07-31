package com.dotcms.content.index.migration;

import java.util.List;

/**
 * Support-facing ES→OS migration-readiness report (issue #36360): a point-in-time snapshot that a
 * role-gated support technician reads <em>before</em> changing the migration phase, to see whether it
 * is safe and, if not, what to do. Read-only and stateless — every field is derived from live index
 * state at request time, nothing is persisted.
 *
 * @param phase             the current migration phase and which engine it reads/writes
 * @param verdict           the overall go/no-go for advancing and rolling back, with reasons
 * @param contentIndices    per-index mirror status for the versioned content indices (working/live)
 * @param siteSearchIndices per-index mirror status for the Site Search indices
 */
public record MigrationReadiness(
        PhaseInfo phase,
        Verdict verdict,
        List<MirrorStatus> contentIndices,
        List<MirrorStatus> siteSearchIndices) {

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
