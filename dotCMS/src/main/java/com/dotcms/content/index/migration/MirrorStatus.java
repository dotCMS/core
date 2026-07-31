package com.dotcms.content.index.migration;

/**
 * Per-index ES↔OS mirror status for the migration-readiness report (issue #36360): how one logical
 * index compares against its twin across the two engines. Purely factual — the phase-aware
 * "is this a blocker for changing phase" interpretation is layered on top by
 * {@link MigrationReadinessService}.
 *
 * <p>Shared by every index family that is mirrored during the migration: the versioned content
 * indices (working/live) and the Site Search indices. The {@link IndexKind} says which family this
 * row belongs to.</p>
 *
 * @param indexName      the logical index name (no {@code .os} tag)
 * @param kind           which mirrored index family this row belongs to
 * @param esExists       whether the Elasticsearch copy exists
 * @param esDocCount     exact document count in the Elasticsearch copy (0 when absent, -1 when the
 *                       count query failed)
 * @param osExists       whether the OpenSearch ({@code .os}) twin exists
 * @param osDocCount     exact document count in the OpenSearch twin (0 when absent, -1 when the count
 *                       query failed)
 * @param verdict        the diff verdict between the two copies
 * @param recommendation human-readable, action-oriented advice for a support technician
 */
public record MirrorStatus(
        String indexName,
        IndexKind kind,
        boolean esExists,
        long esDocCount,
        boolean osExists,
        long osDocCount,
        Verdict verdict,
        String recommendation) {

    /** Which mirrored index family a status row belongs to. */
    public enum IndexKind { CONTENT_WORKING, CONTENT_LIVE, SITE_SEARCH }

    /** The diff outcome between an index and its twin. */
    public enum Verdict {
        /** Both copies exist with the same document count. */
        IN_SYNC,
        /** The index exists on one engine but its twin is missing on the other. */
        MISSING_TWIN,
        /** Both copies exist but hold a different number of documents. */
        COUNT_DRIFT
    }

    /** Whether this index needs operator action (a re-crawl / reindex) before the phase change. */
    public boolean needsAttention() {
        return verdict != Verdict.IN_SYNC;
    }

    /**
     * Classifies a mirror from raw existence + exact counts: a missing copy on either engine is
     * {@link Verdict#MISSING_TWIN}; both present with unequal counts is {@link Verdict#COUNT_DRIFT}
     * (a failed count is reported as {@code -1}, which compares unequal and so surfaces as drift —
     * fail-safe); otherwise {@link Verdict#IN_SYNC}.
     */
    public static Verdict verdictFor(final boolean esExists, final boolean osExists,
            final long esDocCount, final long osDocCount) {
        if (!esExists || !osExists) {
            return Verdict.MISSING_TWIN;
        }
        if (esDocCount != osDocCount) {
            return Verdict.COUNT_DRIFT;
        }
        return Verdict.IN_SYNC;
    }
}
