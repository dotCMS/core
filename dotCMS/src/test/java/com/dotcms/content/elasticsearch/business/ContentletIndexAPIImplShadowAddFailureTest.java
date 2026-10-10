package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexDocumentConstraints;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Unit tests for how {@link ContentletIndexAPIImpl} queues documents when the OpenSearch shadow
 * fails to accept one in Phase 1 (#37269, AC-004).
 *
 * <h2>What is at stake</h2>
 * <p>In Phase 1 Elasticsearch is the source of truth and OpenSearch only mirrors its writes;
 * a shadow failure must be logged and otherwise ignored. Each document is added to every
 * provider's processor in turn, ES first. The OpenSearch adapter parses the document while
 * adding it, so a document it cannot parse throws right there. Today that exception escapes the
 * queueing loop: the journal entry is marked failed and the entry's remaining documents never
 * reach Elasticsearch — a shadow problem breaking the primary.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
 *       -Dtest=ContentletIndexAPIImplShadowAddFailureTest
 * </pre>
 */
public class ContentletIndexAPIImplShadowAddFailureTest {

    private static final String ES_WORKING = "es-working";
    private static final String OS_WORKING = "os-working";
    private static final String MAPPING = "{\"title\":\"x\"}";

    private ContentletIndexOperations esOps;
    private ContentletIndexOperations osOps;
    private IndexBulkProcessor esProc;
    private IndexBulkProcessor osProc;
    private ReindexQueueAPI queueAPI;
    private TwoDocumentIndexAPI api;
    private ReindexEntry entry;

    /**
     * Impl whose mapping step returns two working documents of one identifier, replacing the
     * DB-bound version loading. The queueing loop under test is the real production code.
     */
    private static final class TwoDocumentIndexAPI extends ContentletIndexAPIImpl {

        private final ReindexMappingRunner runner =
                new ReindexMappingRunner(() -> 5, 2, 8, () -> {});

        TwoDocumentIndexAPI(final ContentletIndexOperations esOps,
                final ContentletIndexOperations osOps, final IndiciesAPI indiciesAPI,
                final VersionedIndicesAPI versionedIndicesAPI) {
            super(esOps, osOps, mock(IndexAPI.class), indiciesAPI, versionedIndicesAPI);
        }

        @Override
        ReindexMappingRunner mappingRunner() {
            return runner;
        }

        /** When set, the mapping also yields this rejected document ahead of the healthy ones. */
        private IndexDocumentViolation rejection;

        @Override
        List<MappedDocument> mapEntry(final ReindexEntry idx) {
            final Contentlet contentlet = mock(Contentlet.class);
            when(contentlet.getIdentifier()).thenReturn(idx.getIdentToIndex());
            final List<MappedDocument> documents = new ArrayList<>();
            if (rejection != null) {
                documents.add(MappedDocument.rejected(contentlet, "doc-0", rejection));
            }
            documents.add(new MappedDocument(contentlet, "doc-1", MAPPING, true, false));
            documents.add(new MappedDocument(contentlet, "doc-2", MAPPING, true, false));
            return documents;
        }
    }

    @Before
    public void setUp() throws Exception {
        esOps = mock(ContentletIndexOperations.class);
        osOps = mock(ContentletIndexOperations.class);
        esProc = mock(IndexBulkProcessor.class);
        osProc = mock(IndexBulkProcessor.class);
        queueAPI = mock(ReindexQueueAPI.class);

        final IndiciesAPI indiciesAPI = mock(IndiciesAPI.class);
        when(indiciesAPI.loadIndicies())
                .thenReturn(new IndiciesInfo.Builder().setWorking(ES_WORKING).build());
        final VersionedIndices versioned = mock(VersionedIndices.class);
        when(versioned.working()).thenReturn(Optional.of(OS_WORKING));
        final VersionedIndicesAPI versionedIndicesAPI = mock(VersionedIndicesAPI.class);
        when(versionedIndicesAPI.loadDefaultVersionedIndices()).thenReturn(Optional.of(versioned));

        api = new TwoDocumentIndexAPI(esOps, osOps, indiciesAPI, versionedIndicesAPI);
        entry = ReindexEntry.builder().id(1L).identToIndex("id-1").priority(0).build();
    }

    /**
     * Given Scenario: Phase 1 composite of [ES primary, OS shadow]; the shadow throws when a
     * document is added to it.
     * Expected Result: Both documents of the entry are queued to Elasticsearch and the entry is
     * not marked failed. (The shadow-failure log line is not asserted: it is a static interface
     * method on {@code IndexConfigHelper}.)
     */
    @Test
    public void test_shadowAddFailure_primaryGetsEveryDocument_entryNotMarked() throws Exception {
        doThrow(new IllegalStateException("shadow cannot parse document"))
                .when(osOps).addIndexOpToProcessor(eq(osProc), anyString(), anyString(), anyString());

        append();

        verify(esOps).addIndexOpToProcessor(esProc, ES_WORKING, "doc-1", MAPPING);
        verify(esOps).addIndexOpToProcessor(esProc, ES_WORKING, "doc-2", MAPPING);
        verify(queueAPI, never()).markAsFailed(any(), any());
    }

    /**
     * Given Scenario: Same composite, but it is the <b>primary</b> that throws on add (control).
     * Expected Result: The entry is marked failed, exactly as today — only shadow failures are
     * isolated.
     */
    @Test
    public void test_primaryAddFailure_stillMarksEntry() throws Exception {
        doThrow(new IllegalStateException("primary rejected document"))
                .when(esOps).addIndexOpToProcessor(eq(esProc), anyString(), anyString(), anyString());

        append();

        verify(queueAPI).markAsFailed(eq(entry), eq("primary rejected document"));
    }

    /**
     * Given Scenario: One document of the entry was withheld for exceeding the parser limit, and
     * queueing a healthy sibling then fails on the <b>primary</b>.
     * Expected Result: The entry's last recorded reason still names the withheld document's
     * field and size; the primary's error does not replace it (AC-002).
     */
    @Test
    public void test_primaryAddFailureAfterRejection_keepsViolationReason() throws Exception {
        final IndexDocumentViolation violation = new IndexDocumentViolation("id-1", "inode-0", 1L,
                new IndexDocumentConstraints.Violation(IndexDocumentConstraints.Kind.STRING_LENGTH,
                        "body", 20_000_001L, 20_000_000L));
        api.rejection = violation;
        doThrow(new IllegalStateException("primary rejected document"))
                .when(esOps).addIndexOpToProcessor(eq(esProc), anyString(), anyString(), anyString());

        append();

        final ArgumentCaptor<String> reasons = ArgumentCaptor.forClass(String.class);
        verify(queueAPI, atLeastOnce()).markAsFailed(eq(entry), reasons.capture());
        final String lastReason = reasons.getValue();
        assertTrue("the last reason must keep the violation: " + lastReason,
                lastReason.contains(violation.toFailureReason()));
        assertTrue("the last reason must also carry the primary's error: " + lastReason,
                lastReason.contains("primary rejected document"));
    }

    /** Appends the entry through a Phase 1 composite with the journal API mocked. */
    private void append() throws Exception {
        final ContentletIndexAPIImpl.CompositeBulkProcessor composite =
                new ContentletIndexAPIImpl.CompositeBulkProcessor(List.of(
                        new ContentletIndexAPIImpl.CompositeBulkProcessor.Entry(esOps, esProc, false),
                        new ContentletIndexAPIImpl.CompositeBulkProcessor.Entry(osOps, osProc, true)));
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getReindexQueueAPI).thenReturn(queueAPI);
            api.appendToBulkProcessor(composite, List.of(entry));
        }
    }
}
