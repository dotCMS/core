package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Wiring tests for the mapping-timeout guard in
 * {@link ContentletIndexAPIImpl#appendToBulkProcessor}: a journal entry whose mapping hangs or
 * fails must be marked failed through the existing {@code dist_reindex_journal} semantics while
 * the loop keeps processing the remaining entries (issue #36498).
 */
public class ContentletIndexAPIImplMappingTimeoutTest {

    private ScriptedIndexAPI api;
    private ReindexQueueAPI queueAPI;
    private final AtomicInteger timeoutSeconds = new AtomicInteger(1);

    /**
     * Impl whose per-entry mapping body is scripted per identifier, replacing the DB/ES-bound
     * {@code loadVersionInodes}/{@code toMap} work with test behavior. The timeout guard and
     * failure wiring under test are the real production code.
     */
    private final class ScriptedIndexAPI extends ContentletIndexAPIImpl {

        private final Map<String, Callable<Void>> bodies = new ConcurrentHashMap<>();
        private final List<String> mapped = Collections.synchronizedList(new ArrayList<>());
        private final AtomicReference<Thread> lastMappingThread = new AtomicReference<>();
        private final ReindexMappingRunner runner =
                new ReindexMappingRunner(timeoutSeconds::get, 4, () -> {});

        ScriptedIndexAPI() {
            super(mock(ContentletIndexOperations.class), mock(ContentletIndexOperations.class),
                    mock(IndexAPI.class), mock(IndiciesAPI.class),
                    mock(VersionedIndicesAPI.class));
        }

        @Override
        ReindexMappingRunner mappingRunner() {
            return runner;
        }

        @Override
        void mapEntryForProcessor(final IndexBulkProcessor proc, final ReindexEntry idx)
                throws Exception {
            lastMappingThread.set(Thread.currentThread());
            final Callable<Void> body = bodies.get(idx.getIdentToIndex());
            if (body != null) {
                body.call();
            }
            mapped.add(idx.getIdentToIndex());
        }
    }

    private static ReindexEntry entry(final String identifier) {
        return ReindexEntry.builder()
                .id(identifier.hashCode())
                .identToIndex(identifier)
                .priority(0)
                .serverId("test-server")
                .build();
    }

    @Before
    public void setUp() {
        timeoutSeconds.set(1);
        api = new ScriptedIndexAPI();
        queueAPI = mock(ReindexQueueAPI.class);
    }

    private void appendAll(final ReindexEntry... entries) throws Exception {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getReindexQueueAPI).thenReturn(queueAPI);
            api.appendToBulkProcessor(mock(IndexBulkProcessor.class), List.of(entries));
        }
    }

    /**
     * The incident scenario end to end at the wiring level: entry A wedges in storage I/O,
     * entry B is queued behind it. A must be marked failed with a timeout message and B must
     * still be mapped — the queue keeps draining.
     */
    @Test
    public void hungEntryIsMarkedFailedAndNextEntryStillMaps() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexEntry hung = entry("hung-content");
        final ReindexEntry healthy = entry("healthy-content");
        api.bodies.put("hung-content", () -> {
            while (!release.await(10, TimeUnit.SECONDS)) {
                // ignores interrupts, like a thread wedged in a native stat
            }
            return null;
        });
        try {
            appendAll(hung, healthy);
        } finally {
            release.countDown();
        }
        verify(queueAPI).markAsFailed(eq(hung), contains("Timed out after 1s"));
        verify(queueAPI, never()).markAsFailed(eq(healthy), contains("Timed out"));
        assertTrue("the healthy entry must still be mapped",
                api.mapped.contains("healthy-content"));
    }

    /** A mapping failure keeps its original message on the journal entry (existing semantics). */
    @Test
    public void mappingExceptionIsMarkedFailedWithOriginalMessage() throws Exception {
        final ReindexEntry broken = entry("broken-content");
        final ReindexEntry healthy = entry("healthy-content");
        api.bodies.put("broken-content", () -> {
            throw new IllegalStateException("malformed json");
        });
        appendAll(broken, healthy);
        verify(queueAPI).markAsFailed(eq(broken), eq("malformed json"));
        assertTrue(api.mapped.contains("healthy-content"));
    }

    /** Successful entries never touch the failure path. */
    @Test
    public void successfulEntriesAreNotMarkedFailed() throws Exception {
        appendAll(entry("content-a"), entry("content-b"));
        verify(queueAPI, never()).markAsFailed(eq(entry("content-a")), contains(""));
        verify(queueAPI, never()).markAsFailed(eq(entry("content-b")), contains(""));
        assertEquals(List.of("content-a", "content-b"), api.mapped);
    }

    /** With the guard enabled, mapping runs on a worker thread, not the reindex thread. */
    @Test
    public void guardedMappingRunsOffTheCallerThread() throws Exception {
        appendAll(entry("content-a"));
        assertTrue("mapping must run on a virtual worker thread",
                api.lastMappingThread.get().isVirtual());
    }

    /** Timeout 0 disables the guard: mapping runs inline on the caller (legacy behavior). */
    @Test
    public void timeoutZeroMapsInlineOnCallerThread() throws Exception {
        timeoutSeconds.set(0);
        appendAll(entry("content-a"));
        assertSame("timeout 0 must preserve the legacy inline path",
                Thread.currentThread(), api.lastMappingThread.get());
    }

    /** Timeout 0 also preserves the legacy failure wiring. */
    @Test
    public void timeoutZeroStillMarksFailuresAgainstTheJournal() throws Exception {
        timeoutSeconds.set(0);
        final ReindexEntry broken = entry("broken-content");
        api.bodies.put("broken-content", () -> {
            throw new IllegalStateException("malformed json");
        });
        appendAll(broken, entry("healthy-content"));
        verify(queueAPI).markAsFailed(eq(broken), eq("malformed json"));
        assertTrue(api.mapped.contains("healthy-content"));
    }
}
