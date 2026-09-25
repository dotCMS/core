package com.dotcms.content.index.migration;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ContentletIndexOperationsES;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Content-index half of the migration-readiness report (issue #36360): compares the active versioned
 * content indices (working and live) against their {@code .os} counterparts across both engines,
 * mirroring {@link SiteSearchMirrorReconciler} but for the content store. Never mutates anything.
 *
 * <h4>Where the active index names come from</h4>
 * <p>Each engine's physical name is read from the store that owns it: {@code IndiciesInfo} for
 * Elasticsearch (its backing {@code indicies} rows are the ones with {@code index_version IS NULL})
 * and {@code VersionedIndices} for OpenSearch.</p>
 *
 * <p>Up to Phase 2 the two are kept on one generation by construction, so the OpenSearch name is just
 * the Elasticsearch one plus the {@code .os} tag and only the Elasticsearch store needs reading. From
 * Phase 3 on that stops holding: writes go to OpenSearch alone, so a reindex advances OpenSearch to a
 * newly built pair while Elasticsearch keeps naming the index it held at cutover — an index that
 * survives indefinitely, since deletion in that phase routes to OpenSearch only. Deriving one name
 * from the other there would report "Elasticsearch has no copy" for an index that exists and holds
 * content, so both stores are read and the generation split is reported as what it is (issue #37635).</p>
 *
 * <p><strong>Existence</strong> comes from each engine leaf's {@code getIndicesStatsOrThrow()} — one call per
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
    private final Supplier<Optional<VersionedIndices>> versionedIndicesSupplier;
    private final Supplier<DatabaseCounts> databaseCountsSupplier;

    public ContentIndexMirrorReconciler() {
        this(new ESIndexAPI(), CDIUtils.getBeanThrows(OSIndexAPIImpl.class),
                new ContentletIndexOperationsES(),
                CDIUtils.getBeanThrows(ContentletIndexOperationsOS.class),
                ContentIndexMirrorReconciler::loadIndicies,
                ContentIndexMirrorReconciler::loadVersionedIndices,
                ContentIndexMirrorReconciler::loadDatabaseCountsQuietly);
    }

    @VisibleForTesting
    ContentIndexMirrorReconciler(final IndexAPI esImpl, final IndexAPI osImpl,
            final ContentletIndexOperations esOps, final ContentletIndexOperations osOps,
            final Supplier<IndiciesInfo> indiciesSupplier,
            final Supplier<Optional<VersionedIndices>> versionedIndicesSupplier,
            final Supplier<DatabaseCounts> databaseCountsSupplier) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
        this.esOps = esOps;
        this.osOps = osOps;
        this.indiciesSupplier = indiciesSupplier;
        this.versionedIndicesSupplier = versionedIndicesSupplier;
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

    /**
     * The content half of the report, together with the reason it is empty when it is.
     *
     * <p>An empty {@code statuses} list on its own is ambiguous: it means either "the store was read
     * and holds no active pointers" or "the store could not be read at all". Those call for opposite
     * operator actions — reindex to recreate the indices, versus fix whatever broke the read — so
     * collapsing them into one value would hand the operator a confident instruction derived from an
     * unknown (issue #37635). {@code unreadableReason} is present only in the second case.</p>
     *
     * <p>{@code unreachableEngines} is the per-engine counterpart of {@code unreadableReason}: the
     * pointers were read, but one engine's index stats could not be, so its side of every row is
     * unknown while the other side is still reported (issue #37636).</p>
     *
     * @param statuses           per-index mirror status; empty when no active pointers were resolved
     * @param unreadableReason   why the store could not be read, when that is what happened
     * @param unreachableEngines engine name → why that engine could not be read; empty when both were
     */
    public record ContentMirrors(List<MirrorStatus> statuses, Optional<String> unreadableReason,
            Map<String, String> unreachableEngines) {

        /** Both engines were read (or never needed to be). */
        public ContentMirrors(final List<MirrorStatus> statuses,
                final Optional<String> unreadableReason) {
            this(statuses, unreadableReason, Map.of());
        }
    }

    /**
     * Per-index mirror status for the active working and live content indices.
     *
     * <p>Kept for callers that only need the rows and have no use for the distinction
     * {@link #mirrors()} draws.</p>
     */
    public List<MirrorStatus> statuses() {
        return mirrors().statuses();
    }

    /** Per-index mirror status, plus why it came back empty when it did. */
    public ContentMirrors mirrors() {
        final PointerLookup lookup = activePointers();
        if (lookup.failure() != null) {
            return new ContentMirrors(List.of(), Optional.of(lookup.failure()));
        }
        if (lookup.hasNoPointers()) {
            return new ContentMirrors(List.of(), Optional.empty());
        }
        // One engine-wide stats call per engine. Either can fail on its own — the old cluster retired
        // at Phase 3, or any outage — and the other engine's half of the report is still worth
        // having, so a failure marks that engine unreachable instead of escaping (issue #37636).
        final Map<String, String> unreachable = new LinkedHashMap<>();
        final Map<String, IndexStats> esStats =
                statsOrUnreachable(esImpl, MirrorStatus.ELASTICSEARCH, unreachable);
        final Map<String, IndexStats> osStats =
                statsOrUnreachable(osImpl, MirrorStatus.OPENSEARCH, unreachable);
        return new ContentMirrors(statusesFor(lookup, esStats, osStats, unreachable),
                Optional.empty(), Map.copyOf(unreachable));
    }

    /**
     * One engine's index stats, or {@code null} after recording why that engine could not be read.
     */
    private static Map<String, IndexStats> statsOrUnreachable(final IndexAPI engine,
            final String engineName, final Map<String, String> unreachable) {
        try {
            // The propagating variant: the default OpenSearch getIndicesStats() answers an outage with
            // an empty map, which would read here as "no copies" and prescribe a reindex.
            return engine.getIndicesStatsOrThrow();
        } catch (Exception e) {
            final String reason = MirrorStatus.reasonOf(e);
            Logger.warn(ContentIndexMirrorReconciler.class, engineName
                    + " could not be reached for migration readiness; reporting its side of the "
                    + "content indices as unavailable: " + reason, e);
            unreachable.put(engineName, reason);
            return null;
        }
    }

    private List<MirrorStatus> statusesFor(final PointerLookup pointers,
            final Map<String, IndexStats> esStats, final Map<String, IndexStats> osStats,
            final Map<String, String> unreachable) {
        final DatabaseCounts dbCounts = databaseCountsSupplier.get();
        final List<MirrorStatus> out = new ArrayList<>(2);
        addStatus(out, IndexKind.CONTENT_WORKING, pointers.working(), esStats, osStats, unreachable,
                dbCounts == null ? null : dbCounts.working());
        addStatus(out, IndexKind.CONTENT_LIVE, pointers.live(), esStats, osStats, unreachable,
                dbCounts == null ? null : dbCounts.live());
        return out;
    }

    /**
     * One slot's physical index name on each engine, exactly as its own store records it: the
     * Elasticsearch name cluster-prefixed and un-tagged, the OpenSearch name additionally carrying
     * {@code .os}. Either may be {@code null} when that engine has no pointer for the slot.
     *
     * <p>Two independent names rather than one derived from the other, because after a Phase 3
     * reindex they are <em>not</em> the same generation: OpenSearch advances to the newly built pair
     * while Elasticsearch keeps naming the index it held at cutover. Deriving one from the other
     * would report "Elasticsearch has no copy" for an index that exists and holds content — the
     * divergence has to be followed, not assumed away (issue #37635).</p>
     */
    private record SlotPointers(String es, String os) {

        boolean isUnset() {
            return !UtilMethods.isSet(es) && !UtilMethods.isSet(os);
        }
    }

    /**
     * The outcome of reading the index stores: one {@link SlotPointers} per slot, or the reason a
     * read failed.
     *
     * <p>Three distinguishable outcomes, which is the whole point of the type: pointers present;
     * no pointers (read fine, nothing registered); or {@code failure} set (a store could not be read,
     * so nothing at all is known).</p>
     */
    private record PointerLookup(SlotPointers working, SlotPointers live, String failure) {

        static PointerLookup of(final SlotPointers working, final SlotPointers live) {
            return new PointerLookup(working, live, null);
        }

        static PointerLookup failed(final String reason) {
            return new PointerLookup(null, null, reason);
        }

        boolean hasNoPointers() {
            return working.isUnset() && live.isUnset();
        }
    }

    /**
     * Reads the active pointers for both engines.
     *
     * <p>Before Phase 3 the two engines are kept on one generation by construction, so the single
     * Elasticsearch name yields the OpenSearch one by tagging. From Phase 3 on they can diverge, so
     * each engine's name is read from its own store — see {@link SlotPointers}. The Elasticsearch
     * store may legitimately have nothing there: an installation that reindexed at Phase 3 on a build
     * that still purged those rows has lost the name for good, and the report then says the copy is
     * absent, which at that point is all that can honestly be said.</p>
     *
     * <p>A store read that throws is reported as a failure rather than as an empty store: the loaders
     * propagate so the policy lives here, in the one place that knows the difference matters. Either
     * store failing is fatal to the report — half a comparison would be presented as a whole one.</p>
     */
    private PointerLookup activePointers() {
        final IndiciesInfo info;
        try {
            info = indiciesSupplier.get();
        } catch (Exception e) {
            return readFailure("the Elasticsearch index store", e);
        }
        final String esWorking = info == null ? null : info.getWorking();
        final String esLive = info == null ? null : info.getLive();

        if (!IndexConfigHelper.isMigrationComplete()) {
            return PointerLookup.of(
                    new SlotPointers(esWorking, tagOrNull(esWorking)),
                    new SlotPointers(esLive, tagOrNull(esLive)));
        }

        final Optional<VersionedIndices> versioned;
        try {
            versioned = versionedIndicesSupplier.get();
        } catch (Exception e) {
            return readFailure("the OpenSearch index store", e);
        }
        return PointerLookup.of(
                new SlotPointers(esWorking,
                        versioned.flatMap(VersionedIndices::working).orElse(null)),
                new SlotPointers(esLive,
                        versioned.flatMap(VersionedIndices::live).orElse(null)));
    }

    private static String tagOrNull(final String esName) {
        return UtilMethods.isSet(esName) ? IndexTag.OS.tag(esName) : null;
    }

    /** Logs the store read failure and turns it into the reason carried back to the operator. */
    private static PointerLookup readFailure(final String store, final Exception e) {
        Logger.warn(ContentIndexMirrorReconciler.class,
                "Could not read " + store + " for migration readiness: " + e.getMessage(), e);
        return PointerLookup.failed(store + " could not be read: " + e.getMessage());
    }

    /**
     * Adds the row for one slot. A {@code null} stats map means that engine could not be read: its
     * side is reported as unavailable (existence unknown, no count query sent) and the row's verdict
     * is {@link Verdict#UNMEASURED}, so nothing downstream mistakes "unknown" for "missing".
     */
    private void addStatus(final List<MirrorStatus> out, final IndexKind kind,
            final SlotPointers pointers, final Map<String, IndexStats> esStats,
            final Map<String, IndexStats> osStats, final Map<String, String> unreachable,
            final Long databaseDocCount) {
        if (pointers.isUnset()) {
            return;
        }
        // Each store records the full physical name for its own engine. The stats maps are keyed by
        // the cluster-stripped form (Elasticsearch un-tagged, OpenSearch carrying .os), so strip for
        // the lookup; the count query takes the physical name as stored.
        final String esBare = pointers.es() == null ? null : esImpl.removeClusterIdFromName(pointers.es());
        final String osBare = pointers.os() == null ? null : esImpl.removeClusterIdFromName(pointers.os());

        // Existence from the stats snapshot; the count from a live count query (see class javadoc).
        final boolean esExists = esStats != null && esBare != null && esStats.containsKey(esBare);
        final long esCount = esExists ? countQuietly(esOps, pointers.es()) : 0L;
        final boolean osExists = osStats != null && osBare != null && osStats.containsKey(osBare);
        final long osCount = osExists ? countQuietly(osOps, pointers.os()) : 0L;

        // The row is named after the engine that owns the content in this phase, with the .os tag
        // stripped so the name stays the logical one either way. Each engine's own physical name is
        // reported alongside its copy, so a generation split is visible rather than flattened.
        final String name = IndexTag.strip(
                IndexConfigHelper.isMigrationComplete() && osBare != null ? osBare : esBare);

        final MirrorStatus.EngineCopy esCopy = esStats == null
                ? MirrorStatus.EngineCopy.unavailable(pointers.es(), null,
                        unreachable.get(MirrorStatus.ELASTICSEARCH))
                : new MirrorStatus.EngineCopy(esExists, esCount, pointers.es());
        final MirrorStatus.EngineCopy osCopy = osStats == null
                ? MirrorStatus.EngineCopy.unavailable(pointers.os(), null,
                        unreachable.get(MirrorStatus.OPENSEARCH))
                : new MirrorStatus.EngineCopy(osExists, osCount, pointers.os());

        final Verdict verdict = MirrorStatus.verdictFor(esCopy, osCopy);
        final String recommendation = (verdict == Verdict.UNMEASURED
                ? MirrorStatus.unmeasuredAdvice("content index", name, esCopy, osCopy)
                : recommend(name, verdict, osExists))
                + incompleteNote("Elasticsearch", esExists, esCount, databaseDocCount)
                + incompleteNote("OpenSearch", osExists, osCount, databaseDocCount);
        out.add(new MirrorStatus(name, kind, esCopy, osCopy, verdict, recommendation,
                databaseDocCount));
    }

    /**
     * Exact document count of {@code physicalName} on one engine, or {@code -1} when the query fails.
     *
     * <p>Takes the physical name as the store records it rather than re-deriving it through
     * {@code toPhysicalName}: from Phase 3 on the two engines can name different generations, so a
     * name derived for one engine from the other's would count the wrong index — or none
     * (issue #37635).</p>
     *
     * <p>Failures are reported as {@code -1} rather than propagated: a readiness report that answers
     * "unknown" for one engine is useful, one that returns a 500 is not. {@code -1} is the established
     * unmeasurable marker — it compares unequal, so the verdict degrades to out-of-sync and
     * {@code safeToRollback} to false, never to a false green.</p>
     */
    private static long countQuietly(final ContentletIndexOperations ops, final String physicalName) {
        return Try.of(() -> ops.getIndexDocumentCount(physicalName))
                .onFailure(e -> Logger.warn(ContentIndexMirrorReconciler.class,
                        "Could not count documents of '" + physicalName + "' on "
                                + ops.getClass().getSimpleName() + ": " + e.getMessage()))
                .getOrElse(-1L);
    }

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
        if (indexedPercent >= MirrorStatus.INCOMPLETE_INDEXED_THRESHOLD) {
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

    /**
     * The Elasticsearch index store — the source of the active pointers before Phase 3.
     *
     * <p>Propagates rather than swallowing: a read that threw is a different fact from a store that
     * holds nothing, and only {@link #activePointers()} can act on the difference. Returning
     * {@code null} for both would make the report tell an operator to reindex when the real problem
     * is that the database could not be read (issue #37635).</p>
     */
    private static IndiciesInfo loadIndicies() {
        return Try.of(() -> APILocator.getIndiciesAPI().loadIndicies())
                .getOrElseThrow(e -> new DotRuntimeException(
                        "Could not load content indices for migration readiness", e));
    }

    /** The OpenSearch index store — the Phase 3 source of the active pointers. Propagates, as above. */
    private static Optional<VersionedIndices> loadVersionedIndices() {
        return Try.of(() -> APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices())
                .getOrElseThrow(e -> new DotRuntimeException(
                        "Could not load the OpenSearch index store for migration readiness", e));
    }
}
