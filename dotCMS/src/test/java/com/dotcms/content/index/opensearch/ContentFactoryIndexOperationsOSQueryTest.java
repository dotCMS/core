package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Config;
import java.io.StringWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.query_dsl.Query;

/**
 * Unit tests for {@link ContentFactoryIndexOperationsOS#createQuery(String, String)} — the query
 * the OpenSearch read path actually sends to the cluster.
 *
 * <p>Regression cover for issue #36501 (D12). With
 * {@code ELASTICSEARCH_USE_FILTERS_FOR_SEARCHING=true} — the shipped default in
 * {@code dotcms-config-cluster.properties} — and {@code sortBy="random"}, the OpenSearch port
 * returned a bare {@code function_score} over {@code match_all} and dropped the Lucene query
 * entirely, while its Elasticsearch counterpart kept the query as a {@code post_filter}. A
 * random-sorted search therefore returned an unfiltered sample of the whole index: callers
 * resolved arbitrary documents against the database, and {@code IdentifierDateJob} NPE'd on
 * inodes that do not exist and attempted to INSERT phantom identifier rows.</p>
 *
 * <p>The invariant these tests pin is engine-independent and holds on every branch: <strong>the
 * Lucene query must reach the cluster.</strong> Assertions run against the serialized JSON — the
 * same wire form {@link OSQueryCache} keys on — so they survive refactors of the builder chain
 * and would catch the query being dropped anywhere in it.</p>
 *
 * @author Fabrizzio Araya
 */
public class ContentFactoryIndexOperationsOSQueryTest {

    /** Same key the production code resolves first; shadows the ES fallback. */
    private static final String OS_FILTERS_KEY = "OS_USE_FILTERS_FOR_SEARCHING";

    /** A query with a field that only the real caller's content type would match. */
    private static final String LUCENE_QUERY =
            "+structureName:myTestType +working:true +languageId:1";

    private static final JacksonJsonpMapper MAPPER = new JacksonJsonpMapper();

    private ContentFactoryIndexOperationsOS operations;
    private String previousFlag;

    @BeforeClass
    public static void prepare() {
        Config.initializeConfig();
    }

    @Before
    public void setUp() {
        previousFlag = Config.getStringProperty(OS_FILTERS_KEY, null);
        // Neither collaborator is reachable from createQuery — it is a pure builder.
        operations = new ContentFactoryIndexOperationsOS(null, null);
    }

    @After
    public void tearDown() {
        Config.setProperty(OS_FILTERS_KEY, previousFlag);
    }

    private static void useFiltersForSearching(final boolean enabled) {
        Config.setProperty(OS_FILTERS_KEY, String.valueOf(enabled));
    }

    /** Serializes the query to the JSON the OpenSearch client would put on the wire. */
    private static String json(final Query query) {
        final StringWriter writer = new StringWriter();
        try (final jakarta.json.stream.JsonGenerator generator =
                MAPPER.jsonProvider().createGenerator(writer)) {
            query.serialize(generator, MAPPER);
        }
        return writer.toString();
    }

    private String queryJson(final String sortBy) {
        return json(operations.createQuery(LUCENE_QUERY, sortBy));
    }

    // ---- the regression: random sort must not discard the query ---------------------------------

    @Test
    public void randomSort_withFiltersEnabled_keepsTheLuceneQuery() {
        useFiltersForSearching(true);

        final String json = queryJson("random");

        assertTrue("random sort must still carry the Lucene query, got: " + json,
                json.contains(LUCENE_QUERY));
    }

    @Test
    public void randomSort_withFiltersEnabled_doesNotDegenerateToMatchAll() {
        useFiltersForSearching(true);

        final String json = queryJson("random");

        // The exact shape of the #36501 defect: function_score wrapping an unfiltered match_all.
        assertFalse("random sort must not search the whole index, got: " + json,
                json.contains("match_all"));
    }

    @Test
    public void randomSort_withFiltersEnabled_stillScoresRandomly() {
        useFiltersForSearching(true);

        final String json = queryJson("random");

        // Keeping the query must not cost the randomness the caller asked for.
        assertTrue("random sort must keep its random_score function, got: " + json,
                json.contains("random_score"));
    }

    // ---- every other branch already kept the query; lock that in --------------------------------

    @Test
    public void nonScoreSort_withFiltersEnabled_keepsTheLuceneQuery() {
        useFiltersForSearching(true);

        assertTrue(queryJson("moddate desc").contains(LUCENE_QUERY));
    }

    @Test
    public void scoreSort_withFiltersEnabled_keepsTheLuceneQuery() {
        useFiltersForSearching(true);

        // "score" short-circuits the filter branch entirely.
        assertTrue(queryJson("score").contains(LUCENE_QUERY));
    }

    @Test
    public void randomSort_withFiltersDisabled_keepsTheLuceneQuery() {
        useFiltersForSearching(false);

        assertTrue(queryJson("random").contains(LUCENE_QUERY));
    }

    @Test
    public void noSort_keepsTheLuceneQuery() {
        useFiltersForSearching(true);

        assertTrue(queryJson(null).contains(LUCENE_QUERY));
    }
}
