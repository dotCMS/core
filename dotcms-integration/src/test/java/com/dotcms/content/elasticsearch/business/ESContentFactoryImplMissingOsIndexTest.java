package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.index.opensearch.ContentFactoryIndexOperationsOS;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.content.index.opensearch.OSQueryCache;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import java.util.Optional;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.SearchRequest;

/**
 * Integration tests for the second failure class behind
 * <a href="https://github.com/dotCMS/core/issues/37413">#37413</a>: an OpenSearch node that is
 * <em>reachable</em> but answers with an error — chiefly a missing counterpart index (AC-005),
 * which also covers the reactivated pre-migration backup index scenario.
 *
 * <h2>Why this is a separate failure class from the routing fix</h2>
 * <p>Routing the read path through the phase router fixes an <em>unreachable</em> OpenSearch: a
 * connection failure reaches the provider's generic handler and is rethrown as a runtime
 * exception the router catches. A reachable node answering {@code index_not_found_exception} is
 * different. That is an {@code OpenSearchException}, which the provider absorbs into a sentinel —
 * an empty {@code ERROR_HIT} result for a search, {@code -1} for a count. The provider then
 * reports success, the router sees nothing to catch, and the caller receives a legitimate-looking
 * empty result. Routing alone does not fix it.</p>
 *
 * <h2>Why the OpenSearch client is mocked here</h2>
 * <p>An earlier version of this test relied on the container simply having no OpenSearch
 * counterpart indices. That looked like the real customer scenario but was not: the failure it
 * produced was dotCMS's own index-name resolution giving up ("Unable to load default versioned
 * indices"), which is a {@code DotRuntimeException} thrown <em>before</em> the OpenSearch client
 * is ever called — so it exercised the routing fix again rather than the sentinel branch, and
 * {@code index_not_found_exception} never appeared once in the run. Mocking the client is what
 * makes the failure the one AC-005 is actually about.</p>
 *
 * <h2>Coverage this test cannot give</h2>
 * <p>The scroll path carries a <em>second</em>, independent swallow of the same kind — its own
 * handler that logs a warning and returns an empty list. It cannot be driven from here: the
 * scroll resolves its client through CDI rather than through the injected provider, so a mocked
 * client does not reach it. The fix still covers it; only the behavioural proof is missing, and
 * that gap is deliberate and recorded rather than silently skipped.</p>
 *
 * @author Fabrizzio Araya
 */
public class ESContentFactoryImplMissingOsIndexTest {

    private static String query;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersistedAndPublish();
        assertTrue("Test setup must produce a persisted contentlet",
                null != contentlet.getInode());
        query = "+contentType:" + contentType.variable() + " +live:true";
    }

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    /** The 404 a reachable OpenSearch returns when the counterpart index does not exist. */
    private static OpenSearchException indexNotFound() {
        return new OpenSearchException(ErrorResponse.of(r -> r
                .status(404)
                .error(ErrorCause.of(c -> c
                        .type("index_not_found_exception")
                        .reason("no such index [working_20260101000000.os]")))));
    }

    /**
     * An OpenSearch provider whose node is up and answering, but whose counterpart index is gone.
     * The query cache is mocked empty so every call reaches the client rather than a cached hit —
     * a cached result would mask the failure exactly the way it masked it in QA.
     */
    private static ContentFactoryIndexOperationsOS openSearchMissingIndex() throws Exception {
        return openSearchMissingIndex(mock(OSQueryCache.class));
    }

    /** Same, with a caller-supplied cache mock so a test can assert what was written to it. */
    private static ContentFactoryIndexOperationsOS openSearchMissingIndex(
            final OSQueryCache queryCache) throws Exception {
        final OpenSearchClient client = mock(OpenSearchClient.class);
        when(client.search(any(SearchRequest.class), any())).thenThrow(indexNotFound());
        when(client.count(any(CountRequest.class))).thenThrow(indexNotFound());

        final OSClientProvider clientProvider = mock(OSClientProvider.class);
        when(clientProvider.getClient()).thenReturn(client);

        when(queryCache.get(any(SearchRequest.class))).thenReturn(Optional.empty());
        when(queryCache.get(any(CountRequest.class))).thenReturn(Optional.empty());

        return new ContentFactoryIndexOperationsOS(queryCache, clientProvider);
    }

    /** The factory with a real Elasticsearch leg and an OpenSearch leg missing its index. */
    private static ESContentFactoryImpl factory() throws Exception {
        return new ESContentFactoryImpl(
                new ContentFactoryIndexOperationsES(CacheLocator.getESQueryCache()),
                openSearchMissingIndex());
    }

    /**
     * Method to test: {@link ContentFactoryIndexOperationsOS#indexCount(String)}
     * Given Scenario: the OpenSearch provider is asked directly, with its index missing.
     * Expected Result: it does NOT return the right count — it either throws or hands back its
     *                  sentinel.
     *
     * <p>This is the precondition the rest of the class depends on, and it asserts the failure
     * is real rather than assuming it. Without it a green run could mean "the fallback works" or
     * "nothing ever failed", and those must not be indistinguishable — the previous version of
     * this class passed for exactly that reason while testing the wrong branch.</p>
     */
    @Test
    public void precondition_openSearchCannotServeTheQuery() throws Exception {
        long osCount = Long.MIN_VALUE;
        try {
            osCount = openSearchMissingIndex().indexCount(query);
        } catch (final Exception e) {
            return; // throwing is one of the two acceptable outcomes
        }
        assertNotEquals("OpenSearch answered the query correctly, so the missing-index scenario "
                + "is not being exercised and the rest of this class would pass vacuously",
                1L, osCount);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexSearch(String, int, int, String)}
     * Given Scenario: Phase 2, OpenSearch reachable but answering
     *                 {@code index_not_found_exception}, Elasticsearch healthy and holding the
     *                 content.
     * Expected Result: the real Elasticsearch hits are served. Zero hits here is the silent
     *                  variant of the outage — the dangerous one, because it is indistinguishable
     *                  from an empty content type.
     */
    @Test
    public void indexSearch_phase2_missingOsIndex_fallsBackToEs() throws Exception {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        final SearchHits hits = factory().indexSearch(query, 10, 0, "modDate desc");

        assertEquals("Phase 2 must serve the hits Elasticsearch holds when the OpenSearch "
                + "counterpart index is missing", 1L, hits.getTotalHits().value());
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexCount(String)}
     * Given Scenario: Phase 2, OpenSearch answering {@code index_not_found_exception}.
     * Expected Result: the Elasticsearch count is served. A count of 0 is the silent variant;
     *                  -1 is the sentinel leaking to the caller.
     */
    @Test
    public void indexCount_phase2_missingOsIndex_fallsBackToEs() throws Exception {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        final long count = factory().indexCount(query);

        assertEquals("Phase 2 must serve the count Elasticsearch holds when the OpenSearch "
                + "counterpart index is missing", 1L, count);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexCount(String)}
     * Given Scenario: Phase 3, OpenSearch answering {@code index_not_found_exception}.
     *                 Elasticsearch is decommissioned in Phase 3.
     * Expected Result: the caller must NOT receive Elasticsearch data. Either the sentinel or an
     *                  exception is correct; silently serving Elasticsearch results would be a
     *                  worse bug than the one being fixed, because it would report data absent
     *                  from the only live engine.
     */
    @Test
    public void indexCount_phase3_missingOsIndex_neverFallsBackToEs() throws Exception {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        long count = Long.MIN_VALUE;
        try {
            count = factory().indexCount(query);
        } catch (final Exception e) {
            return; // propagating is a correct Phase 3 outcome
        }
        assertNotEquals("Phase 3 must not fall back to Elasticsearch — it is decommissioned "
                + "there, so serving its data would hide that OpenSearch has no index",
                1L, count);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexSearch(String, int, int, String)}
     * Given Scenario: Phase 3, OpenSearch answering {@code index_not_found_exception}.
     * Expected Result: no Elasticsearch data, by the same reasoning as the count above.
     */
    @Test
    public void indexSearch_phase3_missingOsIndex_neverFallsBackToEs() throws Exception {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        SearchHits hits = null;
        try {
            hits = factory().indexSearch(query, 10, 0, "modDate desc");
        } catch (final Exception e) {
            return; // propagating is a correct Phase 3 outcome
        }
        assertNotEquals("Phase 3 must not fall back to Elasticsearch",
                1L, hits.getTotalHits().value());
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexCount(String)}
     * Given Scenario: Phase 1, where Elasticsearch serves reads and OpenSearch is a shadow.
     * Expected Result: the count is correct and the missing OpenSearch index is irrelevant. The
     *                  change must be a pure pass-through outside Phase 2 — these call sites
     *                  carry essentially all content search, so any behaviour change here is
     *                  felt site-wide in every phase.
     */
    @Test
    public void indexCount_phase1_missingOsIndexIsIrrelevant() throws Exception {
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);

        assertEquals("Phase 1 reads Elasticsearch regardless of OpenSearch index state",
                1L, factory().indexCount(query));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexCount(String)}
     * Given Scenario: Phase 0, the default.
     * Expected Result: unchanged. Elasticsearch answers; the OpenSearch leg is never consulted.
     */
    @Test
    public void indexCount_phase0_missingOsIndexIsIrrelevant() throws Exception {
        setPhase(MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED);

        assertEquals("Phase 0 behaviour must be untouched",
                1L, factory().indexCount(query));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexSearch(String, int, int, String)}
     * Given Scenario: Phase 2, OpenSearch answering with an error, and the fallback firing.
     * Expected Result: nothing is written to the OpenSearch query cache.
     *
     * <p>This is the half of the query-cache concern that is a real defect rather than a
     * reproduction hazard. The provider caches its error sentinel for some failure messages; on
     * the Phase 2 path such an entry would be replayed to every later identical query as a
     * successful empty result, outliving the outage and defeating the fallback even after
     * OpenSearch recovers. The raise therefore has to happen before the cache write, and this
     * asserts the order rather than trusting it.</p>
     *
     * <p>A cache <em>hit</em> during an outage is not a defect and is deliberately not tested
     * here: the cached value is a real result from a real earlier query, so serving it is
     * correct. It matters only because it hides the failure from a human running the
     * reproduction — which cuts both ways, since it can just as easily produce a false
     * confirmation that a fix works.</p>
     */
    @Test
    public void phase2_fallback_neverWritesTheErrorSentinelToTheQueryCache() throws Exception {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final OSQueryCache queryCache = mock(OSQueryCache.class);

        final SearchHits hits = new ESContentFactoryImpl(
                new ContentFactoryIndexOperationsES(CacheLocator.getESQueryCache()),
                openSearchMissingIndex(queryCache))
                .indexSearch(query, 10, 0, "modDate desc");

        assertEquals("Precondition: the fallback must have served the Elasticsearch hits",
                1L, hits.getTotalHits().value());
        verify(queryCache, never()).put(any(SearchRequest.class), any());
        verify(queryCache, never()).put(any(CountRequest.class), any());
    }
}
