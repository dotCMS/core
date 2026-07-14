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
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        try {
            router.writeChecked(impl -> impl.deleteIndex(indexName));
        } catch (DotDataException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
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
