package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.index.domain.TotalHits;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PaginatedArrayList;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the per-phase read contract of the five {@link ContentFactoryIndexOperations}
 * read operations that {@code ESContentFactoryImpl} funnels essentially all content search
 * through (issue #37413).
 *
 * <h2>What regressed</h2>
 * <p>{@code ESContentFactoryImpl} picked a provider with a bare ternary and called it directly,
 * so {@link PhaseRouter#read} — which holds the Phase 2 Elasticsearch fallback — was unreachable
 * from the busiest read path in the product. With OpenSearch down in Phase 2 and Elasticsearch
 * healthy and dual-written, {@code POST /api/content/_search} returned either a well-formed
 * {@code 200} with zero results (indistinguishable from "this content type has no content") or
 * a {@code 500}, instead of the result set Elasticsearch was holding the whole time.</p>
 *
 * <h2>Contract under test</h2>
 * <pre>
 * Phase │ Provider read │ On OpenSearch failure
 * ──────┼───────────────┼──────────────────────────────────────────────────────
 *   0   │ ES            │ n/a — OS is never contacted
 *   1   │ ES            │ n/a — OS is never contacted
 *   2   │ OS            │ logged at ERROR, retried against ES, ES result returned
 *   3   │ OS            │ propagates — ES is decommissioned, there is no fallback
 * </pre>
 *
 * <h2>Why this tests the router and not {@code ESContentFactoryImpl}</h2>
 * <p>{@code ESContentFactoryImpl} cannot be constructed outside a container: its read methods
 * run through static {@code CacheLocator}/{@code APILocator} calls — {@code indexCount} and
 * {@code indexSearch} both go through the static {@code translateQuery} — before they reach a
 * provider. So the per-phase contract is pinned here, against the real provider interface, and
 * the separate question of whether {@code ESContentFactoryImpl} actually consults the router is
 * proved by {@code ESContentFactoryImplPhase2FallbackTest} in {@code dotcms-integration}. A unit
 * test on the router cannot show which class calls it.</p>
 *
 * @author Fabrizzio Araya
 */
public class ContentFactoryIndexOperationsPhaseRoutingTest {

    /** The failure an unreachable node produces once the provider has wrapped it. */
    private static DotRuntimeException unreachable() {
        return new DotRuntimeException("An error occurred when executing the Lucene Query",
                new java.net.ConnectException("Connection refused"));
    }

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    private static PhaseRouter<ContentFactoryIndexOperations> router(
            final ContentFactoryIndexOperations es, final ContentFactoryIndexOperations os) {
        return new PhaseRouter<>(es, os);
    }

    // =========================================================================
    // indexCount — the operation that produced the 500s, because ContentHelper
    // runs the count before the search and an uncached count threw.
    // =========================================================================

    /**
     * Given : Phase 2, OpenSearch unreachable, Elasticsearch healthy and holding 184 documents.
     * When  : indexCount runs through the router.
     * Then  : the Elasticsearch count is returned — not 0, and not an exception.
     */
    @Test
    public void indexCount_phase2_osUnreachable_fallsBackToEs() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.indexCount(anyString())).thenThrow(unreachable());
        when(es.indexCount(anyString())).thenReturn(184L);

        final long count = router(es, os).read(impl -> impl.indexCount("+contentType:Profile"));

        assertEquals("Phase 2 must serve the count Elasticsearch holds, not 0", 184L, count);
        verify(os, times(1)).indexCount(anyString());
        verify(es, times(1)).indexCount(anyString());
    }

    /**
     * Given : Phase 3, OpenSearch unreachable.
     * When  : indexCount runs through the router.
     * Then  : the failure propagates and Elasticsearch is never contacted — it is decommissioned
     *         in Phase 3, so a silent fallback there would report data that is genuinely gone.
     */
    @Test
    public void indexCount_phase3_osUnreachable_propagatesAndNeverTouchesEs() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.indexCount(anyString())).thenThrow(unreachable());

        assertThrows(DotRuntimeException.class,
                () -> router(es, os).read(impl -> impl.indexCount("+contentType:Profile")));

        verifyNoInteractions(es);
    }

    /**
     * Given : Phase 0 and Phase 1, where Elasticsearch serves reads.
     * When  : indexCount runs through the router.
     * Then  : Elasticsearch answers and OpenSearch is never contacted — the routing change must
     *         be a pure pass-through outside Phase 2.
     */
    @Test
    public void indexCount_phases0And1_readEsAndNeverTouchOs() {
        for (final MigrationPhase phase : List.of(MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED,
                MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS)) {
            setPhase(phase);
            final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
            final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
            when(es.indexCount(anyString())).thenReturn(27L);

            final long count = router(es, os).read(impl -> impl.indexCount("+contentType:JobPosting"));

            assertEquals("In " + phase + " the count must come from Elasticsearch", 27L, count);
            verifyNoInteractions(os);
        }
    }

    // =========================================================================
    // searchHits — the operation behind indexSearch, and the one that produced
    // the silent 200/total=0 once the legacy layer swallowed the failure.
    // =========================================================================

    /**
     * Given : Phase 2, OpenSearch unreachable, Elasticsearch holding real hits.
     * When  : searchHits runs through the router.
     * Then  : the Elasticsearch hits are returned — the same object, so nothing substitutes an
     *         empty result on the way back.
     */
    @Test
    public void searchHits_phase2_osUnreachable_fallsBackToEs() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        final SearchHits esHits = SearchHits.builder().totalHits(TotalHits.builder().value(184L).build()).build();
        when(os.searchHits(anyString(), anyInt(), anyInt(), anyString())).thenThrow(unreachable());
        when(es.searchHits(anyString(), anyInt(), anyInt(), anyString())).thenReturn(esHits);

        final SearchHits hits = router(es, os)
                .read(impl -> impl.searchHits("+contentType:Profile +live:true", 7, 1, "title asc"));

        assertSame("Phase 2 must return the Elasticsearch hits untouched", esHits, hits);
        verify(es, times(1)).searchHits(anyString(), anyInt(), anyInt(), anyString());
    }

    /**
     * Given : Phase 3, OpenSearch unreachable.
     * When  : searchHits runs through the router.
     * Then  : the failure propagates; Elasticsearch is never contacted.
     */
    @Test
    public void searchHits_phase3_osUnreachable_propagates() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.searchHits(anyString(), anyInt(), anyInt(), anyString())).thenThrow(unreachable());

        assertThrows(DotRuntimeException.class, () -> router(es, os)
                .read(impl -> impl.searchHits("+contentType:Hero", 10, 0, "title asc")));

        verifyNoInteractions(es);
    }

    // =========================================================================
    // search — inode-only search, behind findContentletsByHost
    // =========================================================================

    /**
     * Given : Phase 2, OpenSearch unreachable, Elasticsearch holding two inodes.
     * When  : search runs through the router.
     * Then  : the Elasticsearch inodes are returned rather than an empty list.
     */
    @Test
    public void search_phase2_osUnreachable_fallsBackToEs() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.search(anyString(), anyInt(), anyInt())).thenThrow(unreachable());
        when(es.search(anyString(), anyInt(), anyInt())).thenReturn(List.of("inode-a", "inode-b"));

        final List<String> inodes = router(es, os)
                .read(impl -> impl.search("+conhost:48190c8c", 10, 0));

        assertEquals(List.of("inode-a", "inode-b"), inodes);
    }

    // =========================================================================
    // indexSearchScroll — drains the whole scroll internally, so a failure means
    // the operation produced nothing and can simply be re-run against ES.
    // =========================================================================

    /**
     * Given : Phase 2, OpenSearch unreachable, Elasticsearch able to serve the scroll.
     * When  : indexSearchScroll runs through the router.
     * Then  : the Elasticsearch result is returned. This site can fall back safely precisely
     *         because it materialises the entire scroll before returning — there is no cursor
     *         left half-drained on the failing engine.
     */
    @Test
    public void indexSearchScroll_phase2_osUnreachable_fallsBackToEs() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        final PaginatedArrayList<ContentletSearch> esResults = new PaginatedArrayList<>();
        esResults.setTotalResults(317L);
        when(os.indexSearchScroll(anyString(), anyString(), anyInt())).thenThrow(unreachable());
        when(es.indexSearchScroll(anyString(), anyString(), anyInt())).thenReturn(esResults);

        final PaginatedArrayList<ContentletSearch> results = router(es, os)
                .read(impl -> impl.indexSearchScroll("+contentType:Testimonials", "title asc", 1000));

        assertEquals(317L, results.getTotalResults());
        verify(es, times(1)).indexSearchScroll(anyString(), anyString(), anyInt());
    }

    /**
     * Given : Phase 3, OpenSearch unreachable.
     * When  : indexSearchScroll runs through the router.
     * Then  : the failure propagates.
     */
    @Test
    public void indexSearchScroll_phase3_osUnreachable_propagates() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.indexSearchScroll(anyString(), anyString(), anyInt())).thenThrow(unreachable());

        assertThrows(DotRuntimeException.class, () -> router(es, os)
                .read(impl -> impl.indexSearchScroll("+contentType:Testimonials", "title asc", 1000)));

        verifyNoInteractions(es);
    }

    // =========================================================================
    // createScrollQuery — routed for provider selection only.
    // =========================================================================

    /**
     * Given : Phase 2 and a working OpenSearch provider.
     * When  : createScrollQuery runs through the router.
     * Then  : the OpenSearch cursor is returned and Elasticsearch is never contacted.
     *
     * <p>This site is routed so that provider selection has exactly one mechanism in the class,
     * but its fallback can never fire and that is correct, not an oversight: the OpenSearch
     * implementation is {@code return new OSContentletScrollImpl(...)} — pure construction, no
     * I/O, nothing to throw. The requests happen later, inside the returned cursor, outside the
     * router. A mid-scroll fallback is impossible in principle anyway, because an OpenSearch
     * scroll id is meaningless to Elasticsearch, so a half-drained scroll cannot be resumed on
     * the other engine. The residual gap is documented at the call site.</p>
     */
    @Test
    public void createScrollQuery_phase2_selectsOsAndDoesNotTouchEs() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        final IndexContentletScroll osScroll = mock(IndexContentletScroll.class);
        when(os.createScrollQuery(anyString(), any(), anyBoolean(), anyInt(), anyString()))
                .thenReturn(osScroll);

        final IndexContentletScroll scroll = router(es, os).read(impl ->
                impl.createScrollQuery("+contentType:Profile", null, false, 100, "title asc"));

        assertSame(osScroll, scroll);
        verifyNoInteractions(es);
    }

    /**
     * Given : Phase 1, where Elasticsearch serves reads.
     * When  : createScrollQuery runs through the router.
     * Then  : the Elasticsearch cursor is returned and OpenSearch is never contacted.
     */
    @Test
    public void createScrollQuery_phase1_selectsEs() {
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        final IndexContentletScroll esScroll = mock(IndexContentletScroll.class);
        when(es.createScrollQuery(anyString(), any(), anyBoolean(), anyInt(), anyString()))
                .thenReturn(esScroll);

        final IndexContentletScroll scroll = router(es, os).read(impl ->
                impl.createScrollQuery("+contentType:Profile", null, false, 100, "title asc"));

        assertSame(esScroll, scroll);
        verifyNoInteractions(os);
    }

    // =========================================================================
    // The fallback must be a single attempt — a failing read costs one extra
    // call against ES, with no nesting and no retry storm.
    // =========================================================================

    /**
     * Given : Phase 2 with both engines failing on the same read.
     * When  : the read runs through the router.
     * Then  : the Elasticsearch failure surfaces to the caller, and each provider was called
     *         exactly once. A two-engine outage is the one case where a caller now sees an error
     *         where it previously saw an empty result — which is the point of the issue, since
     *         the silent-empty variant is the dangerous one.
     */
    @Test
    public void phase2_bothEnginesFail_surfacesErrorAfterExactlyOneAttemptEach() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.indexCount(anyString())).thenThrow(unreachable());
        when(es.indexCount(anyString())).thenThrow(unreachable());

        assertThrows(DotRuntimeException.class,
                () -> router(es, os).read(impl -> impl.indexCount("+contentType:Profile")));

        verify(os, times(1)).indexCount(anyString());
        verify(es, times(1)).indexCount(anyString());
    }

    /**
     * Given : Phase 2 and a healthy OpenSearch.
     * When  : the read succeeds.
     * Then  : Elasticsearch is never contacted. The fallback must cost nothing on the success
     *         path — otherwise Phase 2 would double every read in the product.
     */
    @Test
    public void phase2_osHealthy_neverTouchesEs() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.indexCount(anyString())).thenReturn(215L);

        final long count = router(es, os).read(impl -> impl.indexCount("+contentType:Hero"));

        assertEquals(215L, count);

        verify(es, never()).indexCount(anyString());
    }

    // =========================================================================
    // The fallback log line (AC-003).
    //
    // The pre-fix path logged these failures at WARN, which is why an outage was
    // invisible to log-based monitoring even though the design documents it as the
    // early-warning signal. Level is therefore part of the contract, not cosmetic:
    // a test that only checked "something was logged" would pass against the old
    // behaviour.
    // =========================================================================

    /** Captures events published to a single log4j2 logger. */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        CapturingAppender() {
            super("capture-phase-router", null, null, true, null);
        }

        @Override
        public void append(final LogEvent event) {
            events.add(event.toImmutable());
        }
    }

    private CapturingAppender attachAppender() {
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        final org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger(PhaseRouter.class);
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(final CapturingAppender appender) {
        final org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger(PhaseRouter.class);
        logger.removeAppender(appender);
        appender.stop();
    }

    /**
     * Given : Phase 2 and an unreachable OpenSearch.
     * When  : a named read falls back.
     * Then  : exactly one event is logged, at ERROR, naming both the operation that failed and
     *         the cause, with the throwable attached. One event per read — not per provider, and
     *         with no retry storm behind it.
     */
    @Test
    public void phase2_fallback_logsOnceAtErrorNamingOperationAndCause() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.indexCount(anyString())).thenThrow(unreachable());
        when(es.indexCount(anyString())).thenReturn(184L);

        final CapturingAppender appender = attachAppender();
        try {
            router(es, os).read("indexCount", impl -> impl.indexCount("+contentType:Profile"));
        } finally {
            detachAppender(appender);
        }

        assertEquals("A fallback must log exactly one event per read", 1, appender.events.size());
        final LogEvent event = appender.events.get(0);
        assertEquals("The fallback must be ERROR — WARN is what monitoring was missing",
                Level.ERROR, event.getLevel());
        final String message = event.getMessage().getFormattedMessage();
        assertTrue("The log line must name the failing operation, so monitoring can say what "
                        + "stopped working. Was: " + message,
                message.contains("indexCount"));
        assertTrue("The log line must carry the cause. Was: " + message,
                message.contains("Connection refused"));
        assertNotNull("The throwable must be attached so the stack identifies the call site",
                event.getThrown());
    }

    /**
     * Given : Phase 3 and an unreachable OpenSearch.
     * When  : the read propagates instead of falling back.
     * Then  : no fallback event is logged. A fallback log line in Phase 3 would be a false
     *         signal — there is no fallback there, and Elasticsearch is decommissioned.
     */
    @Test
    public void phase3_failure_logsNoFallbackEvent() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
        final ContentFactoryIndexOperations es = mock(ContentFactoryIndexOperations.class);
        final ContentFactoryIndexOperations os = mock(ContentFactoryIndexOperations.class);
        when(os.indexCount(anyString())).thenThrow(unreachable());

        final CapturingAppender appender = attachAppender();
        try {
            assertThrows(DotRuntimeException.class, () -> router(es, os)
                    .read("indexCount", impl -> impl.indexCount("+contentType:Profile")));
        } finally {
            detachAppender(appender);
        }

        assertEquals("Phase 3 must not report a fallback", 0, appender.events.size());
    }
}
