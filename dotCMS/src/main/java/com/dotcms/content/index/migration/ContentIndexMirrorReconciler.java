package com.dotcms.content.index.migration;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ContentletIndexOperationsES;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Content-index half of the migration-readiness report (issue #36360): compares the active versioned
 * content indices (working and live) against their {@code .os} counterparts across both engines,
 * mirroring {@link SiteSearchMirrorReconciler} but for the content store. Never mutates anything.
 *
 * <h4>How the counts are read (phase-independently)</h4>
 * <p>{@code IndiciesInfo} always holds the cluster-prefixed, <em>un-tagged</em> Elasticsearch name for
 * working/live (its backing {@code indicies} table owns only the ES rows — {@code index_version IS
 * NULL}); the OpenSearch counterpart is that name with the {@code .os} tag.</p>
 *
 * <p><strong>Existence</strong> comes from each engine leaf's {@code getIndicesStats()} — one call per
 * engine covering the whole index set, so both slots are decided from a single snapshot. Those stats
 * maps are keyed by the <em>cluster-stripped</em> name (Elasticsearch un-tagged, OpenSearch carrying
 * {@code .os}), so each raw name is stripped of the cluster prefix and then, for the OpenSearch lookup,
 * tagged — the same strip-then-tag order the maintenance JSP uses.</p>
 *
 * <p><strong>The document count</strong> is a real count query per index
 * ({@link ContentletIndexOperations#getIndexDocumentCount}), <em>not</em> the {@code docs.count} of
 * those same stats. The stats counter is per-shard and only advances when the shard refreshes, so it
 * trails a just-written document by seconds: a support technician checking whether a publish reached
 * OpenSearch would read the previous number and conclude the write was lost. This endpoint is the
 * source of truth for that question, so it must never report a number the engine can already
 * contradict (issue #36983). A count query is also not subject to the 10,000 search hit-count cap, and
 * it matches how the Site Search half has always counted — both halves now answer the same way.</p>
 *
 * <p>It queries the two engine leaves directly (never the phase-aware router) so the report shows both
 * sides regardless of which engine the current phase reads from. Scope is the active working/live
 * pair; reindex slots are out of scope for this report.</p>
 */
public class ContentIndexMirrorReconciler {

    private final IndexAPI esImpl;
    private final IndexAPI osImpl;
    private final ContentletIndexOperations esOps;
    private final ContentletIndexOperations osOps;
    private final Supplier<IndiciesInfo> indiciesSupplier;
    private final Supplier<DatabaseCounts> databaseCountsSupplier;

    public ContentIndexMirrorReconciler() {
        this(new ESIndexAPI(), CDIUtils.getBeanThrows(OSIndexAPIImpl.class),
                new ContentletIndexOperationsES(),
                CDIUtils.getBeanThrows(ContentletIndexOperationsOS.class),
                ContentIndexMirrorReconciler::loadIndiciesQuietly,
                ContentIndexMirrorReconciler::loadDatabaseCountsQuietly);
    }

    @VisibleForTesting
    ContentIndexMirrorReconciler(final IndexAPI esImpl, final IndexAPI osImpl,
            final ContentletIndexOperations esOps, final ContentletIndexOperations osOps,
            final Supplier<IndiciesInfo> indiciesSupplier,
            final Supplier<DatabaseCounts> databaseCountsSupplier) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
        this.esOps = esOps;
        this.osOps = osOps;
        this.indiciesSupplier = indiciesSupplier;
        this.databaseCountsSupplier = databaseCountsSupplier;
    }

    /**
     * How many documents each content index should hold according to the database — the denominator
     * behind the indexed percentages. Counted exactly (see {@link #loadDatabaseCountsQuietly()});
     * {@code null} on either field when it could not be read.
     *
     * @param working one row per (identifier, language, variant): the working version always exists
     * @param live    the subset of those rows that also have a live version
     */
    public record DatabaseCounts(Long working, Long live) {}

    /** Per-index mirror status for the active working and live content indices. */
    public List<MirrorStatus> statuses() {
        final IndiciesInfo info = indiciesSupplier.get();
        if (info == null) {
            return List.of();
        }
        final Map<String, IndexStats> esStats = esImpl.getIndicesStats();
        final Map<String, IndexStats> osStats = osImpl.getIndicesStats();
        final DatabaseCounts dbCounts = databaseCountsSupplier.get();
        final List<MirrorStatus> out = new ArrayList<>(2);
        addStatus(out, IndexKind.CONTENT_WORKING, info.getWorking(), esStats, osStats,
                dbCounts == null ? null : dbCounts.working());
        addStatus(out, IndexKind.CONTENT_LIVE, info.getLive(), esStats, osStats,
                dbCounts == null ? null : dbCounts.live());
        return out;
    }

    private void addStatus(final List<MirrorStatus> out, final IndexKind kind, final String rawName,
            final Map<String, IndexStats> esStats, final Map<String, IndexStats> osStats,
            final Long databaseDocCount) {
        if (!UtilMethods.isSet(rawName)) {
            return;
        }
        // IndiciesInfo holds the cluster-prefixed, un-tagged ES name — which IS the full ES physical
        // name; the OS physical name is that + .os. The stats maps are keyed by the cluster-stripped
        // name (ES un-tagged, OS carrying .os), so strip for the count lookup, tag for the OS key.
        final String esPhysical = rawName;
        final String osPhysical = IndexTag.OS.tag(rawName);
        final String bare = esImpl.removeClusterIdFromName(rawName);
        final String osKey = IndexTag.OS.tag(bare);

        // Existence from the stats snapshot; the count from a live count query (see class javadoc).
        final boolean esExists = esStats.containsKey(bare);
        final long esCount = esExists ? countQuietly(esOps, bare) : 0L;
        final boolean osExists = osStats.containsKey(osKey);
        final long osCount = osExists ? countQuietly(osOps, bare) : 0L;

        final Verdict verdict = MirrorStatus.verdictFor(esExists, osExists, esCount, osCount);
        final String recommendation = recommend(bare, verdict, osExists)
                + incompleteNote("Elasticsearch", esExists, esCount, databaseDocCount)
                + incompleteNote("OpenSearch", osExists, osCount, databaseDocCount);
        out.add(new MirrorStatus(bare, kind,
                new MirrorStatus.EngineCopy(esExists, esCount, esPhysical),
                new MirrorStatus.EngineCopy(osExists, osCount, osPhysical),
                verdict, recommendation, databaseDocCount));
    }

    /**
     * Exact document count of {@code logicalName} on one engine, or {@code -1} when the query fails.
     *
     * <p>The leaf turns the logical name into its own physical form ({@code toPhysicalName}: the ES
     * leaf cluster-prefixes it, the OpenSearch leaf also applies {@code .os}), the same convention
     * {@code ContentletIndexAPIImpl} uses — so this never hand-builds a physical name.</p>
     *
     * <p>Failures are reported as {@code -1} rather than propagated: a readiness report that answers
     * "unknown" for one engine is useful, one that returns a 500 is not. {@code -1} is the established
     * unmeasurable marker — it compares unequal, so the verdict degrades to out-of-sync and
     * {@code safeToRollback} to false, never to a false green.</p>
     */
    private static long countQuietly(final ContentletIndexOperations ops, final String logicalName) {
        return Try.of(() -> ops.getIndexDocumentCount(ops.toPhysicalName(logicalName)))
                .onFailure(e -> Logger.warn(ContentIndexMirrorReconciler.class,
                        "Could not count documents of '" + logicalName + "' on "
                                + ops.getClass().getSimpleName() + ": " + e.getMessage()))
                .getOrElse(-1L);
    }

    /**
     * Indexed percentage below which an existing index is called out as incomplete in the recommendation. Not a
     * tight bound on purpose: the denominator is an order-of-magnitude measure (see
     * {@code MirrorStatus#indexedPercentOf}), so this is meant to catch "3% of the content", not a handful of
     * documents.
     */
    private static final double INCOMPLETE_INDEXED_THRESHOLD = 95.0;

    /**
     * A sentence appended to the recommendation when an engine holds materially less content than the
     * database says it should.
     *
     * <p>This is the half of the report that survives into Phase 3. The verdict compares the two
     * engines against each other, so once one of them is the only one left it can read reassuringly
     * while the surviving index is nearly empty — and everything downstream inherits that emptiness
     * silently, including a Site Search crawl, whose corpus is a query over this very index
     * (issue #36983). Comparing against the database keeps that visible with nothing to diff.</p>
     *
     * <p>It never changes the {@code verdict}: the verdict states the ES↔OS relationship, which is a
     * different fact. Reported side by side, not merged.</p>
     */
    private static String incompleteNote(final String engine, final boolean exists, final long count,
            final Long databaseDocCount) {
        if (!exists || count < 0 || databaseDocCount == null || databaseDocCount <= 0) {
            return "";
        }
        final double indexedPercent = count * 100.0 / databaseDocCount;
        if (indexedPercent >= INCOMPLETE_INDEXED_THRESHOLD) {
            return "";
        }
        return String.format(" NOTE: the %s copy holds %d of the %d contentlets the database has "
                        + "(%.2f%%) — it was never fully rebuilt. Run a full reindex; until then, "
                        + "anything reading through this index sees only that fraction of the content "
                        + "(a Site Search crawl included, since it builds its corpus from a query "
                        + "against it).",
                engine, count, databaseDocCount, indexedPercent);
    }

    private static String recommend(final String name, final Verdict verdict, final boolean osExists) {
        switch (verdict) {
            case IN_SYNC:
                return "In sync — no action needed.";
            case MISSING_COUNTERPART:
                final String missing = osExists ? "Elasticsearch" : "OpenSearch";
                return String.format("The %s copy of content index '%s' is missing. Run a full "
                        + "reindex to rebuild it before promoting to the OpenSearch-only phase.",
                        missing, name);
            case COUNT_DRIFT:
            default:
                return String.format("The two copies of content index '%s' hold a different number "
                        + "of documents. Run a full reindex to rebuild the OpenSearch copy before "
                        + "promoting the phase.", name);
        }
    }

    /**
     * A fixed, fully literal statement — no interpolation, no parameters, nothing caller-supplied. Kept
     * as a constant rather than assembled inline so that stays evident at a glance (and so a
     * concatenation-based injection scan has nothing to flag).
     */
    private static final String DATABASE_COUNTS_SQL = """
            SELECT COUNT(*) AS working_count, COUNT(live_inode) AS live_count
            FROM contentlet_version_info
            """;

    /**
     * How many documents each content index should hold, counted exactly from
     * {@code contentlet_version_info}.
     *
     * <p>That table is keyed by {@code (identifier, lang, variant_id)} — the same unit as an index
     * document ({@code identifier_language_variant}) — so its row count is the denominator directly:
     * {@code COUNT(*)} is every working version ({@code working_inode} is {@code NOT NULL}, so every
     * row has one) and {@code COUNT(live_inode)} skips nulls and therefore counts exactly the rows that
     * also have a live version. Verified against a live install: 686/685, matching the index document
     * counts exactly.</p>
     *
     * <p><strong>Cost.</strong> PostgreSQL runs this as a {@code Parallel Seq Scan}: asking for
     * {@code COUNT(live_inode)} needs the column, so the heap is read. Measured on local copies —
     * 171k rows / 15&nbsp;ms, 394k / 21&nbsp;ms, 453k / 22&nbsp;ms — i.e. roughly linear at ~50&nbsp;ns
     * per row (warm cache; a cold one pays the disk I/O). It runs on an admin-only endpoint on demand
     * and once per crawl, never on a write path.</p>
     *
     * <p>Kept as <em>one</em> statement deliberately. Splitting it lets {@code COUNT(*)} alone drop to a
     * {@code Parallel Index Only Scan} (17&nbsp;ms), but the live half stays a sequential scan anyway —
     * nearly every row has a live version, so the index buys the planner nothing — and the two together
     * measured 41&nbsp;ms against 28&nbsp;ms for the combined form.</p>
     *
     * <p>Exact rather than the {@code pg_class.reltuples} estimate on purpose: the estimate drifts a few
     * points in either direction between {@code ANALYZE} runs, which surfaces as an indexed percentage slightly over
     * 100% and reads as a defect. With exact counts, 100% means complete and any excess is real —
     * documents in the index that no longer exist in the database.</p>
     *
     * <p>Failure is quiet: the indexed-percentage fields are omitted rather than failing the whole report.</p>
     */
    private static DatabaseCounts loadDatabaseCountsQuietly() {
        return Try.of(() -> {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL(DATABASE_COUNTS_SQL)
                    .loadObjectResults();
            if (rows.isEmpty()) {
                return null;
            }
            final Map<String, Object> row = rows.get(0);
            return new DatabaseCounts(positiveOrNull(row.get("working_count")),
                    positiveOrNull(row.get("live_count")));
        }).onFailure(e -> Logger.warn(ContentIndexMirrorReconciler.class,
                "Could not read the expected content counts for migration readiness: " + e.getMessage()))
                .getOrElse((DatabaseCounts) null);
    }

    /** The count as a positive Long, or {@code null} when it is absent, negative or zero. */
    private static Long positiveOrNull(final Object value) {
        final Long count = asLong(value);
        return count != null && count > 0 ? count : null;
    }

    /** JDBC hands a {@code COUNT} back as Long, BigDecimal or BigInteger depending on the driver. */
    private static Long asLong(final Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private static IndiciesInfo loadIndiciesQuietly() {
        return Try.of(() -> APILocator.getIndiciesAPI().loadIndicies())
                .onFailure(e -> Logger.warn(ContentIndexMirrorReconciler.class,
                        "Could not load content indices for migration readiness: " + e.getMessage()))
                .getOrNull();
    }
}
