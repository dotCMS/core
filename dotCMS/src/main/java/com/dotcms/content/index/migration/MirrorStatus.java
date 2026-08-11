package com.dotcms.content.index.migration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Per-index ES↔OS mirror status for the migration-readiness report (issue #36360): how one logical
 * index compares against its counterpart across the two engines. Purely factual — the phase-aware
 * "is this a blocker for changing phase" interpretation is layered on top by
 * {@link MigrationReadinessService}.
 *
 * <p>Shared by every index family that is mirrored during the migration: the versioned content
 * indices (working/live) and the Site Search indices. The {@link IndexKind} says which family this
 * row belongs to. Each engine's side is a nested {@link EngineCopy} so the report reads as
 * {@code es:{exists,docCount}} / {@code os:{exists,docCount}}.</p>
 *
 * @param indexName      the logical index name (no {@code .os} tag)
 * @param kind           which mirrored index family this row belongs to
 * @param es             the Elasticsearch copy (existence + exact document count)
 * @param os             the OpenSearch ({@code .os}) copy (existence + exact document count)
 * @param verdict        the diff verdict between the two copies
 * @param recommendation human-readable, action-oriented advice for a support technician
 */
@JsonIgnoreProperties("kind") // internal grouping/label only — the report keys rows by it, never emits it
public record MirrorStatus(
        String indexName,
        IndexKind kind,
        EngineCopy es,
        EngineCopy os,
        Verdict verdict,
        String recommendation) {

    /** Which mirrored index family a status row belongs to. */
    public enum IndexKind { CONTENT_WORKING, CONTENT_LIVE, SITE_SEARCH }

    /** The diff outcome between an index and its counterpart. */
    public enum Verdict {
        /** Both copies exist with the same document count. */
        IN_SYNC,
        /** The index exists on one engine but its counterpart is missing on the other. */
        MISSING_COUNTERPART,
        /** Both copies exist but hold a different number of documents. */
        COUNT_DRIFT
    }

    /**
     * One engine's copy of the index.
     *
     * <p>The alias is reported <em>per engine</em> on purpose: during the migration an index can carry
     * its alias on one engine and not on the other (e.g. an index created before dual-write started,
     * whose counterpart was built later), and that asymmetry is precisely what an operator needs to
     * see. Collapsing both sides into one field would hide it.</p>
     *
     * @param exists       whether this engine holds the index
     * @param docCount     exact document count (0 when absent, -1 when the count query failed)
     * @param physicalName the full index name as stored on that engine's server — cluster-prefixed and,
     *                     for OpenSearch, {@code .os}-tagged (e.g. {@code cluster_08abc3.live_20260406}
     *                     on ES, {@code cluster_08abc3.live_20260406.os} on OS). Reported whether or not
     *                     the copy exists, so a missing copy shows the name to look for.
     * @param alias        the alias this engine has attached to the index, or {@code null} when it has
     *                     none — and always {@code null} for the content indices, which are addressed by
     *                     name only. Omitted from the JSON when {@code null}.
     */
    public record EngineCopy(boolean exists, long docCount, String physicalName,
            @JsonInclude(JsonInclude.Include.NON_NULL) String alias) {

        /** An engine copy with no alias — the shape the content indices use. */
        public EngineCopy(final boolean exists, final long docCount, final String physicalName) {
            this(exists, docCount, physicalName, null);
        }
    }

    /** Whether this index needs operator action (a re-crawl / reindex) before the phase change. */
    public boolean needsAttention() {
        return verdict != Verdict.IN_SYNC;
    }

    /**
     * Signed percentage by which the OpenSearch (mirror) document count deviates from the
     * Elasticsearch (original), relative to the original: {@code 0.0} when equal, negative when the
     * mirror is behind, positive when it is ahead (e.g. ES=1000/OS=900 → {@code -10.0}; a missing OS
     * copy → {@code -100.0}). When the original is empty a non-empty mirror reads as {@code 100.0}.
     * {@code null} when either count is unknown (a failed count, reported as -1). Rounded to two
     * decimals.
     */
    @JsonProperty("driftPercent")
    @Schema(description = "How far the OpenSearch mirror deviates from the Elasticsearch original, as a "
            + "signed percentage of the original count: (OS − ES) / ES × 100, rounded to 2 decimals. "
            + "Read it as: 0.0 = in sync; negative = mirror is BEHIND (missing that % of docs, e.g. "
            + "-10.0 means the mirror lacks 10% of the original); positive = mirror is AHEAD (has that "
            + "% extra); -100.0 = mirror empty or absent; +100.0 = original empty but mirror has data; "
            + "null = a count could not be measured. Negative drift is the risk before advancing to "
            + "Phase 3; positive drift is the risk before a downgrade.")
    public Double driftPercent() {
        final long esCount = es.docCount();
        final long osCount = os.docCount();
        if (esCount < 0 || osCount < 0) {
            return null;
        }
        if (esCount == osCount) {
            return 0.0;
        }
        final double pct = esCount == 0 ? 100.0 : (osCount - esCount) * 100.0 / esCount;
        return Math.round(pct * 100.0) / 100.0;
    }

    /**
     * Classifies a mirror from raw existence + exact counts: a missing copy on either engine is
     * {@link Verdict#MISSING_COUNTERPART}; both present with unequal counts is {@link Verdict#COUNT_DRIFT}
     * (a failed count is reported as {@code -1}, which compares unequal and so surfaces as drift —
     * fail-safe); otherwise {@link Verdict#IN_SYNC}.
     */
    public static Verdict verdictFor(final boolean esExists, final boolean osExists,
            final long esDocCount, final long osDocCount) {
        if (!esExists || !osExists) {
            return Verdict.MISSING_COUNTERPART;
        }
        if (esDocCount != osDocCount) {
            return Verdict.COUNT_DRIFT;
        }
        return Verdict.IN_SYNC;
    }
}
