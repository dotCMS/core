package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.ContentFactoryIndexOperations;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.IndexContentletScroll;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests proving that {@link ESContentFactoryImpl} routes its reads through
 * {@link com.dotcms.content.index.PhaseRouter}, so the Phase 2 Elasticsearch fallback actually
 * fires for content search (<a href="https://github.com/dotCMS/core/issues/37413">#37413</a>).
 *
 * <h2>What regressed</h2>
 * <p>The fallback was implemented and correct; the content read path simply never reached it.
 * {@code ESContentFactoryImpl} selected a provider with a bare ternary and called it directly
 * from five read call sites, so the router was unreachable from the busiest read path in the
 * product. In Phase 2 with OpenSearch down and Elasticsearch healthy and dual-written,
 * {@code POST /api/content/_search} returned a well-formed {@code 200} with zero results — which
 * a caller cannot tell apart from "this content type has no content", so a live site rendered as
 * missing content with nothing for 5xx monitoring to catch.</p>
 *
 * <h2>What this test adds over the unit tests</h2>
 * <p>{@code ContentFactoryIndexOperationsPhaseRoutingTest} pins the per-phase fallback contract
 * against the provider interface, but a unit test on the router cannot show <em>which class
 * calls it</em> — and that was the entire defect. This test is the wiring proof: it injects an
 * OpenSearch provider that fails every read alongside the real Elasticsearch provider, and
 * asserts that the factory's own read methods still return real Elasticsearch data. Before the
 * fix these calls propagate the injected failure, because nothing catches it.</p>
 *
 * @author Fabrizzio Araya
 */
public class ESContentFactoryImplPhase2FallbackTest {

    private static ContentType contentType;
    private static String query;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        contentType = new ContentTypeDataGen().nextPersisted();
        // WAIT_FOR so the document is searchable before the assertions run.
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

    /**
     * An OpenSearch provider standing in for an unreachable node: every read fails the way the
     * real provider's generic handler wraps a connection failure.
     */
    private static final class UnreachableOpenSearch implements ContentFactoryIndexOperations {

        private static DotRuntimeException down() {
            return new DotRuntimeException("An error occurred when executing the Lucene Query",
                    new java.net.ConnectException("Connection refused"));
        }

        @Override
        public String inferIndexToHit(final String query) {
            throw down();
        }

        @Override
        public long indexCount(final String query) {
            throw down();
        }

        @Override
        public SearchHits searchHits(final String query, final int limit, final int offset,
                final String sortBy) {
            throw down();
        }

        @Override
        public List<String> search(final String query, final int limit, final int offset) {
            throw down();
        }

        @Override
        public PaginatedArrayList<ContentletSearch> indexSearchScroll(final String query,
                final String sortBy, final int scrollBatchSize) {
            throw down();
        }

        @Override
        public IndexContentletScroll createScrollQuery(final String luceneQuery, final User user,
                final boolean respectFrontendRoles, final int batchSize, final String sortBy) {
            throw down();
        }

        @Override
        public IndexContentletScroll createScrollQuery(final String luceneQuery, final User user,
                final boolean respectFrontendRoles, final int batchSize) {
            throw down();
        }
    }

    /** A factory whose OpenSearch leg is dead and whose Elasticsearch leg is the real one. */
    private static ESContentFactoryImpl factoryWithDeadOpenSearch() {
        return new ESContentFactoryImpl(
                new ContentFactoryIndexOperationsES(CacheLocator.getESQueryCache()),
                new UnreachableOpenSearch());
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexCount(String)}
     * Given Scenario: Phase 2, OpenSearch unreachable, Elasticsearch healthy and holding the
     *                 content. This is the call that produced the 500s, because the count runs
     *                 before the search and an uncached count threw.
     * Expected Result: the real Elasticsearch count comes back — not 0, and not an exception.
     */
    @Test
    public void indexCount_phase2_deadOpenSearch_servesElasticsearchCount() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        final long count = factoryWithDeadOpenSearch().indexCount(query);

        assertEquals("Phase 2 must fall back to the count Elasticsearch holds", 1L, count);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexSearch(String, int, int, String)}
     * Given Scenario: Phase 2, OpenSearch unreachable, Elasticsearch healthy.
     * Expected Result: the real hits come back. A zero-hit result here is the silent variant of
     *                  the outage — the dangerous one, because it looks like an empty content
     *                  type rather than a failure.
     */
    @Test
    public void indexSearch_phase2_deadOpenSearch_servesElasticsearchHits() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        final SearchHits hits = factoryWithDeadOpenSearch()
                .indexSearch(query, 10, 0, "modDate desc");

        assertEquals("Phase 2 must fall back to the hits Elasticsearch holds",
                1L, hits.getTotalHits().value());
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexSearchScroll(String, String)}
     * Given Scenario: Phase 2, OpenSearch unreachable, Elasticsearch healthy.
     * Expected Result: the scroll is served from Elasticsearch. This site can fall back safely
     *                  because it materialises the whole scroll before returning, so a failure
     *                  leaves no cursor half-drained on the failing engine.
     */
    @Test
    public void indexSearchScroll_phase2_deadOpenSearch_servesElasticsearchResults() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        final PaginatedArrayList<ContentletSearch> results =
                factoryWithDeadOpenSearch().indexSearchScroll(query, "modDate desc");

        assertEquals("Phase 2 must fall back to the scroll Elasticsearch can serve",
                1, results.size());
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexCount(String)}
     * Given Scenario: Phase 1, where Elasticsearch serves reads and OpenSearch is only a shadow.
     * Expected Result: the count is served without the dead OpenSearch provider being consulted
     *                  at all. The routing change has to be a pure pass-through outside Phase 2 —
     *                  these five call sites carry essentially all content search in the product,
     *                  so any behaviour change here is felt site-wide in every phase.
     */
    @Test
    public void indexCount_phase1_deadOpenSearchIsNeverConsulted() {
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);

        final long count = factoryWithDeadOpenSearch().indexCount(query);

        assertEquals("Phase 1 reads Elasticsearch and must not touch OpenSearch", 1L, count);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#indexCount(String)}
     * Given Scenario: Phase 0, the default — migration not started.
     * Expected Result: same as Phase 1. Elasticsearch answers; OpenSearch is never contacted.
     */
    @Test
    public void indexCount_phase0_deadOpenSearchIsNeverConsulted() {
        setPhase(MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED);

        final long count = factoryWithDeadOpenSearch().indexCount(query);

        assertEquals("Phase 0 reads Elasticsearch and must not touch OpenSearch", 1L, count);
    }
}
