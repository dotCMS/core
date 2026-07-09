package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the reindex mapping-timeout guard (issue #36498): a journal entry whose
 * mapping hangs in storage I/O must be marked failed through the real
 * {@code dist_reindex_journal} semantics while the rest of the batch keeps indexing through the
 * real mapping path.
 */
public class ContentletIndexAPIImplMappingTimeoutIT extends IntegrationTestBase {

    private static final String TIMEOUT_KEY = "REINDEX_CONTENTLET_MAPPING_TIMEOUT_SECONDS";

    private static ReindexQueueAPI reindexQueueAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        reindexQueueAPI = APILocator.getReindexQueueAPI();
        // Keep the background reindex thread from claiming our journal entries, and keep
        // markAsFailed from unpausing it mid-test.
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
        ReindexThread.pause();
    }

    @AfterClass
    public static void cleanUp() {
        Config.setProperty(TIMEOUT_KEY, null);
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
        ReindexThread.unpause();
    }

    /**
     * Real {@link ContentletIndexAPIImpl} whose mapping wedges "in storage I/O" for one chosen
     * identifier; every other entry goes through the real load/map/index path.
     */
    private static final class WedgedIndexAPI extends ContentletIndexAPIImpl {

        private final String hungIdentifier;
        private final CountDownLatch release;
        private final List<String> mapped = Collections.synchronizedList(new ArrayList<>());

        WedgedIndexAPI(final String hungIdentifier, final CountDownLatch release) {
            this.hungIdentifier = hungIdentifier;
            this.release = release;
        }

        @Override
        void mapEntryForProcessor(final IndexBulkProcessor proc, final ReindexEntry idx)
                throws Exception {
            if (hungIdentifier.equals(idx.getIdentToIndex())) {
                // Wedge like an unanswered native stat: ignore interrupts until released.
                while (!release.await(30, TimeUnit.SECONDS)) {
                    // keep waiting
                }
                return;
            }
            super.mapEntryForProcessor(proc, idx);
            mapped.add(idx.getIdentToIndex());
        }
    }

    private static Map<String, Object> journalRow(final String identifier) throws Exception {
        final DotConnect dc = new DotConnect();
        dc.setSQL("select priority, index_val from dist_reindex_journal where ident_to_index = ?");
        dc.addParam(identifier);
        final List<Map<String, Object>> rows = dc.loadObjectResults();
        assertFalse("journal row must exist for " + identifier, rows.isEmpty());
        return rows.get(0);
    }

    /**
     * The production incident at integration level: one entry wedges in storage I/O, a second
     * entry is queued behind it. The wedged entry must be marked failed in
     * {@code dist_reindex_journal} (priority bumped, cause recorded) and the healthy entry must
     * still be mapped through the real pipeline.
     */
    @Test
    public void hungMappingFailsItsJournalEntryAndTheBatchContinues() throws Exception {
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        final Contentlet hungContent = new ContentletDataGen(type.id()).nextPersisted();
        final Contentlet healthyContent = new ContentletDataGen(type.id()).nextPersisted();
        final String hungId = hungContent.getIdentifier();
        final String healthyId = healthyContent.getIdentifier();

        reindexQueueAPI.deleteReindexAndFailedRecords();
        reindexQueueAPI.addIdentifierReindex(hungId);
        reindexQueueAPI.addIdentifierReindex(healthyId);

        final Map<String, ReindexEntry> entries = reindexQueueAPI.findContentToReindex();
        assertTrue("hung entry must be claimed from the queue", entries.containsKey(hungId));
        assertTrue("healthy entry must be claimed from the queue",
                entries.containsKey(healthyId));
        final int originalPriority = entries.get(hungId).getPriority();

        Config.setProperty(TIMEOUT_KEY, "2");
        final CountDownLatch release = new CountDownLatch(1);
        final WedgedIndexAPI indexAPI = new WedgedIndexAPI(hungId, release);
        try {
            // Journal bookkeeping for successful docs is BulkProcessorListener's job and is not
            // under test here — a no-op listener keeps the batch flow real without it.
            final IndexBulkListener listener = new IndexBulkListener() {
                @Override
                public void beforeBulk(final long executionId, final int actionCount) {
                    // no-op
                }

                @Override
                public void afterBulk(final long executionId,
                        final List<IndexBulkItemResult> results) {
                    // no-op
                }

                @Override
                public void afterBulk(final long executionId, final Throwable failure) {
                    // no-op
                }
            };
            try (final IndexBulkProcessor processor = indexAPI.createBulkProcessor(listener)) {
                indexAPI.appendToBulkProcessor(processor, entries.values());
            }
        } finally {
            release.countDown();
            Config.setProperty(TIMEOUT_KEY, null);
        }

        // The wedged entry followed the existing journal failure semantics: still queued for
        // retry, priority bumped by one, timeout recorded as the failure cause.
        final Map<String, Object> hungRow = journalRow(hungId);
        assertEquals("failure must bump the journal priority by one",
                originalPriority + 1, ((Number) hungRow.get("priority")).intValue());
        assertTrue("the timeout must be recorded as the failure cause, got: "
                        + hungRow.get("index_val"),
                String.valueOf(hungRow.get("index_val")).contains("Timed out after 2s"));

        // The healthy entry went through the real load/map path and was never marked failed.
        assertTrue("healthy entry must be mapped through the real pipeline",
                indexAPI.mapped.contains(healthyId));
        final Map<String, Object> healthyRow = journalRow(healthyId);
        assertEquals("healthy entry priority must be untouched",
                entries.get(healthyId).getPriority(),
                ((Number) healthyRow.get("priority")).intValue());
        assertNull("healthy entry must have no failure cause", healthyRow.get("index_val"));
    }
}
