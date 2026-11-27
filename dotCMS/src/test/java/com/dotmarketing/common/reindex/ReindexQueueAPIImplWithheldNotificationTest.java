package com.dotmarketing.common.reindex;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.IndexDocumentConstraints;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.exception.DotDataException;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for deciding when a saved content whose index document was withheld is reported
 * (review of #37924); how it is reported is {@link FailedIndexingNotifierTest}'s concern.
 *
 * <h2>What is at stake</h2>
 * <p>A save with the DEFER policy returns at once and is indexed later by the reindex thread. When
 * that document is withheld for exceeding the parser limits, the save had already reported
 * success: the content is stored but never searchable, and nothing in the UI says so. A
 * notification is sent when such an entry is parked — once, not per retry — for entries queued
 * by a save; a full reindex reports its failures in one summary instead.</p>
 */
public class ReindexQueueAPIImplWithheldNotificationTest {

    private static final int MAX = ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS;

    private static final String WITHHELD_REASON = new IndexDocumentViolation("id-1", "inode-1", 1L,
            new IndexDocumentConstraints.Violation(IndexDocumentConstraints.Kind.STRING_LENGTH,
                    "catchall", 42_000_921L, 20_000_000L)).toFailureReason();

    private ReindexQueueFactory factory;
    private FailedIndexingNotifier notifier;
    private ReindexQueueAPIImpl api;

    @Before
    public void setUp() {
        factory = mock(ReindexQueueFactory.class);
        notifier = mock(FailedIndexingNotifier.class);
        api = new ReindexQueueAPIImpl(factory, () -> notifier);
    }

    /**
     * Given Scenario: An entry queued by a save (NORMAL base) fails for the last time — it is
     * parked — because its document was withheld.
     * Expected Result: The failure is recorded and the content is reported once, with the
     * withheld document's reason.
     */
    @Test
    public void test_savedEntryParkedForWithheldDocument_notifies() throws Exception {
        final ReindexEntry entry = entry(Priority.NORMAL.dbValue() + MAX);

        api.markAsFailed(entry, WITHHELD_REASON);

        verify(factory).markAsFailed(entry, WITHHELD_REASON);
        verify(notifier).savedContentWithheld("id-1", WITHHELD_REASON);
    }

    /**
     * Given Scenario: A save queued two journal rows for the same identifier, and the first one
     * was already parked — and reported — for its withheld document; now the second one parks.
     * Expected Result: No second report for the same content.
     */
    @Test
    public void test_identifierAlreadyParkedInAnotherRow_noSecondNotification() throws Exception {
        final ReindexEntry entry = entry(Priority.NORMAL.dbValue() + MAX);
        when(factory.hasOtherParkedEntry("id-1", 1L)).thenReturn(true);

        api.markAsFailed(entry, WITHHELD_REASON);

        verify(factory).markAsFailed(entry, WITHHELD_REASON);
        verify(notifier, never()).savedContentWithheld(anyString(), anyString());
    }

    /**
     * Given Scenario: The same entry fails but still has retries left.
     * Expected Result: No report yet — only when it is parked.
     */
    @Test
    public void test_entryStillRetrying_noNotification() throws Exception {
        api.markAsFailed(entry(Priority.NORMAL.dbValue() + MAX - 1), WITHHELD_REASON);

        verify(notifier, never()).savedContentWithheld(anyString(), anyString());
    }

    /**
     * Given Scenario: An entry queued by a full reindex (REINDEX base) is parked for a withheld
     * document.
     * Expected Result: No per-content report; the reindex reports its failures once, when it
     * finishes.
     */
    @Test
    public void test_fullReindexEntryParked_noNotification() throws Exception {
        api.markAsFailed(entry(Priority.REINDEX.dbValue() + MAX), WITHHELD_REASON);

        verify(notifier, never()).savedContentWithheld(anyString(), anyString());
    }

    /**
     * Given Scenario: A saved entry is parked for a reason that is not a withheld document (for
     * instance the engine being unreachable).
     * Expected Result: No report: an outage would otherwise send one per saved content.
     */
    @Test
    public void test_parkedForOtherReason_noNotification() throws Exception {
        api.markAsFailed(entry(Priority.NORMAL.dbValue() + MAX),
                "Connect to http://opensearch:9200 failed: Connection refused");

        verify(notifier, never()).savedContentWithheld(anyString(), anyString());
    }

    /**
     * Given Scenario: Reporting the content fails.
     * Expected Result: The failure is still recorded and nothing is thrown.
     */
    @Test
    public void test_notificationFailure_doesNotBreakMarkAsFailed() throws Exception {
        doThrow(new DotDataException("notifications down")).when(notifier)
                .savedContentWithheld(anyString(), anyString());
        final ReindexEntry entry = entry(Priority.NORMAL.dbValue() + MAX);

        api.markAsFailed(entry, WITHHELD_REASON);

        verify(factory).markAsFailed(entry, WITHHELD_REASON);
    }

    private static ReindexEntry entry(final int priority) {
        return ReindexEntry.builder().id(1L).identToIndex("id-1").priority(priority).build();
    }
}
