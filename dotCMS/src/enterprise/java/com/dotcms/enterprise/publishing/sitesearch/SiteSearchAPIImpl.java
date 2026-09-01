/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.sitesearch;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.PhaseRouter;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.DotSearchException;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotcms.content.model.annotation.IndexRouter;
import com.dotcms.content.model.annotation.IndexRouter.IndexAccess;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.quartz.SchedulerException;

/**
 * Phase-aware router implementation of {@link SiteSearchAPI}.
 *
 * <p>Single entry point for Site Search during the Elasticsearch → OpenSearch migration. It owns no
 * business logic — every call is delegated to the active provider(s) chosen by {@link PhaseRouter}
 * according to the migration phase, mirroring {@link com.dotcms.content.index.IndexAPIImpl}.</p>
 *
 * <pre>
 * Phase                     | Read provider | Write providers
 * --------------------------|---------------|-----------------
 * 0 — not started           | ES            | [ES]
 * 1 — dual-write, ES reads  | ES            | [ES, OS]
 * 2 — dual-write, OS reads  | OS            | [ES, OS]
 * 3 — OS only               | OS            | [OS]
 * </pre>
 *
 * <h2>Why this router is the single fan-out point</h2>
 * <p>{@link ESSiteSearchAPI} and {@link OSSiteSearchAPI} each talk to their own vendor's index API
 * directly ({@code ESIndexAPI} / {@code OSIndexAPIImpl}) rather than the neutral {@code IndexAPI}
 * router. If they used the neutral router, a write here would fan out twice (once per provider, each
 * of which would itself dual-write), creating duplicate indices. Routing happens in exactly one place:
 * here.</p>
 *
 * <h2>Routing categories</h2>
 * <ul>
 *   <li><strong>Document/index reads</strong> ({@code search}, {@code getFromIndex},
 *       {@code getAggregations}, {@code getFacets}, {@code isDefaultIndex}) → read provider.</li>
 *   <li><strong>Document/index writes</strong> ({@code putToIndex}, {@code deleteFromIndex},
 *       {@code createSiteSearchIndex}, {@code setAlias}, {@code activateIndex},
 *       {@code deactivateIndex}, {@code deleteOldSiteSearchIndices}) → write fan-out.</li>
 *   <li><strong>Aggregating reads</strong> ({@code listIndices}, {@code listClosedIndices}) → in
 *       dual-write phases the two providers each own a distinct physical index set, so results are
 *       merged (deduplicated) rather than selecting one provider.</li>
 *   <li><strong>Quartz scheduling</strong> ({@code scheduleTask}, {@code deleteTask},
 *       {@code pauseTask}, {@code executeTaskNow}, {@code getTasks}, {@code getTask},
 *       {@code getTaskProgress}, {@code isTaskRunning}) → these touch the shared Quartz scheduler,
 *       NOT a search backend. They are routed to a single provider so a job is never scheduled twice.
 *       The job itself, when it runs, calls {@code putToIndex} through this router and therefore still
 *       dual-writes documents.</li>
 * </ul>
 *
 * @author Fabrizio Araya
 * @see PhaseRouter
 * @see ESSiteSearchAPI
 * @see OSSiteSearchAPI
 */
@IndexLibraryIndependent
@IndexRouter(access = {IndexAccess.READ, IndexAccess.WRITE})
public class SiteSearchAPIImpl implements SiteSearchAPI {

    private final SiteSearchAPI esImpl;
    private final SiteSearchAPI osImpl;
    private final PhaseRouter<SiteSearchAPI> router;

    public SiteSearchAPIImpl() {
        this(new ESSiteSearchAPI(), CDIUtils.getBeanThrows(OSSiteSearchAPI.class));
    }

    /**
     * Package-private constructor for testing.
     */
    SiteSearchAPIImpl(final SiteSearchAPI esImpl, final SiteSearchAPI osImpl) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
        this.router = new PhaseRouter<>(esImpl, osImpl);
    }

    // -------------------------------------------------------------------------
    // Aggregating reads — merge both providers in dual-write phases
    // -------------------------------------------------------------------------

    @Override
    public List<String> listIndices() {
        final List<SiteSearchAPI> providers = router.writeProviders();
        if (providers.size() == 1) {
            return providers.getFirst().listIndices();
        }
        final Set<String> merged = new LinkedHashSet<>(esImpl.listIndices());
        merged.addAll(osImpl.listIndices());
        return new ArrayList<>(merged);
    }

    @Override
    public List<String> listClosedIndices() {
        final List<SiteSearchAPI> providers = router.writeProviders();
        if (providers.size() == 1) {
            return providers.getFirst().listClosedIndices();
        }
        final Set<String> merged = new LinkedHashSet<>(esImpl.listClosedIndices());
        merged.addAll(osImpl.listClosedIndices());
        return new ArrayList<>(merged);
    }

    /**
     * True only when {@code indexName} exists on <em>every</em> engine that receives writes in the
     * current phase — the incremental-crawl safety gate (see {@link SiteSearchAPI#existsOnAllWriteEngines}).
     * Aggregates the per-engine leaf checks across {@link PhaseRouter#writeProviders()}: a single
     * missing twin makes an in-place incremental write unsafe, so the caller must rebuild fully.
     */
    @Override
    public boolean existsOnAllWriteEngines(final String indexName) {
        for (final SiteSearchAPI impl : router.writeProviders()) {
            if (!impl.existsOnAllWriteEngines(indexName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * True only when the index is present on every current write engine AND their document counts
     * match — the incremental-crawl gate (see {@link SiteSearchAPI#writeMirrorsInSync}). Existence is
     * checked first (a missing twin is out of sync); then each write provider's own document count is
     * compared. A single write engine (Phases 0/3) has nothing to compare, so it is trivially in sync.
     *
     * <p>The per-engine count comes from each leaf's {@link SiteSearchAPI#documentCount(String)} — an
     * exact count (not a plain {@code search} total, which is capped at 10,000 and would hide drift on
     * large indices). Any drift — including a shadow write that failed fire-and-forget on OpenSearch —
     * makes this {@code false} so the caller rebuilds fully instead of layering another incremental delta
     * on top of divergent copies. A count that fails on any engine ({@code -1}) is treated as out of sync
     * so an unknown state is never mistaken for "in sync" (issue #36360).</p>
     */
    @Override
    public boolean writeMirrorsInSync(final String indexName) {
        final List<SiteSearchAPI> providers = router.writeProviders();
        if (providers.size() < 2) {
            return true; // single write engine — no mirror to reconcile
        }
        if (!existsOnAllWriteEngines(indexName)) {
            return false; // a twin is missing
        }
        long expected = -1L;
        for (final SiteSearchAPI provider : providers) {
            final long count = provider.documentCount(indexName);
            if (count < 0L) {
                // a count query failed on this engine — the real state is unknown, so fail safe to a
                // full rebuild rather than let a 0==0 (both-failed) match permit an incremental (#36360)
                return false;
            }
            if (expected < 0L) {
                expected = count;
            } else if (count != expected) {
                return false; // document counts diverge across engines → content drift
            }
        }
        return true;
    }

    /**
     * Router: the exact document count from the current read provider (ES in Phases 0/1, OS in Phases
     * 2/3). The mirror parity gate does not use this router method — it counts each write provider leaf
     * directly — but the neutral surface exposes it for a single-engine caller (see
     * {@link SiteSearchAPI#documentCount}).
     */
    @Override
    public long documentCount(final String indexName) {
        return router.read(provider -> provider.documentCount(indexName));
    }

    /**
     * Phase-aware alias resolution. Delegated to the current read provider (ES in Phases 0/1, OS in
     * Phases 2/3, with the Phase-2 ES fallback of {@link PhaseRouter#read}) — each engine resolves
     * aliases against its own physical names (ES plain, OS {@code .os}-tagged) and returns logical
     * names, so the map is correct in every phase without this router touching the {@code .os} tag.
     */
    @Override
    public Map<String, String> getAliasToIndexMap() {
        return router.read(SiteSearchAPI::getAliasToIndexMap);
    }

    /**
     * Router: alias resolution over the SAME provider set {@link #listIndices()} uses, so every listed
     * index can show its alias — the management/display view (issue #36983).
     *
     * <p>Deliberately NOT the read provider alone. The list is a union in the dual-write phases, so an
     * index living only on the other engine would otherwise render with a blank alias: Phase 2 + a
     * Phase-0 (ES-only) index, or Phase 1 + a Phase-3 (OS-only) index after a downgrade. Merging over
     * the write providers keeps the alias view and the index list exactly in step.</p>
     *
     * <p>The read provider is applied last so it wins any collision: if the two engines resolve one
     * alias to different logical indices (a mirror desync), the map agrees with what a search would
     * actually hit. In the single-provider phases (0 and 3) there is nothing to merge.</p>
     */
    @Override
    public Map<String, String> getAliasToIndexMapAllEngines() {
        final List<SiteSearchAPI> providers = router.writeProviders();
        if (providers.size() == 1) {
            return providers.getFirst().getAliasToIndexMap();
        }
        final SiteSearchAPI readProvider = router.readProvider();
        final Map<String, String> merged = new LinkedHashMap<>();
        providers.stream().filter(provider -> provider != readProvider)
                .forEach(provider -> merged.putAll(provider.getAliasToIndexMap()));
        merged.putAll(readProvider.getAliasToIndexMap()); // last write wins → read provider
        return merged;
    }

    // -------------------------------------------------------------------------
    // Reads — read provider
    // -------------------------------------------------------------------------

    @Override
    public SiteSearchResults search(final String query, final int start, final int rows) {
        return router.read(impl -> impl.search(query, start, rows));
    }

    @Override
    public SiteSearchResults search(final String indexName, final String query, final int start,
            final int rows) {
        return router.read(impl -> impl.search(indexName, query, start, rows));
    }

    @Override
    public SiteSearchResult getFromIndex(final String index, final String id) {
        return router.read(impl -> impl.getFromIndex(index, id));
    }

    /**
     * Router: the default site-search index according to the current read provider — Elasticsearch's
     * legacy pointer in Phases 0/1, OpenSearch's {@code VersionedIndices} (with a legacy fallback) in
     * Phases 2/3. Reading the legacy pointer directly goes stale from Phase 3 on, where
     * {@code activateIndex} fans out to OpenSearch alone (issue #36983).
     */
    @Override
    public Optional<String> defaultIndexName() throws DotDataException {
        try {
            return router.readChecked(SiteSearchAPI::defaultIndexName);
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isDefaultIndex(final String indexName) throws DotDataException {
        try {
            return router.readChecked(impl -> impl.isDefaultIndex(indexName));
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Aggregation> getAggregations(final String indexName, final String query)
            throws DotDataException {
        try {
            return router.readChecked(impl -> impl.getAggregations(indexName, query));
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Aggregation> getFacets(final String indexName, final String query)
            throws DotDataException {
        try {
            return router.readChecked(impl -> impl.getFacets(indexName, query));
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Writes — fan out to all active write providers
    // -------------------------------------------------------------------------

    @Override
    public boolean createSiteSearchIndex(final String indexName, final String alias, final int shards)
            throws DotSearchException, IOException {
        try {
            return router.writeReturningChecked(
                    impl -> impl.createSiteSearchIndex(indexName, alias, shards));
        } catch (DotSearchException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public boolean setAlias(final String indexName, final String alias) {
        return router.writeBoolean(impl -> impl.setAlias(indexName, alias));
    }

    @Override
    public void activateIndex(final String indexName) throws DotDataException {
        try {
            router.writeChecked(impl -> impl.activateIndex(indexName));
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public void deactivateIndex(final String indexName) throws DotDataException, IOException {
        try {
            router.writeChecked(impl -> impl.deactivateIndex(indexName));
        } catch (DotDataException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public void putToIndex(final String idx, final SiteSearchResult res, final String resultType) {
        // Each provider gets its own copy: putToIndex mutates the result's backing map
        // (e.g. SiteSearchResult.setKeywords rewrites the "keywords" entry String -> List), so a
        // shared instance would let the first provider in the fan-out corrupt the input the next
        // provider reads — producing a ClassCastException on the second leaf. The lambda is invoked
        // once per provider, so copyOf(res) is evaluated fresh from the untouched original each time.
        router.write(impl -> impl.putToIndex(idx, copyOf(res), resultType));
    }

    @Override
    public void putToIndex(final String idx, final List<SiteSearchResult> res, final String resultType) {
        // See single-result overload: copy per provider so the fan-out never shares mutable state.
        router.write(impl -> impl.putToIndex(idx, copyOf(res), resultType));
    }

    /**
     * Shallow-copies a {@link SiteSearchResult} so the fan-out can hand an independent instance to
     * each write provider. {@code putToIndex} mutates the backing map in place (HTML stripping,
     * description derivation, {@code keywords} String→List rewrite); copying the map prevents one
     * provider's mutations from leaking into the next provider's input. A shallow map copy is
     * sufficient because every mutation replaces a map entry rather than mutating a value object.
     */
    private static SiteSearchResult copyOf(final SiteSearchResult res) {
        return new SiteSearchResult(new HashMap<>(res.getMap()));
    }

    /** Copies each element of a result batch — see {@link #copyOf(SiteSearchResult)}. */
    private static List<SiteSearchResult> copyOf(final List<SiteSearchResult> results) {
        final List<SiteSearchResult> copies = new ArrayList<>(results.size());
        for (final SiteSearchResult r : results) {
            copies.add(copyOf(r));
        }
        return copies;
    }

    @Override
    public void deleteFromIndex(final String idx, final String docId) {
        router.write(impl -> impl.deleteFromIndex(idx, docId));
    }

    @Override
    public void deleteOldSiteSearchIndices() {
        router.write(SiteSearchAPI::deleteOldSiteSearchIndices);
    }

    @Override
    public void deleteIndex(final String indexName) throws DotDataException, IOException {
        // Guard once, phase-aware: the active (default) site-search index cannot be deleted —
        // deactivate it first. isDefaultIndex reads the pointer from the phase-appropriate store,
        // so this reproduces the single-index UX (issue #35640).
        if (isDefaultIndex(indexName)) {
            throw new DotStateException("Site-search index '" + indexName
                    + "' is active and cannot be deleted. Deactivate it first.");
        }
        // A site-search index is one logical index mirrored across both engines, so a delete must
        // clear it from BOTH — not only the current phase's write providers. A phase rollback can
        // leave a twin on an engine that is no longer in the write set (e.g. a Phase-2 OpenSearch
        // twin after rolling back to Phase 0/1, or an ES index after Phase 3); a phase-scoped delete
        // would strand it as an unmanageable orphan. The primary (current read provider) delete is
        // authoritative and its failure propagates; the other engine is swept best-effort so an
        // unreachable or decommissioned engine never blocks the delete. Each leaf delete is already
        // idempotent — it skips when the index is absent (issue #36360).
        final SiteSearchAPI primary = router.readProvider();
        final SiteSearchAPI secondary = primary == esImpl ? osImpl : esImpl;
        try {
            primary.deleteIndex(indexName);
        } catch (DotDataException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            secondary.deleteIndex(indexName);
        } catch (final Exception e) {
            Logger.warn(SiteSearchAPIImpl.class, String.format(
                    "Best-effort delete of site-search index '%s' on the non-primary engine failed "
                            + "(a leftover twin may remain; reconcile if the engine comes back): %s",
                    indexName, e.getMessage()), e);
        }
    }

    // -------------------------------------------------------------------------
    // Quartz scheduling — single provider (shared scheduler; never fan out)
    // -------------------------------------------------------------------------

    @Override
    public List<ScheduledTask> getTasks() throws SchedulerException {
        return router.readProvider().getTasks();
    }

    @Override
    public ScheduledTask getTask(final String taskName) throws SchedulerException {
        return router.readProvider().getTask(taskName);
    }

    @Override
    public void scheduleTask(final SiteSearchConfig config)
            throws SchedulerException, ParseException, ClassNotFoundException {
        router.readProvider().scheduleTask(config);
    }

    @Override
    public void deleteTask(final String taskName) throws SchedulerException {
        router.readProvider().deleteTask(taskName);
    }

    @Override
    public void pauseTask(final String taskName) throws SchedulerException {
        router.readProvider().pauseTask(taskName);
    }

    @Override
    public SiteSearchPublishStatus getTaskProgress(final String jobName) throws SchedulerException {
        return router.readProvider().getTaskProgress(jobName);
    }

    @Override
    public boolean isTaskRunning(final String jobName) throws SchedulerException {
        return router.readProvider().isTaskRunning(jobName);
    }

    @Override
    public void executeTaskNow(final SiteSearchConfig config)
            throws SchedulerException, ParseException, ClassNotFoundException {
        router.readProvider().executeTaskNow(config);
    }
}
