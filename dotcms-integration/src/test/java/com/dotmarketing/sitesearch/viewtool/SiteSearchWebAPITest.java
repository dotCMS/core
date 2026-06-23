package com.dotmarketing.sitesearch.viewtool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.AggregationBucket;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.viewtool.SiteSearchWebAPI.Facet;
import com.dotmarketing.sitesearch.viewtool.SiteSearchWebAPI.InternalWrapperCountDateHistogramFacet;
import com.dotmarketing.sitesearch.viewtool.SiteSearchWebAPI.InternalWrapperStringTermsFacet;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the {@link SiteSearchWebAPI} Velocity view tool, modelled on
 * {@code ContentSearchToolTest}.
 *
 * <p>Exercises the public view-tool surface end-to-end against a live search backend after the
 * Elasticsearch → OpenSearch neutral-aggregation refactor (#35786), with emphasis on the fields of
 * the POJOs returned by the refactored methods:</p>
 * <ul>
 *   <li>{@code search(...)} → {@link SiteSearchResults} / {@link SiteSearchResult} fields, the
 *       alias path, the default-index path, pagination and error states.</li>
 *   <li>{@code getAggregations(...)} → the neutral {@link Aggregation} / {@link AggregationBucket}
 *       tree: name/type/buckets, doc counts, {@code getKeyAsNumber} (numeric histogram), and the
 *       nested {@code top_hits} {@link SearchHit}s.</li>
 *   <li>{@code getFacets(...)} → all three legacy wrappers: string-terms, date/numeric-histogram and
 *       the plain {@link Facet} fallback, plus their entry POJOs.</li>
 *   <li>{@code listSearchIndicies()} / {@code listSearchIncidies()}.</li>
 * </ul>
 *
 * <p>The tool resolves its backend through {@code APILocator.getSiteSearchAPI()} — now the
 * {@code SiteSearchAPIImpl} phase router — so this also proves the router wiring did not break the
 * legacy view-tool contract. Runs in the default integration profile (migration Phase 0 →
 * Elasticsearch), like {@code ContentSearchToolTest}; no OpenSearch container is required.</p>
 *
 * @author Fabrizio Araya
 */
public class SiteSearchWebAPITest extends IntegrationTestBase {

    private static final long SUFFIX = System.currentTimeMillis();
    private static final String IDX = "sitesearch_" + SUFFIX;
    private static final String ALIAS = "ss_it_alias_" + SUFFIX;

    /** Unique token embedded in every indexed doc so the text query matches only this run's data. */
    private static final String TOKEN = "ssqa" + SUFFIX;

    private static final String MIME_HTML = "text/html";
    private static final String MIME_PDF = "application/pdf";
    private static final Set<String> EXPECTED_MIMES = Set.of(MIME_HTML, MIME_PDF);

    /** 3 html docs + 2 pdf docs = 5 docs, all carrying TOKEN. */
    private static final int HTML_DOCS = 3;
    private static final int PDF_DOCS = 2;
    private static final int TOTAL_DOCS = HTML_DOCS + PDF_DOCS;

    // ---- Queries (JSON, so search() skips the request-host lookup) -----------------------------

    private static final String SEARCH_TOKEN =
            "{\"query\":{\"query_string\":{\"query\":\"" + "TOKEN_PLACEHOLDER"
                    + "\",\"default_field\":\"*\"}}}";

    private static final String TERMS_AGG =
            "{\"size\":0,\"aggs\":{\"by_mime\":{\"terms\":{\"field\":\"mimeType\",\"size\":10}}}}";

    private static final String NESTED_AGG =
            "{\"size\":0,\"aggs\":{\"by_mime\":{\"terms\":{\"field\":\"mimeType\",\"size\":10},"
                    + "\"aggs\":{\"top_docs\":{\"top_hits\":{\"size\":2}}}}}}";

    private static final String HISTO_AGG =
            "{\"size\":0,\"aggs\":{\"by_len\":{\"histogram\":{\"field\":\"contentLength\","
                    + "\"interval\":25}}}}";

    /** Query matches no doc, so the terms aggregation comes back with empty buckets. */
    private static final String EMPTY_AGG =
            "{\"size\":0,\"query\":{\"term\":{\"mimeType\":\"zzz/none\"}},"
                    + "\"aggs\":{\"empty\":{\"terms\":{\"field\":\"mimeType\",\"size\":10}}}}";

    private static SiteSearchAPI siteSearchAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        siteSearchAPI = APILocator.getSiteSearchAPI();

        // Create the index WITH an alias (so the alias search path is exercised) and activate it as
        // the default (so the default-index search path is exercised).
        siteSearchAPI.createSiteSearchIndex(IDX, ALIAS, 1);
        siteSearchAPI.activateIndex(IDX);

        for (int i = 0; i < TOTAL_DOCS; i++) {
            final boolean html = i < HTML_DOCS;
            final SiteSearchResult doc = new SiteSearchResult();
            doc.setId("ss-it-" + SUFFIX + "-" + i);
            doc.setUrl("/site-search-webapi-it/" + i);
            doc.setTitle("Site Search WebAPI IT doc " + i);
            doc.setHost("demo.dotcms.com");
            doc.setAuthor("qa-author-" + i);
            doc.setMimeType(html ? MIME_HTML : MIME_PDF);
            // Vary the body length so the numeric histogram on contentLength spreads over buckets.
            doc.setContent("dotcms site search viewtool integration " + TOKEN
                    + " ".repeat(i * 30));
            doc.setContentLength(doc.getContent().length());
            siteSearchAPI.putToIndex(IDX, doc, "content");
        }
    }

    @AfterClass
    public static void cleanup() {
        try {
            siteSearchAPI.deactivateIndex(IDX);
        } catch (final Exception e) {
            Logger.warn(SiteSearchWebAPITest.class, "Cleanup: deactivate failed: " + e.getMessage());
        }
        try {
            APILocator.getESIndexAPI()
                    .delete(APILocator.getESIndexAPI().getNameWithClusterIDPrefix(IDX));
        } catch (final Exception e) {
            Logger.warn(SiteSearchWebAPITest.class, "Cleanup: delete failed: " + e.getMessage());
        }
    }

    /** Builds a {@link SiteSearchWebAPI} initialized with a mock request/response. */
    private SiteSearchWebAPI siteSearchWebAPI() {
        final ViewContext viewContext = mock(ViewContext.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(viewContext.getRequest()).thenReturn(request);
        when(viewContext.getResponse()).thenReturn(response);

        final SiteSearchWebAPI tool = new SiteSearchWebAPI();
        tool.init(viewContext);
        return tool;
    }

    private static String searchToken() {
        return SEARCH_TOKEN.replace("TOKEN_PLACEHOLDER", TOKEN);
    }

    // =========================================================================
    // listSearchIndicies
    // =========================================================================

    /**
     * Given scenario: a populated, active site-search index.
     * Expected: listSearchIndicies() (and its legacy-typo alias) returns the created index.
     */
    @Test
    public void listSearchIndicies_containsCreatedIndex() {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        assertTrue("listSearchIndicies() must contain the created index",
                tool.listSearchIndicies().contains(IDX));
        assertTrue("legacy-typo alias listSearchIncidies() must behave identically",
                tool.listSearchIncidies().contains(IDX));

        Logger.info(this, "✅ listSearchIndicies_containsCreatedIndex passed");
    }

    // =========================================================================
    // search — SiteSearchResults / SiteSearchResult field coverage
    // =========================================================================

    /**
     * Given scenario: 5 docs carrying TOKEN in the default (active) index.
     * Expected: the default-index search (3-arg) populates every SiteSearchResults field and each
     * SiteSearchResult exposes id/url/title/mimeType/score.
     */
    @Test
    public void search_defaultIndex_populatesResultFields() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final SiteSearchResults results = tool.search(searchToken(), 0, 10);

        assertNull("Search must not return an error: " + results.getError(), results.getError());
        assertEquals("All TOKEN docs must be counted", TOTAL_DOCS, results.getTotalResults());
        assertEquals("getTotalHits() alias must match getTotalResults()",
                results.getTotalResults(), results.getTotalHits());
        assertEquals("Result rows must match the total (under the page size)",
                TOTAL_DOCS, results.getResults().size());
        assertTrue("maxScore must be positive for a matching query", results.getMaxScore() > 0);
        assertEquals("offset must reflect the requested start", 0, results.getOffset());
        assertEquals("start alias must match offset", results.getOffset(), results.getStart());
        assertEquals("limit must reflect the requested rows", 10, results.getLimit());
        assertNotNull("query echo must be set", results.getQuery());
        assertNotNull("took must be set", results.getTook());

        for (final SiteSearchResult hit : results.getResults()) {
            assertNotNull("each hit must carry an id", hit.getId());
            assertTrue("each hit id must belong to this run", hit.getId().startsWith("ss-it-" + SUFFIX));
            assertNotNull("each hit must carry a url", hit.getUrl());
            assertNotNull("each hit must carry a title", hit.getTitle());
            assertTrue("each hit mimeType must be one of the indexed types",
                    EXPECTED_MIMES.contains(hit.getMimeType()));
            assertTrue("each hit must have a positive score", hit.getScore() > 0);
        }

        Logger.info(this, "✅ search_defaultIndex_populatesResultFields passed – hits: "
                + results.getTotalResults());
    }

    /**
     * Given scenario: the index was created with an alias.
     * Expected: the 4-arg alias search resolves the alias to the backing index and returns the docs.
     */
    @Test
    public void search_byAlias_resolvesIndex() {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final SiteSearchResults results = tool.search(ALIAS, searchToken(), 0, 10);

        assertNull("Alias search must not return an error: " + results.getError(),
                results.getError());
        assertEquals("Alias search must reach the same docs", TOTAL_DOCS, results.getTotalResults());

        Logger.info(this, "✅ search_byAlias_resolvesIndex passed");
    }

    /**
     * Given scenario: a JSON body that caps the page size to 2.
     * Expected: the returned rows are capped to the page size while the total still reflects all
     * matches — covering the offset/limit/totalResults fields together.
     */
    @Test
    public void search_pagination_capsReturnedRows() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final String paged = "{\"size\":2,\"query\":{\"query_string\":{\"query\":\"" + TOKEN
                + "\",\"default_field\":\"*\"}}}";
        final SiteSearchResults results = tool.search(paged, 0, 2);

        assertNull("Paged search must not error: " + results.getError(), results.getError());
        assertEquals("Total must still reflect every match", TOTAL_DOCS, results.getTotalResults());
        assertTrue("Returned rows must be capped by the page size",
                results.getResults().size() <= 2);

        Logger.info(this, "✅ search_pagination_capsReturnedRows passed – returned: "
                + results.getResults().size());
    }

    /**
     * Given scenario: a query for a token that matches nothing.
     * Expected: zero results, an empty result list and no error (a clean empty response).
     */
    @Test
    public void search_noMatch_returnsEmptyWithoutError() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final String noMatch = "{\"query\":{\"query_string\":{\"query\":\"zzznomatchzzz" + SUFFIX
                + "\",\"default_field\":\"*\"}}}";
        final SiteSearchResults results = tool.search(noMatch, 0, 10);

        assertNull("No-match search must not error", results.getError());
        assertEquals("No-match search must count zero", 0, results.getTotalResults());
        assertTrue("No-match search must return no rows", results.getResults().isEmpty());

        Logger.info(this, "✅ search_noMatch_returnsEmptyWithoutError passed");
    }

    /**
     * Given scenario: a null query.
     * Expected: the tool reports an error on the SiteSearchResults rather than throwing.
     */
    @Test
    public void search_nullQuery_setsError() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final SiteSearchResults results = tool.search(null, 0, 10);

        assertNotNull("A null query must surface an error", results.getError());
        Logger.info(this, "✅ search_nullQuery_setsError passed – error: " + results.getError());
    }

    // =========================================================================
    // getAggregations — Aggregation / AggregationBucket field coverage
    // =========================================================================

    /**
     * Given scenario: 3 html + 2 pdf docs.
     * Expected: the terms aggregation on mimeType exposes a populated neutral Aggregation — name,
     * type, two buckets with correct doc counts, string keys, null numeric keys (non-numeric) and no
     * top-hits — covering the multi-bucket AggregationBucket accessors.
     */
    @Test
    public void getAggregations_termsBuckets_fieldsPopulated() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final Map<String, Aggregation> aggregations = tool.getAggregations(IDX, TERMS_AGG);

        assertNotNull("Aggregations map must not be null", aggregations);
        final Aggregation byMime = aggregations.get("by_mime");
        assertNotNull("'by_mime' aggregation must be present", byMime);
        assertEquals("aggregation name must round-trip", "by_mime", byMime.getName());
        assertNotNull("aggregation type must be reported", byMime.getType());
        assertNull("a terms aggregation carries no top-hits", byMime.getHits());
        assertEquals("there must be one bucket per mimeType", 2, byMime.getBuckets().size());

        long htmlCount = -1;
        long pdfCount = -1;
        for (final AggregationBucket bucket : byMime.getBuckets()) {
            assertTrue("bucket key must be a known mimeType",
                    EXPECTED_MIMES.contains(bucket.getKey()));
            assertEquals("getKeyAsString must mirror getKey", bucket.getKey(),
                    bucket.getKeyAsString());
            assertNull("a non-numeric key must yield a null number", bucket.getKeyAsNumber());
            assertTrue("each bucket must carry documents", bucket.getDocCount() > 0);
            assertTrue("a terms bucket has no sub-aggregations here",
                    bucket.getAggregations().isEmpty());
            if (MIME_HTML.equals(bucket.getKey())) {
                htmlCount = bucket.getDocCount();
            } else if (MIME_PDF.equals(bucket.getKey())) {
                pdfCount = bucket.getDocCount();
            }
        }
        assertEquals("html bucket must count the html docs", HTML_DOCS, htmlCount);
        assertEquals("pdf bucket must count the pdf docs", PDF_DOCS, pdfCount);

        Logger.info(this, "✅ getAggregations_termsBuckets_fieldsPopulated passed");
    }

    /**
     * Given scenario: a terms aggregation with a nested top_hits sub-aggregation.
     * Expected: the neutral tree preserves the nested {@code top_docs} as an Aggregation that carries
     * SearchHits, and each SearchHit exposes id and source — covering getHits()/SearchHit fields and
     * the nested getAggregations() path.
     */
    @Test
    public void getAggregations_nestedTopHits_preserved() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final Map<String, Aggregation> aggregations = tool.getAggregations(IDX, NESTED_AGG);
        final Aggregation byMime = aggregations.get("by_mime");
        assertNotNull("'by_mime' aggregation must be present", byMime);
        assertFalse("'by_mime' must have buckets", byMime.getBuckets().isEmpty());

        final AggregationBucket firstBucket = byMime.getBuckets().getFirst();
        final Aggregation topDocs = firstBucket.getAggregations().get("top_docs");
        assertNotNull("nested top_hits sub-aggregation must be preserved", topDocs);
        assertNotNull("top_hits must carry a SearchHits container", topDocs.getHits());

        final List<SearchHit> hits = topDocs.getHits().getHits();
        assertFalse("top_hits must carry at least one hit", hits.isEmpty());
        final SearchHit hit = hits.getFirst();
        assertNotNull("each top-hit must expose an id", hit.getId());
        assertFalse("each top-hit must expose its source document",
                hit.getSourceAsMap().isEmpty());

        Logger.info(this, "✅ getAggregations_nestedTopHits_preserved passed – topHits: " + hits.size());
    }

    /**
     * Given scenario: a numeric histogram on the long field {@code contentLength}.
     * Expected: the buckets carry numeric keys, so {@link AggregationBucket#getKeyAsNumber()} returns
     * a non-null Number — covering the numeric-key path (distinct from the non-numeric terms keys).
     */
    @Test
    public void getAggregations_numericHistogram_keyAsNumber() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final Map<String, Aggregation> aggregations = tool.getAggregations(IDX, HISTO_AGG);
        final Aggregation byLen = aggregations.get("by_len");
        assertNotNull("'by_len' histogram aggregation must be present", byLen);
        assertTrue("histogram type must be reported as a histogram",
                byLen.getType().contains("histogram"));
        assertFalse("histogram must produce buckets", byLen.getBuckets().isEmpty());

        boolean sawPopulatedNumericBucket = false;
        for (final AggregationBucket bucket : byLen.getBuckets()) {
            assertNotNull("a histogram bucket key must be numeric", bucket.getKeyAsNumber());
            if (bucket.getDocCount() > 0) {
                sawPopulatedNumericBucket = true;
            }
        }
        assertTrue("at least one histogram bucket must contain documents", sawPopulatedNumericBucket);

        Logger.info(this, "✅ getAggregations_numericHistogram_keyAsNumber passed");
    }

    // =========================================================================
    // getFacets — legacy wrapper coverage (terms / histogram / plain)
    // =========================================================================

    /**
     * Given scenario: a terms aggregation with non-empty buckets.
     * Expected: getFacets wraps it as an {@link InternalWrapperStringTermsFacet} exposing name/type
     * and term entries with term + count — covering the legacy string-terms facet POJO.
     */
    @Test
    public void getFacets_termsAggregation_wrapsAsStringTermsFacet() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final Map<String, Facet> facets = tool.getFacets(IDX, TERMS_AGG);
        assertNotNull("Facets map must not be null", facets);

        final Facet facet = facets.get("by_mime");
        assertNotNull("'by_mime' facet must be present", facet);
        assertEquals("facet name must round-trip", "by_mime", facet.getName());
        assertNotNull("facet type must be reported", facet.getType());
        assertTrue("non-empty terms aggregation must map to InternalWrapperStringTermsFacet",
                facet instanceof InternalWrapperStringTermsFacet);

        final InternalWrapperStringTermsFacet termsFacet = (InternalWrapperStringTermsFacet) facet;
        assertEquals("there must be one entry per bucket", 2, termsFacet.entries().size());

        long htmlCount = -1;
        for (final var entry : termsFacet.entries()) {
            assertTrue("entry term must be a known mimeType", EXPECTED_MIMES.contains(entry.getTerm()));
            assertTrue("entry count must be positive", entry.getCount() > 0);
            if (MIME_HTML.equals(entry.getTerm())) {
                htmlCount = entry.getCount();
            }
        }
        assertEquals("html term entry must count the html docs", HTML_DOCS, htmlCount);

        Logger.info(this, "✅ getFacets_termsAggregation_wrapsAsStringTermsFacet passed");
    }

    /**
     * Given scenario: a numeric histogram aggregation.
     * Expected: getFacets wraps it as an {@link InternalWrapperCountDateHistogramFacet} exposing
     * CountEntry rows with time (the numeric key) and count — covering the legacy histogram facet
     * POJO and the {@code isHistogram} branch.
     */
    @Test
    public void getFacets_histogramAggregation_wrapsAsCountHistogramFacet() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final Map<String, Facet> facets = tool.getFacets(IDX, HISTO_AGG);
        final Facet facet = facets.get("by_len");
        assertNotNull("'by_len' facet must be present", facet);
        assertTrue("a histogram aggregation must map to InternalWrapperCountDateHistogramFacet",
                facet instanceof InternalWrapperCountDateHistogramFacet);

        final InternalWrapperCountDateHistogramFacet histoFacet =
                (InternalWrapperCountDateHistogramFacet) facet;
        assertFalse("histogram facet must expose count entries", histoFacet.entries().isEmpty());

        boolean sawPopulatedEntry = false;
        for (final var entry : histoFacet.entries()) {
            assertTrue("entry time (numeric key) must be non-negative", entry.getTime() >= 0);
            if (entry.getCount() > 0) {
                sawPopulatedEntry = true;
            }
        }
        assertTrue("at least one histogram entry must carry a count", sawPopulatedEntry);

        Logger.info(this, "✅ getFacets_histogramAggregation_wrapsAsCountHistogramFacet passed");
    }

    /**
     * Given scenario: a terms aggregation whose query matches no document (empty buckets).
     * Expected: getFacets falls back to a plain {@link Facet} (neither wrapper), still exposing
     * name and type — covering the empty-bucket branch.
     */
    @Test
    public void getFacets_emptyBuckets_fallsBackToPlainFacet() throws Exception {
        final SiteSearchWebAPI tool = siteSearchWebAPI();

        final Map<String, Facet> facets = tool.getFacets(IDX, EMPTY_AGG);
        final Facet facet = facets.get("empty");
        assertNotNull("'empty' facet must be present", facet);
        assertEquals("facet name must round-trip", "empty", facet.getName());
        assertNotNull("facet type must be reported", facet.getType());
        assertFalse("an empty terms aggregation must NOT be a string-terms wrapper",
                facet instanceof InternalWrapperStringTermsFacet);
        assertFalse("an empty terms aggregation must NOT be a histogram wrapper",
                facet instanceof InternalWrapperCountDateHistogramFacet);

        Logger.info(this, "✅ getFacets_emptyBuckets_fallsBackToPlainFacet passed");
    }
}
