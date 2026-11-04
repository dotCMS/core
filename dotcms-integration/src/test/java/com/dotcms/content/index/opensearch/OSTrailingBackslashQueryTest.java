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
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.RefreshRequest;

/**
 * Checks, against a live OpenSearch cluster, that a query whose text was composed by concatenation
 * still means what it was written to mean.
 *
 * <p>The clauses that narrow a search to what the caller may read are appended to the caller's
 * query as text, separated from it by a single space. A query that ends in an odd number of
 * backslashes ends in an escape sequence, and that escape consumes the separating space: what the
 * author wrote as a clause of its own is then read as part of the caller's last term, and the
 * clause stops restricting anything. {@code ESContentletAPIImpl} balances the trailing backslashes
 * before appending, and the question this test answers is whether OpenSearch — a different engine
 * on a different Lucene version — reads the result the way that guard assumes.</p>
 *
 * <p>Two documents are indexed, each readable by a different role, with the {@code permissions}
 * field mapped to the whitespace analyzer the production mapping gives it so a permission token
 * survives indexing whole. The restriction is not written here: it is the one
 * {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#addPermissionsToQuery} builds
 * for a real user, normalization included, so reverting that normalization fails
 * {@link #test_productionClauseRestrictsAQueryEndingInBackslash()}.</p>
 *
 * <p>The three cases that compose their own query are characterization of the engine rather than
 * cover for this repository's code: they record how OpenSearch reads each form, which is the
 * premise the guard rests on.</p>
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

    /** A restriction written by hand, for the cases that ask what the engine does with one. */
    private static final String RESTRICTION = "+(permissions:readable" + RUN_ID + ")";

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

    private static User systemUser;
    private static Role readerRole;
    private static Role otherRole;
    private static User readerUser;

    private String physicalLive;
    private String physicalWorking;

    /** What the installation pointed at before this test replaced it, restored in {@link #tearDown()}. */
    private VersionedIndices previousIndices;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        readerRole = new RoleDataGen().nextPersisted();
        otherRole = new RoleDataGen().nextPersisted();
        readerUser = new UserDataGen().roles(readerRole).nextPersisted();
    }

    /**
     * Removes the role and the user this test created. Deleting a role empties the role cache,
     * which is why this class is registered last in its suite.
     */
    @AfterClass
    public static void cleanUpFixture() {
        UserDataGen.remove(readerUser);
        RoleDataGen.remove(otherRole);
        RoleDataGen.remove(readerRole);
    }

    @Before
    public void setUp() throws Exception {
        physicalLive = opsOS.toPhysicalName(IDX_LIVE);
        physicalWorking = opsOS.toPhysicalName(IDX_WORKING);

        // Saving this test's indices replaces every row the version has, so remember what was
        // there and put it back in tearDown rather than leaving the installation without a
        // pointer for whatever runs next.
        previousIndices = versionedIndicesAPI.loadIndices(DEFAULT_OS_VERSION).orElse(null);

        cleanupTestOsIndices();
        cleanupVersionedRows();

        osIndexAPI.createIndex(physicalLive, 1);
        osIndexAPI.createIndex(physicalWorking, 1);
        mapPermissionsField(physicalLive);

        versionedIndicesAPI.saveIndices(
                VersionedIndicesImpl.builder()
                        .version(DEFAULT_OS_VERSION)
                        .live(physicalLive)
                        .working(physicalWorking)
                        .build());

        // Make the search path read the rows just saved rather than a cached prior state.
        versionedIndicesAPI.clearCache();

        indexDocument(physicalLive, READABLE_ID,
                document(READABLE_ID, "readable" + RUN_ID, readerRole));
        indexDocument(physicalLive, RESTRICTED_ID,
                document(RESTRICTED_ID, "restricted" + RUN_ID, otherRole));
    }

    @After
    public void tearDown() throws Exception {
        cleanupTestOsIndices();
        cleanupVersionedRows();
        if (previousIndices != null) {
            versionedIndicesAPI.saveIndices(previousIndices);
        }
        // The cached pointer still names indices this test has just deleted; leaving it in place
        // breaks index resolution for whatever runs next. Mirrors the clearCache() in setUp.
        versionedIndicesAPI.clearCache();
    }

    /**
     * Given scenario: a caller query ending in a backslash, restricted by the clause the production
     * code builds for a user who holds one of the two roles.
     * Expected: only the document that role can read comes back.
     *
     * <p>This is the case that fails if the query stops being normalized before the clause is
     * appended: the escape swallows the separating space, the clause is folded into the caller's
     * last term, and the document the user cannot read comes back too.
     */
    @Test
    public void test_productionClauseRestrictsAQueryEndingInBackslash() throws Exception {
        final StringBuffer buffy = new StringBuffer(CALLER_QUERY + " x\\");
        APILocator.getContentletAPI()
                .addPermissionsToQuery(buffy, readerUser, List.of(readerRole), false);

        final List<String> found = search(buffy.toString());

        assertEquals("the caller must see only what their role grants them",
                List.of(READABLE_ID), found);
    }

    /**
     * The same clause on a caller query that ends in nothing special, which is what the case above
     * is measured against: it shows the fixture restricts in the ordinary case too.
     */
    @Test
    public void test_productionClauseRestrictsAPlainQuery() throws Exception {
        final StringBuffer buffy = new StringBuffer(CALLER_QUERY);
        APILocator.getContentletAPI()
                .addPermissionsToQuery(buffy, readerUser, List.of(readerRole), false);

        assertEquals(List.of(READABLE_ID), search(buffy.toString()));
    }

    /**
     * Given scenario: a caller query that ends in nothing special, with a restriction appended.
     * Expected: OpenSearch honours the restriction and returns only the readable document. This is
     * the engine's baseline, which the two cases below are measured against.
     */
    @Test
    public void test_plainCallerQueryIsRestricted() throws Exception {
        final List<String> found = search(CALLER_QUERY + " " + RESTRICTION);

        assertEquals("the restriction must keep the restricted document out",
                List.of(READABLE_ID), found);
    }

    /**
     * Given scenario: a caller query ending in a backslash that was balanced before the restriction
     * was appended, which is what the search code does.
     * Expected: OpenSearch reads the restriction as a clause of its own, exactly as for a caller
     * query that ends in nothing special.
     */
    @Test
    public void test_balancedTrailingBackslashKeepsTheRestrictionSeparate() throws Exception {
        final List<String> found = search(CALLER_QUERY + " x\\\\" + " " + RESTRICTION);

        assertEquals("balancing the escape must leave the restriction in force",
                List.of(READABLE_ID), found);
    }

    /**
     * Given scenario: the same caller query with the trailing backslash left unbalanced, which is
     * what would reach the engine if nothing normalized the query first.
     * Expected: OpenSearch does not apply the restriction. It may read the clause as part of the
     * caller's last term, in which case the restricted document comes back too, or it may refuse
     * the query as unparseable; the case accepts either, and nothing else.
     */
    @Test
    public void test_unbalancedTrailingBackslashLosesTheRestriction() throws Exception {
        // Prove the fixture is alive before reading anything into what the unbalanced query does:
        // an empty result from a broken index would otherwise look like a passing case.
        assertEquals("the fixture must restrict before this case means anything",
                List.of(READABLE_ID), search(CALLER_QUERY + " " + RESTRICTION));

        final String unbalanced = CALLER_QUERY + " x\\" + " " + RESTRICTION;

        final List<String> found;
        try {
            found = search(unbalanced);
        } catch (final Exception e) {
            if (!isQueryParseFailure(e)) {
                throw e;
            }
            Logger.info(this, "OpenSearch refused the unbalanced query: " + e.getMessage());
            return;
        }

        if (List.of(READABLE_ID).equals(found)) {
            fail("OpenSearch applied the restriction to a query ending in an unbalanced escape,"
                    + " so the premise the normalization rests on no longer holds on this engine");
        }
        assertTrue("the unbalanced query must come back unrestricted: " + found,
                found.contains(RESTRICTED_ID));
        Logger.info(this, "unbalanced query returned " + found);
    }

    /**
     * Whether the engine refused the query rather than failing for some other reason, such as an
     * index that could not be resolved.
     *
     * @param e the exception the search threw
     * @return {@code true} when the message names a parse failure, at any depth of the cause chain
     */
    private static boolean isQueryParseFailure(final Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            final String message = String.valueOf(cause.getMessage()).toLowerCase();
            if (message.contains("parse_exception") || message.contains("failed to parse query")
                    || message.contains("search_phase_execution_exception")) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    /**
     * Gives the {@code permissions} field the analyzer the production mapping gives it, so that a
     * permission token is indexed whole instead of being split on its punctuation.
     *
     * @param index the index to map the field on
     */
    private void mapPermissionsField(final String index) throws Exception {
        clientProvider.getClient().indices().putMapping(m -> m
                .index(index)
                .properties("permissions", p -> p.text(t -> t.analyzer("whitespace"))));
    }

    /**
     * Builds a document for this test.
     *
     * @param identifier the document's identifier and inode
     * @param marker the value the hand-written restriction asks for
     * @param role the role whose read grant the document carries
     * @return the document as JSON
     */
    private static String document(final String identifier, final String marker, final Role role) {
        // The permission token is written lower case because the search path lower-cases the query
        // and the whitespace analyzer does not fold case on either side.
        return "{\"identifier\":\"" + identifier + "\","
                + "\"inode\":\"" + identifier + "-inode\","
                + "\"contenttype\":\"" + CONTENT_TYPE + "\","
                + "\"permissions\":\"" + marker + " p" + role.getId() + ".1p\","
                + "\"live\":true}";
    }

    /**
     * Runs the given query text through the raw search path against the live index.
     *
     * @param queryText the query as the search code would hand it to the engine
     * @return the identifiers of the documents that matched, sorted
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
                .sorted()
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
            new DotConnect()
                    .setSQL("DELETE FROM indicies WHERE index_name LIKE ?")
                    .addParam("%" + RUN_ID + "%")
                    .loadResult();
        } catch (final Exception e) {
            Logger.warn(this, "Cleanup: error removing versioned DB rows: " + e.getMessage());
        }
    }
}
