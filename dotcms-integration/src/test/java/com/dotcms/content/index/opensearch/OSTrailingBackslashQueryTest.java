package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.RefreshRequest;

/**
 * Checks how OpenSearch reads a query whose text was composed by concatenation, which is how the
 * search code builds a query when it has to restrict what a caller wrote.
 *
 * <p>The restriction is appended to the caller's query as text, separated from it by a single
 * space. A query that ends in an odd number of backslashes ends in an escape sequence, and that
 * escape consumes the separating space: what the author wrote as a clause of its own is then read
 * as part of the caller's last term, and the restriction stops restricting anything. The code
 * guards against this by balancing the trailing backslashes before appending, and this test asks
 * whether OpenSearch — a different engine, a different Lucene version — reads the two forms the
 * way the guard assumes.</p>
 *
 * <p>Two documents are indexed, only one of which carries the value the restriction asks for. The
 * query runs through the raw search path as the System User, so nothing is added to the query
 * beyond the text written here: the restriction is the one this test composes, and the engine's
 * reading of it is what the assertions are about. The field standing in for the restriction is a
 * plain one, because the question is whether the required group survives the concatenation, not
 * what the group asks for.</p>
 *
 * <p>Requires the {@code opensearch-upgrade} Docker container running on
 * {@code http://localhost:9201} with security disabled.
 * Registered in {@link com.dotcms.OpenSearchUpgradeSuite}.</p>
 *
 * <p>Run with:
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true \
 *       -Dmaven.build.cache.enabled=false
 * </pre>
 * </p>
 *
 * @author Fabrizzio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OSTrailingBackslashQueryTest extends IntegrationTestBase {

    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    private static final String IDX_LIVE = "live_backslash_" + RUN_ID;
    private static final String IDX_WORKING = "working_backslash_" + RUN_ID;

    private static final String CONTENT_TYPE = "backslashtype" + RUN_ID;

    /** The document the restriction is meant to let through. */
    private static final String READABLE_ID = "readable-" + RUN_ID;
    /** The document the restriction is meant to keep out. */
    private static final String RESTRICTED_ID = "restricted-" + RUN_ID;

    /** The clause that restricts the results, the way it is appended to a caller's query. */
    private static final String RESTRICTION = "+(permissions:readable)";

    /** What a caller wrote, with nothing special at the end of it. */
    private static final String CALLER_QUERY = "+contenttype:" + CONTENT_TYPE;

    private static final String DEFAULT_OS_VERSION = VersionedIndices.OPENSEARCH_3X;

    @Inject
    private OSSearchAPIImpl osSearchAPI;

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    @Inject
    private ContentletIndexOperationsOS opsOS;

    @Inject
    private OSClientProvider clientProvider;

    @Inject
    private VersionedIndicesAPI versionedIndicesAPI;

    private User systemUser;

    private String physicalLive;
    private String physicalWorking;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws Exception {
        physicalLive = opsOS.toPhysicalName(IDX_LIVE);
        physicalWorking = opsOS.toPhysicalName(IDX_WORKING);

        cleanupTestOsIndices();
        cleanupVersionedRows();

        systemUser = APILocator.getUserAPI().getSystemUser();

        osIndexAPI.createIndex(physicalLive, 1);
        osIndexAPI.createIndex(physicalWorking, 1);

        versionedIndicesAPI.saveIndices(
                VersionedIndicesImpl.builder()
                        .version(DEFAULT_OS_VERSION)
                        .live(physicalLive)
                        .working(physicalWorking)
                        .build());

        // Make the search path read the rows just saved rather than a cached prior state.
        versionedIndicesAPI.clearCache();

        indexDocument(physicalLive, READABLE_ID, document(READABLE_ID, "readable"));
        indexDocument(physicalLive, RESTRICTED_ID, document(RESTRICTED_ID, "restricted"));
    }

    @After
    public void tearDown() {
        cleanupTestOsIndices();
        cleanupVersionedRows();
        // The cached pointer names indices this test has just deleted; leaving it in place breaks
        // index resolution for the next member of the suite. Mirrors the clearCache() in setUp.
        versionedIndicesAPI.clearCache();
    }

    /**
     * Given scenario: a caller query that ends in nothing special, with the restriction appended.
     * Expected: OpenSearch honours the restriction and returns only the readable document. This is
     * the result the two tests below are measured against.
     */
    @Test
    public void test_plain_caller_query_is_restricted() throws Exception {
        final List<String> found = search(CALLER_QUERY + " " + RESTRICTION);

        assertEquals("the restriction must keep the restricted document out", 1, found.size());
        assertEquals(READABLE_ID, found.get(0));
    }

    /**
     * Given scenario: a caller query ending in a backslash that was balanced before the
     * restriction was appended, which is what the search code does.
     * Expected: OpenSearch reads the restriction as a clause of its own and returns only the
     * readable document, exactly as for a caller query that ends in nothing special.
     */
    @Test
    public void test_balanced_trailing_backslash_keeps_the_restriction_separate() throws Exception {
        final List<String> found = search(CALLER_QUERY + " x\\\\" + " " + RESTRICTION);

        assertEquals("balancing the escape must leave the restriction in force", 1, found.size());
        assertEquals(READABLE_ID, found.get(0));
    }

    /**
     * Given scenario: the same caller query, with the trailing backslash left unbalanced, which is
     * what reaches the engine when nothing normalizes the query first.
     * Expected: OpenSearch does <em>not</em> return the restricted result. The engine may read the
     * restriction as part of the caller's last term, in which case the restricted document comes
     * back too, or it may refuse the query outright; either way the restriction is not applied,
     * which is why the query is normalized before the clause is appended.
     */
    @Test
    public void test_unbalanced_trailing_backslash_loses_the_restriction() throws Exception {
        final String unbalanced = CALLER_QUERY + " x\\" + " " + RESTRICTION;

        final List<String> found;
        try {
            found = search(unbalanced);
        } catch (final Exception e) {
            // The engine refused the query. The restriction is not applied either way, and a
            // caller that normalizes first never gets here.
            Logger.info(this, "OpenSearch refused the unbalanced query: " + e.getMessage());
            return;
        }

        if (found.size() == 1 && READABLE_ID.equals(found.get(0))) {
            fail("OpenSearch applied the restriction to a query ending in an unbalanced escape,"
                    + " which no engine this code runs against is expected to do; the result was "
                    + found);
        }
        assertTrue("the unbalanced query must not come back restricted",
                found.contains(RESTRICTED_ID) || found.isEmpty());
        Logger.info(this, "unbalanced query returned " + found);
    }

    /**
     * Builds the document indexed for this test.
     *
     * @param identifier the document's identifier and inode
     * @param permissions the value the restriction asks for, or another value to be kept out
     * @return the document as JSON
     */
    private static String document(final String identifier, final String permissions) {
        return "{\"identifier\":\"" + identifier + "\","
                + "\"inode\":\"" + identifier + "-inode\","
                + "\"contenttype\":\"" + CONTENT_TYPE + "\","
                + "\"permissions\":\"" + permissions + "\","
                + "\"live\":true}";
    }

    /**
     * Runs the given query text through the raw search path against the live index.
     *
     * @param queryText the query as the search code would hand it to the engine
     * @return the identifiers of the documents that matched, in the order the engine returned them
     */
    private List<String> search(final String queryText) throws Exception {
        final String body = "{\"query\":{\"query_string\":{\"query\":\""
                + queryText.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}}}";

        final ContentSearchResponse response =
                osSearchAPI.searchRaw(body, true, systemUser, false);

        assertNotNull("the search must return a response", response);
        assertNotNull("the response must carry hits", response.hits());

        return response.hits().getHits().stream()
                .map(SearchHit::getSourceAsMap)
                .map(source -> String.valueOf(source.get("identifier")))
                .collect(Collectors.toList());
    }

    private void indexDocument(final String fullIndexName, final String docId, final String docJson)
            throws Exception {
        final IndexBulkRequest req = opsOS.createBulkRequest();
        opsOS.addIndexOp(req, fullIndexName, docId, docJson);
        opsOS.putToIndex(req);
        refreshTestIndex(fullIndexName);
    }

    private void refreshTestIndex(final String fullIndexName) {
        try {
            final OpenSearchClient client = clientProvider.getClient();
            client.indices().refresh(RefreshRequest.of(r -> r.index(fullIndexName)));
        } catch (final Exception e) {
            Logger.warn(this, "refreshTestIndex: error refreshing '" + fullIndexName
                    + "': " + e.getMessage());
        }
    }

    private synchronized void cleanupTestOsIndices() {
        for (final String idx : List.of(physicalLive, physicalWorking)) {
            try {
                if (osIndexAPI.indexExists(idx)) {
                    osIndexAPI.delete(idx);
                }
            } catch (final Exception e) {
                Logger.warn(this, "Cleanup: error removing OS index '" + idx + "': "
                        + e.getMessage());
            }
        }
    }

    private void cleanupVersionedRows() {
        try {
            // Remove only the rows this test registered, found by their RUN_ID-tagged index name.
            // Deleting the whole OPENSEARCH_3X version row would break the tests running beside
            // this one.
            new DotConnect()
                    .setSQL("DELETE FROM indicies WHERE index_name LIKE ?")
                    .addParam("%" + RUN_ID + "%")
                    .loadResult();
        } catch (final Exception e) {
            Logger.warn(this, "Cleanup: error removing versioned DB rows: " + e.getMessage());
        }
    }
}
