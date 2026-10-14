package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeContentletIndexOperations;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndiciesAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeVersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
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
    private static final class RecordingOperations extends FakeContentletIndexOperations {

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
