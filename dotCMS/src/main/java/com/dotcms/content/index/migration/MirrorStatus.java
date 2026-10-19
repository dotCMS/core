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
 * @param indexName         the logical index name (no {@code .os} tag)
 * @param kind              which mirrored index family this row belongs to
 * @param es                the Elasticsearch copy (existence + exact document count)
 * @param os                the OpenSearch ({@code .os}) copy (existence + exact document count)
 * @param verdict           the diff verdict between the two copies
 * @param recommendation    human-readable, action-oriented advice for a support technician
 * @param databaseDocCount  how many documents this index <em>should</em> hold according to the
 *                          database — the engine-independent denominator behind
 *                          {@link #esIndexedPercent()} / {@link #osIndexedPercent()}. An exact count
 *                          of {@code contentlet_version_info}, so 100% means complete and any excess is
 *                          real. Only the content indices have one; {@code null} for Site Search, whose
 *                          corpus (crawled pages and files) has no such counterpart, and {@code null}
 *                          when it could not be read.
 */
@JsonIgnoreProperties("kind") // internal grouping/label only — the report keys rows by it, never emits it
public record MirrorStatus(
        String indexName,
        IndexKind kind,
        EngineCopy es,
        EngineCopy os,
        Verdict verdict,
        String recommendation,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long databaseDocCount) {

    /**
     * Indexed percentage (see {@link #osIndexedPercent()}) below which an existing copy is treated as
     * incomplete against the database. Not a tight bound on purpose: counts are taken while indexing
     * may still be catching up, so this is meant to catch "3% of the content" — a reindex that never
     * finished — not a handful of documents in flight. Shared by the reconciler's "incomplete" note
     * and the Phase 3 health count so the two never disagree.
     */
    public static final double INCOMPLETE_INDEXED_THRESHOLD = 95.0;

    /** Engine name as the report spells it, in {@code unreachableEngines} and in its messages. */
    public static final String ELASTICSEARCH = "Elasticsearch";

    /** Engine name as the report spells it, in {@code unreachableEngines} and in its messages. */
    public static final String OPENSEARCH = "OpenSearch";

    /** A row with no database denominator — the shape the Site Search indices use. */
    public MirrorStatus(final String indexName, final IndexKind kind, final EngineCopy es,
            final EngineCopy os, final Verdict verdict, final String recommendation) {
        this(indexName, kind, es, os, verdict, recommendation, null);
    }

    /** Which mirrored index family a status row belongs to. */
    public enum IndexKind { CONTENT_WORKING, CONTENT_LIVE, SITE_SEARCH }

    /** The diff outcome between an index and its counterpart. */
    public enum Verdict {
        /** Both copies exist with the same document count. */
        IN_SYNC,
        /** The index exists on one engine but its counterpart is missing on the other. */
        MISSING_COUNTERPART,
        /** Both copies exist but hold a different number of documents. */
        COUNT_DRIFT,
        /**
         * One engine could not be read at all (unreachable, timing out, refusing the request), so the
         * two copies could not be compared. Says nothing about whether the copy exists: the fix is to
         * restore access and re-run the check, never to reindex on the strength of it (issue #37636).
         */
        UNMEASURED
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
     * @param unavailableReason why this engine could not be read at all, or {@code null} when it was
     *                     read. When set, {@code exists} is {@code false} and {@code docCount} is
     *                     {@code -1} because nothing is known, not because the copy is gone — read this
     *                     field before either of them (issue #37636). Omitted from the JSON when
     *                     {@code null}.
     */
    public record EngineCopy(boolean exists, long docCount, String physicalName,
            @JsonInclude(JsonInclude.Include.NON_NULL) String alias,
            @JsonInclude(JsonInclude.Include.NON_NULL) String unavailableReason) {

        /** An engine copy with no alias — the shape the content indices use. */
        public EngineCopy(final boolean exists, final long docCount, final String physicalName) {
            this(exists, docCount, physicalName, null, null);
        }

        /** An engine copy that was read, with the alias that engine attached to it. */
        public EngineCopy(final boolean exists, final long docCount, final String physicalName,
                final String alias) {
            this(exists, docCount, physicalName, alias, null);
        }

        /**
         * The copy on an engine that could not be read: existence unknown, count unmeasurable.
         *
         * @param physicalName the name to look for once the engine answers again (may be {@code null})
         * @param alias        the alias, when known from elsewhere; usually {@code null}
         * @param reason       why the engine could not be read, as reported to the operator
         */
        public static EngineCopy unavailable(final String physicalName, final String alias,
                final String reason) {
            return new EngineCopy(false, -1L, physicalName, alias, reason);
        }

        /**
         * Whether this engine was actually read. Not a JSON property: {@code unavailableReason} already
         * carries the same fact.
         */
        public boolean wasRead() {
            return unavailableReason == null;
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
     * What percentage of the database's content the Elasticsearch copy holds — a percentage of
     * {@link #databaseDocCount}. See {@link #indexedPercentOf(EngineCopy)}.
     */
    @JsonProperty("esIndexedPercent")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Percentage of the documents the database says this index should hold that "
            + "the Elasticsearch copy actually holds. 100.0 = complete. Absent for Site Search "
            + "(no database denominator) and when a count could not be measured.")
    public Double esIndexedPercent() {
        return indexedPercentOf(es);
    }

    /**
     * What percentage of the database's content the OpenSearch copy holds — a percentage of
     * {@link #databaseDocCount}. See {@link #indexedPercentOf(EngineCopy)}.
     */
    @JsonProperty("osIndexedPercent")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Percentage of the documents the database says this index should hold that "
            + "the OpenSearch copy actually holds. 100.0 = complete; a low value means the mirror was "
            + "never rebuilt — and anything reading through it (including a Site Search crawl) sees "
            + "only that fraction of the content. Absent for Site Search (no database denominator) "
            + "and when a count could not be measured.")
    public Double osIndexedPercent() {
        return indexedPercentOf(os);
    }

    /**
     * One engine's completeness against the database: {@code docCount / databaseDocCount × 100},
     * rounded to two decimals.
     *
     * <p><strong>Why this exists next to {@link #driftPercent()}.</strong> Drift compares the two
     * engines against <em>each other</em>, which stops being an answer once one of them is the only
     * one left: in Phase 3 there is no Elasticsearch side to compare against, so a mirror that was
     * never rebuilt looks unremarkable. This compares each engine against the <em>database</em> —
     * the source of truth, identical in every phase — so "this index holds 3% of the content" is
     * still visible when there is nothing to diff (issue #36983).</p>
     *
     * <p>The denominator counts one row per (identifier, language, variant) in
     * {@code contentlet_version_info} — the same unit as an index document — so a complete index reads
     * exactly {@code 100.0}. Above 100% means the index holds documents the database no longer has
     * (orphans left by a delete that never propagated), which is worth looking at rather than
     * rounding away.</p>
     *
     * @return the percentage, or {@code null} when there is no denominator ({@code databaseDocCount}
     *         absent or zero), the count was unmeasurable ({@code -1}), or this engine has no copy of
     *         the index at all
     */
    private Double indexedPercentOf(final EngineCopy copy) {
        // A copy that does not exist counts 0 documents, but reporting 0.0% would say "this engine
        // lost all its content" when the truth is "this engine has no such index" — two different
        // facts, and the difference matters most in Phase 3, where every Elasticsearch copy is
        // legitimately gone and would otherwise light the report up with zeroes. Absence is already
        // stated by `exists` and by the MISSING_COUNTERPART verdict; this field stays silent.
        if (!copy.exists() || databaseDocCount == null || databaseDocCount <= 0
                || copy.docCount() < 0) {
            return null;
        }
        return Math.round(copy.docCount() * 10_000.0 / databaseDocCount) / 100.0;
    }

    /**
     * Classifies a mirror from raw existence + exact counts: a missing copy on either engine is
     * {@link Verdict#MISSING_COUNTERPART}; otherwise unequal counts are {@link Verdict#COUNT_DRIFT};
     * otherwise {@link Verdict#IN_SYNC}.
     *
     * <p><strong>An unmeasurable count is never {@code IN_SYNC}.</strong> A failed count query is
     * reported as {@code -1}, and it is checked explicitly rather than left to compare unequal: when
     * <em>both</em> engines fail — one cluster under load, or a search user granted
     * {@code indices:monitor/stats} but not {@code indices:data/read/count}, so existence resolves but
     * counting does not — {@code -1 == -1} would otherwise read as a perfect match and the report
     * would answer "safe to advance" about a mirror it never actually measured. Unknown degrades to
     * drift, which is what {@code needsAttention()} and {@code safeToAdvance} act on.</p>
     */
    public static Verdict verdictFor(final EngineCopy es, final EngineCopy os) {
        if (!es.wasRead() || !os.wasRead()) {
            return Verdict.UNMEASURED;
        }
        return verdictFor(es.exists(), os.exists(), es.docCount(), os.docCount());
    }

    /**
     * The recommendation for an {@link Verdict#UNMEASURED} row: which engine could not be read, why,
     * and that the fix is access, not a rebuild.
     *
     * @param what a short label for the index family, e.g. {@code "content index"}
     */
    public static String unmeasuredAdvice(final String what, final String name, final EngineCopy es,
            final EngineCopy os) {
        final String engines;
        final String reason;
        if (!es.wasRead() && !os.wasRead()) {
            engines = "Both Elasticsearch and OpenSearch";
            reason = ELASTICSEARCH + ": " + es.unavailableReason() + "; " + OPENSEARCH + ": "
                    + os.unavailableReason();
        } else if (!es.wasRead()) {
            engines = ELASTICSEARCH;
            reason = es.unavailableReason();
        } else {
            engines = OPENSEARCH;
            reason = os.unavailableReason();
        }
        return String.format("%s could not be reached (%s), so the two copies of %s '%s' could not "
                + "be compared. Restore access and re-run this check; do not rebuild the index on the "
                + "strength of this reading.", engines, reason, what, name);
    }

    /** A failure as the report shows it: the message, or the exception type when there is none. */
    public static String reasonOf(final Throwable e) {
        final String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    /**
     * Same classification from raw values, for two copies that were both read. A copy on an engine
     * that could not be read goes through {@link #verdictFor(EngineCopy, EngineCopy)} instead.
     */
    public static Verdict verdictFor(final boolean esExists, final boolean osExists,
            final long esDocCount, final long osDocCount) {
        if (!esExists || !osExists) {
            return Verdict.MISSING_COUNTERPART;
        }
        if (esDocCount < 0 || osDocCount < 0) {
            return Verdict.COUNT_DRIFT;
        }
        if (esDocCount != osDocCount) {
            return Verdict.COUNT_DRIFT;
        }
        return Verdict.IN_SYNC;
    }
}
