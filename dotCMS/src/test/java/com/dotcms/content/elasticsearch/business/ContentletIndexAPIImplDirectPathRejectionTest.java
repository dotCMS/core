package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeContentletIndexOperations;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndiciesAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeVersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for documents the engine clients' parser would refuse when they are indexed
 * <b>directly</b>, without going through the reindex queue (#37269, review of #37924).
 *
 * <h2>What is at stake</h2>
 * <p>The reindex thread already withholds such a document on its own. The direct path —
 * {@link ContentletIndexAPIImpl#addContentToIndex(List)} for saves with a WAIT_FOR or FORCE
 * policy, related content pulled in by {@code includeDependencies}, and list callers such as
 * {@code ESContentFactoryImpl.reindexReplacedUserContent} — builds one bulk request for the whole
 * list and had no check. One oversized document made the engine client throw while the request
 * was being built, so nothing was sent and every healthy document of the list was lost.</p>
 *
 * <p>Agreed behaviour: the offending documents are left out of the request, the healthy ones are
 * sent, and then one {@link DotRuntimeException} is thrown whose message names every offending
 * document (identifier, inode, language, field and size). The caller still sees an exception, as
 * today; nothing is written to the reindex queue.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
 *       -Dtest=ContentletIndexAPIImplDirectPathRejectionTest
 * </pre>
 */
public class ContentletIndexAPIImplDirectPathRejectionTest {

    /** One character over Jackson's default maximum string length (20,000,000). */
    private static final int OVERSIZED_LENGTH = 20_000_001;

    private static final String ES_WORKING = "es-working";
    private static final String ES_LIVE = "es-live";
    private static final String OS_WORKING = "os-working";

    private RecordingOperations es;
    private RecordingOperations os;
    private ContentletIndexAPIImpl api;
    private ContentMappingAPI mappingAPI;

    /** A bulk request that remembers the document ids added to it. */
    private static final class RecordingBulkRequest implements IndexBulkRequest {

        final List<String> documentIds = new ArrayList<>();

        @Override
        public int size() {
            return documentIds.size();
        }
    }

    /** Provider operations that record what is added to and sent from each request. */
    private static class RecordingOperations extends FakeContentletIndexOperations {

        /** Document ids of every request sent with {@code putToIndex}, in order. */
        final List<List<String>> sent = new ArrayList<>();

        @Override
        public IndexBulkRequest createBulkRequest() {
            return new RecordingBulkRequest();
        }

        @Override
        public void addIndexOp(final IndexBulkRequest req, final String index, final String docId,
                final String json) {
            ((RecordingBulkRequest) req).documentIds.add(docId);
        }

        @Override
        public void addDeleteOp(final IndexBulkRequest req, final String index,
                final String docId) {
            ((RecordingBulkRequest) req).documentIds.add("delete:" + docId);
        }

        @Override
        public void setRefreshPolicy(final IndexBulkRequest req,
                final IndexBulkRequest.RefreshPolicy policy) {
            // not relevant here
        }

        @Override
        public void putToIndex(final IndexBulkRequest req) {
            sent.add(List.copyOf(((RecordingBulkRequest) req).documentIds));
        }

        /** Every document id sent to this provider. */
        List<String> sentIds() {
            final List<String> ids = new ArrayList<>();
            sent.forEach(ids::addAll);
            return ids;
        }
    }

    @Before
    public void setUp() throws Exception {
        es = new RecordingOperations();
        os = new RecordingOperations();
        final FakeIndiciesAPI indicies = new FakeIndiciesAPI();
        indicies.setWorking(ES_WORKING);
        indicies.setLive(ES_LIVE);
        final FakeVersionedIndicesAPI versioned = new FakeVersionedIndicesAPI();
        final VersionedIndices osIndices = mock(VersionedIndices.class);
        when(osIndices.working()).thenReturn(Optional.of(OS_WORKING));
        versioned.stored = osIndices;
        api = new ContentletIndexAPIImpl(es, os, new FakeIndexAPI(List.of()), indicies, versioned);
        mappingAPI = mock(ContentMappingAPI.class);
    }

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    /**
     * Given Scenario: Phase 0. Three working contentlets are indexed directly; the middle one has
     * a string longer than the parser accepts.
     * Expected Result: the two healthy documents are sent to Elasticsearch, the oversized one is
     * not, and a {@link DotRuntimeException} naming the oversized document's identifier, inode,
     * field and real length is thrown after the send.
     */
    @Test
    public void test_directIndex_oneOversized_healthySent_thenThrowsNamingIt() throws Exception {
        setPhase(0);
        final Contentlet first = contentlet("id-1", "inode-1", "small body");
        final Contentlet oversized = contentlet("id-2", "inode-2", "x".repeat(OVERSIZED_LENGTH));
        final Contentlet third = contentlet("id-3", "inode-3", "small body");

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> addContentToIndex(List.of(first, oversized, third)));

        assertEquals("the healthy documents must be sent",
                List.of(docId(first), docId(third)), sortedCopy(es.sentIds()));
        assertNamesOffender(thrown.getMessage(), oversized);
    }

    /**
     * Given Scenario: Phase 0. Four contentlets are indexed directly; two of them are oversized.
     * Expected Result: the two healthy documents are sent, and a single exception lists both
     * offending documents.
     */
    @Test
    public void test_directIndex_twoOversized_oneExceptionNamesBoth() throws Exception {
        setPhase(0);
        final Contentlet healthyA = contentlet("id-1", "inode-1", "small body");
        final Contentlet oversizedA = contentlet("id-2", "inode-2", "x".repeat(OVERSIZED_LENGTH));
        final Contentlet healthyB = contentlet("id-3", "inode-3", "small body");
        final Contentlet oversizedB = contentlet("id-4", "inode-4", "y".repeat(OVERSIZED_LENGTH));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> addContentToIndex(List.of(healthyA, oversizedA, healthyB, oversizedB)));

        assertEquals("the healthy documents must be sent",
                List.of(docId(healthyA), docId(healthyB)), sortedCopy(es.sentIds()));
        assertNamesOffender(thrown.getMessage(), oversizedA);
        assertNamesOffender(thrown.getMessage(), oversizedB);
    }

    /**
     * Given Scenario: Phase 0. The only contentlet indexed directly is oversized.
     * Expected Result: it is not sent and the exception names it.
     */
    @Test
    public void test_directIndex_onlyOversized_notSent_throws() throws Exception {
        setPhase(0);
        final Contentlet oversized = contentlet("id-1", "inode-1", "x".repeat(OVERSIZED_LENGTH));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> addContentToIndex(List.of(oversized)));

        assertFalse("the oversized document must not be sent: " + es.sentIds(),
                es.sentIds().contains(docId(oversized)));
        assertNamesOffender(thrown.getMessage(), oversized);
    }

    /**
     * Given Scenario: Phase 1 (dual write). Two contentlets are indexed directly; one is
     * oversized.
     * Expected Result: the healthy document reaches both engines, the oversized one reaches
     * neither, and the exception names it.
     */
    @Test
    public void test_directIndex_dualWrite_oversizedWithheldFromBothEngines() throws Exception {
        setPhase(1);
        final Contentlet healthy = contentlet("id-1", "inode-1", "small body");
        final Contentlet oversized = contentlet("id-2", "inode-2", "x".repeat(OVERSIZED_LENGTH));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> addContentToIndex(List.of(healthy, oversized)));

        assertEquals("Elasticsearch must receive only the healthy document",
                List.of(docId(healthy)), es.sentIds());
        assertEquals("OpenSearch must receive only the healthy document",
                List.of(docId(healthy)), os.sentIds());
        assertNamesOffender(thrown.getMessage(), oversized);
    }

    /**
     * Given Scenario: Phase 0. Every contentlet indexed directly is healthy (control).
     * Expected Result: all of them are sent in one request and nothing is thrown — the behaviour
     * when no document is oversized must not change.
     */
    @Test
    public void test_directIndex_allHealthy_unchanged() throws Exception {
        setPhase(0);
        final Contentlet first = contentlet("id-1", "inode-1", "small body");
        final Contentlet second = contentlet("id-2", "inode-2", "small body");

        addContentToIndex(List.of(first, second));

        assertEquals("one request with both documents",
                List.of(List.of(docId(first), docId(second))),
                List.of(sortedCopy(es.sent.get(0))));
        assertEquals(1, es.sent.size());
    }

    /**
     * Given Scenario: Phase 0. A journal entry is appended to a plain bulk request with
     * {@link ContentletIndexAPIImpl#appendBulkRequest(IndexBulkRequest, ReindexEntry)}; its live
     * version is healthy and its working version is oversized.
     * Expected Result: the live document is added to the request, the oversized working document
     * is not, and the entry is marked failed with a reason naming the oversized version. Nothing
     * is thrown: this path reports through the journal, as it does for any other failure.
     */
    @Test
    public void test_appendBulkRequest_oversizedVersion_withheld_entryMarkedFailed()
            throws Exception {
        setPhase(0);
        final Contentlet live = contentlet("id-1", "inode-live", "small body");
        when(live.isWorking()).thenReturn(false);
        when(live.isLive()).thenReturn(true);
        final Contentlet working = contentlet("id-1", "inode-working", "x".repeat(OVERSIZED_LENGTH));
        final ReindexEntry entry = ReindexEntry.builder().id(1L).identToIndex("id-1")
                .priority(0).build();

        final ContentletVersionInfo versionInfo = mock(ContentletVersionInfo.class);
        when(versionInfo.getWorkingInode()).thenReturn("inode-working");
        when(versionInfo.getLiveInode()).thenReturn("inode-live");
        final VersionableAPI versionableAPI = mock(VersionableAPI.class);
        when(versionableAPI.findContentletVersionInfos("id-1")).thenReturn(List.of(versionInfo));
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.findInDb("inode-working")).thenReturn(Optional.of(working));
        when(contentletAPI.findInDb("inode-live")).thenReturn(Optional.of(live));
        final ReindexQueueAPI queueAPI = mock(ReindexQueueAPI.class);

        final RecordingBulkRequest request = new RecordingBulkRequest();
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getContentMappingAPI).thenReturn(mappingAPI);
            apiLocator.when(APILocator::getVersionableAPI).thenReturn(versionableAPI);
            apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
            apiLocator.when(APILocator::getReindexQueueAPI).thenReturn(queueAPI);
            api.appendBulkRequest(request, entry);
        }

        assertEquals("only the healthy live document may be added",
                List.of(docId(live)), request.documentIds);
        verify(queueAPI).markAsFailed(eq(entry),
                org.mockito.ArgumentMatchers.argThat(reason -> reason != null
                        && reason.contains("inode-working")
                        && reason.contains("field 'body' is 20,000,001 chars")));
        verify(queueAPI, never()).markAsFailed(eq(entry),
                org.mockito.ArgumentMatchers.argThat(reason -> reason != null
                        && reason.contains("inode-live")));
    }

    /**
     * Given Scenario: Phase 3 with no OpenSearch index record yet, so no write provider can take
     * the documents.
     * Expected Result: Nothing is mapped and no contentlet is marked as reindexed — as before the
     * change, when the provider loop never ran.
     */
    @Test
    public void test_directIndex_noWriteProvider_nothingMappedOrMarked() throws Exception {
        setPhase(3);
        final FakeVersionedIndicesAPI noOsRecord = new FakeVersionedIndicesAPI();
        api = new ContentletIndexAPIImpl(es, os, new FakeIndexAPI(List.of()), new FakeIndiciesAPI(),
                noOsRecord);
        final Contentlet contentlet = contentlet("id-1", "inode-1", "small body");

        addContentToIndex(List.of(contentlet));

        verify(mappingAPI, never()).toMap(contentlet);
        verify(contentlet, never()).markAsReindexed();
    }

    /**
     * Given Scenario: Phase 0. A list with one oversized document is indexed directly and the
     * engine then rejects the request of the healthy ones.
     * Expected Result: The caller sees the engine's error, and the withheld document is still
     * reported: the error carries, as a suppressed exception, one naming it.
     */
    @Test
    public void test_directIndex_sendFails_withheldStillReported() throws Exception {
        setPhase(0);
        final RecordingOperations failing = new RecordingOperations() {
            @Override
            public void putToIndex(final IndexBulkRequest req) {
                throw new DotRuntimeException("bulk write rejected");
            }
        };
        final FakeIndiciesAPI indicies = new FakeIndiciesAPI();
        indicies.setWorking(ES_WORKING);
        api = new ContentletIndexAPIImpl(failing, os, new FakeIndexAPI(List.of()), indicies,
                new FakeVersionedIndicesAPI());
        final Contentlet healthy = contentlet("id-1", "inode-1", "small body");
        final Contentlet oversized = contentlet("id-2", "inode-2", "x".repeat(OVERSIZED_LENGTH));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> addContentToIndex(List.of(healthy, oversized)));

        assertEquals("bulk write rejected", thrown.getMessage());
        assertEquals("the withheld document must travel with the engine's error",
                1, thrown.getSuppressed().length);
        assertNamesOffender(thrown.getSuppressed()[0].getMessage(), oversized);
    }

    /**
     * Given Scenario: Phase 0. A contentlet is removed from the index with a FORCE policy; it has
     * one relationship, and the related content found for it is one healthy contentlet and one
     * whose document is oversized.
     * Expected Result: The healthy related document is reindexed, the oversized one is not and is
     * named in a warning, the removal of the deleted contentlet is still sent, and nothing is
     * thrown — reindexing related content is clean-up and must not block the removal, nor drop a
     * withheld document silently.
     */
    @Test
    public void test_removeFromIndex_oversizedRelated_loggedAndRemovalStillSent() throws Exception {
        setPhase(0);
        final Contentlet healthyRelated = relatedContentlet("id-related-1", "inode-r1", "small body");
        final Contentlet oversizedRelated = relatedContentlet("id-related-2", "inode-r2",
                "x".repeat(OVERSIZED_LENGTH));
        final Relationship relationship = relationship("Parent-Child");

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            removeFromIndex(List.of(relationship),
                    query -> List.of(healthyRelated, oversizedRelated));

            logger.verify(() -> Logger.warn(any(Object.class),
                    argThat((String message) -> message != null
                            && message.contains("(inode inode-r2)"))));
        }

        final List<String> sent = es.sentIds();
        assertTrue("the healthy related document must be reindexed: " + sent,
                sent.contains(docId(healthyRelated)));
        assertFalse("the oversized related document must not be sent: " + sent,
                sent.contains(docId(oversizedRelated)));
        assertTrue("the removal of the deleted contentlet must still be sent: " + sent,
                sent.contains("delete:id-deleted_1_DEFAULT"));
    }

    /**
     * Given Scenario: Phase 0. The deleted contentlet has two relationships; the first one's
     * related content includes an oversized document, and searching the second one's related
     * content fails with a {@link DotDataException}.
     * Expected Result: The search failure reaches the caller as before (wrapped by
     * {@code removeContentFromIndex}), carrying, as a suppressed exception, one naming the
     * document withheld for the first relationship.
     */
    @Test
    public void test_removeFromIndex_checkedFailureAfterWithheld_withheldTravelsWithIt()
            throws Exception {
        setPhase(0);
        final Contentlet oversizedRelated = relatedContentlet("id-related-2", "inode-r2",
                "x".repeat(OVERSIZED_LENGTH));
        final Relationship first = relationship("First-Rel");
        final Relationship second = relationship("Second-Rel");

        final Exception thrown = assertThrows(Exception.class,
                () -> removeFromIndex(List.of(first, second), query -> {
                    if (query.contains("Second-Rel")) {
                        throw new DotDataException("related search failed");
                    }
                    return List.of(oversizedRelated);
                }));

        // removeContentFromIndex wraps it in a DotHibernateException, itself a DotDataException
        // with the same message: look for the original by its exact type.
        Throwable failure = thrown;
        while (failure != null && failure.getClass() != DotDataException.class) {
            failure = failure.getCause();
        }
        assertNotNull("the search failure must still reach the caller: " + thrown, failure);
        assertEquals("related search failed", failure.getMessage());
        assertEquals("the withheld document must travel with the failure",
                1, failure.getSuppressed().length);
        assertNamesOffender(failure.getSuppressed()[0].getMessage(), oversizedRelated);
    }

    // -------------------------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------------------------

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /**
     * A working contentlet in language 1 whose index document is {@code {"body": <body>}}. The
     * policy is DEFER so the direct path sends the request without touching the query caches.
     */
    private Contentlet contentlet(final String identifier, final String inode, final String body)
            throws Exception {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn(identifier);
        when(contentlet.getInode()).thenReturn(inode);
        when(contentlet.getLanguageId()).thenReturn(1L);
        when(contentlet.getVariantId()).thenReturn("DEFAULT");
        when(contentlet.getIndexPolicy()).thenReturn(IndexPolicy.DEFER);
        when(contentlet.isWorking()).thenReturn(true);
        when(contentlet.isLive()).thenReturn(false);
        final Map<String, Object> document = new HashMap<>();
        document.put("body", body);
        when(mappingAPI.toMap(contentlet)).thenReturn(document);
        return contentlet;
    }

    /** Related content found for a deleted contentlet, mapped as {@code {"body": <body>}}. */
    private Contentlet relatedContentlet(final String identifier, final String inode,
            final String body) throws Exception {
        final Contentlet contentlet = contentlet(identifier, inode, body);
        when(contentlet.getIndexPolicy()).thenReturn(IndexPolicy.FORCE);
        return contentlet;
    }

    private static Relationship relationship(final String typeValue) {
        final Relationship relationship = mock(Relationship.class);
        when(relationship.getRelationTypeValue()).thenReturn(typeValue);
        return relationship;
    }

    /** Answers the related-content search for a relationship query. */
    @FunctionalInterface
    private interface RelatedSearch {
        List<Contentlet> find(String query) throws Exception;
    }

    /**
     * Removes a working contentlet ({@code id-deleted}, FORCE policy) from the index with the
     * given relationships, the related content returned by {@code search}, and the APIs mocked.
     */
    private void removeFromIndex(final List<Relationship> relationships,
            final RelatedSearch search) throws Exception {
        final ContentType type = mock(ContentType.class);
        final Contentlet deleted = mock(Contentlet.class);
        when(deleted.getIdentifier()).thenReturn("id-deleted");
        when(deleted.getInode()).thenReturn("inode-deleted");
        when(deleted.getLanguageId()).thenReturn(1L);
        when(deleted.getVariantId()).thenReturn("DEFAULT");
        when(deleted.getContentType()).thenReturn(type);
        when(deleted.getIndexPolicy()).thenReturn(IndexPolicy.FORCE);
        when(deleted.getIndexPolicyDependencies()).thenReturn(IndexPolicy.FORCE);

        final RelationshipAPI relationshipAPI = mock(RelationshipAPI.class);
        when(relationshipAPI.byContentType(type)).thenReturn(relationships);
        final User systemUser = mock(User.class);
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.search(anyString(), eq(-1), eq(0), isNull(), eq(systemUser), eq(false)))
                .thenAnswer(invocation -> search.find(invocation.getArgument(0)));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class);
                MockedStatic<CacheLocator> cacheLocator = mockStatic(CacheLocator.class,
                        RETURNS_DEEP_STUBS)) {
            apiLocator.when(APILocator::getContentMappingAPI).thenReturn(mappingAPI);
            apiLocator.when(APILocator::getRelationshipAPI).thenReturn(relationshipAPI);
            apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.removeContentFromIndex(deleted, false);
        }
    }

    /** Runs {@link ContentletIndexAPIImpl#addContentToIndex(List)} with the mapping API mocked. */
    private void addContentToIndex(final List<Contentlet> contentlets) {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getContentMappingAPI).thenReturn(mappingAPI);
            api.addContentToIndex(contentlets);
        }
    }

    private static String docId(final Contentlet contentlet) {
        return contentlet.getIdentifier() + "_" + contentlet.getLanguageId() + "_"
                + contentlet.getVariantId();
    }

    private static List<String> sortedCopy(final List<String> ids) {
        final List<String> copy = new ArrayList<>(ids);
        copy.sort(String::compareTo);
        return copy;
    }

    /** Asserts the message names the offending document: identifier, inode, field and length. */
    private static void assertNamesOffender(final String message, final Contentlet offender) {
        assertTrue("message must name the identifier and language: " + message,
                message.contains("document " + offender.getIdentifier() + "_1"));
        assertTrue("message must name the inode: " + message,
                message.contains("(inode " + offender.getInode() + ")"));
        assertTrue("message must name the field and its real length: " + message,
                message.contains("field 'body' is 20,000,001 chars"));
    }
}
