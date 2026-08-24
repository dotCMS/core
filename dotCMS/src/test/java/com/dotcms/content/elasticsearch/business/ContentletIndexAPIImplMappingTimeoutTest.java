package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
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
 * the loop keeps processing the remaining entries (issue #36498) — but an entry rejected because
 * the pool itself is unavailable must be left completely untouched (issue #37038).
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
        private final ReindexMappingRunner runner;

        ScriptedIndexAPI(final int maxAbandoned) {
            super(mock(ContentletIndexOperations.class), mock(ContentletIndexOperations.class),
                    mock(IndexAPI.class), mock(IndiciesAPI.class),
                    mock(VersionedIndicesAPI.class));
            this.runner = new ReindexMappingRunner(timeoutSeconds::get, 4, maxAbandoned, () -> {});
        }

        @Override
        ReindexMappingRunner mappingRunner() {
            return runner;
        }

        @Override
        List<MappedDocument> mapEntry(final ReindexEntry idx) throws Exception {
            lastMappingThread.set(Thread.currentThread());
            final Callable<Void> body = bodies.get(idx.getIdentToIndex());
            if (body != null) {
                body.call();
            }
            mapped.add(idx.getIdentToIndex());
            return List.of();
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

    /** Blocks "uninterruptibly" like a native stat: ignores interrupts until released. */
    private static Callable<Void> wedge(final CountDownLatch release) {
        return () -> {
            while (true) {
                try {
                    if (release.await(10, TimeUnit.SECONDS)) {
                        return null;
                    }
                } catch (final InterruptedException ignored) {
                    // a thread wedged in native I/O does not respond to interrupt
                }
            }
        };
    }

    @Before
    public void setUp() {
        timeoutSeconds.set(1);
        api = new ScriptedIndexAPI(8);
        queueAPI = mock(ReindexQueueAPI.class);
    }

    private void appendAll(final ReindexEntry... entries) throws Exception {
        appendAll(api, entries);
    }

    private void appendAll(final ScriptedIndexAPI target, final ReindexEntry... entries)
            throws Exception {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getReindexQueueAPI).thenReturn(queueAPI);
            target.appendToBulkProcessor(mock(IndexBulkProcessor.class), List.of(entries));
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
        api.bodies.put("hung-content", wedge(release));
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

    /**
     * The core regression of issue #37038: once the pool stops accepting work, the entries that
     * get rejected must NOT be charged a failure. Charging them is what marched 300K entries past
     * {@code Priority.ERROR}, where the queue loader never reads them again.
     */
    @Test
    public void poolRejectionNeverFailsTheJournalEntry() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ScriptedIndexAPI degrading = new ScriptedIndexAPI(1);
        final ReindexEntry hung = entry("hung-content");
        final ReindexEntry rejected = entry("rejected-content");
        degrading.bodies.put("hung-content", wedge(release));
        try {
            appendAll(degrading, hung, rejected);
            fail("an open circuit must abort the batch");
        } catch (final ReindexPoolExhaustedException expected) {
            assertTrue("the circuit must be reported as open", expected.isCircuitOpen());
        } finally {
            release.countDown();
        }
        // The wedged entry is genuinely this entry's failure and is charged as before...
        verify(queueAPI).markAsFailed(eq(hung), contains("Timed out"));
        // ...but the entry that merely arrived while the pool was unusable is left alone.
        verify(queueAPI, never()).markAsFailed(eq(rejected), any());
        assertFalse("the rejected entry must not have been mapped",
                degrading.mapped.contains("rejected-content"));
    }

    /**
     * And once the storage answers again, the pool heals on its own: no restart, and the entry
     * that was previously rejected maps normally.
     */
    @Test
    public void poolRecoversWhenAbandonedTaskFinishes() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ScriptedIndexAPI degrading = new ScriptedIndexAPI(1);
        degrading.bodies.put("hung-content", wedge(release));
        try {
            appendAll(degrading, entry("hung-content"), entry("rejected-content"));
            fail("an open circuit must abort the batch");
        } catch (final ReindexPoolExhaustedException expected) {
            assertTrue(expected.isCircuitOpen());
        }

        release.countDown(); // storage recovers, the abandoned task returns
        final long deadline = System.currentTimeMillis() + 10_000;
        while (degrading.mappingRunner().isDegraded()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertFalse("the pool must heal without a restart",
                degrading.mappingRunner().isDegraded());

        appendAll(degrading, entry("recovered-content"));
        assertTrue("indexing must resume after recovery",
                degrading.mapped.contains("recovered-content"));
    }

    /**
     * With the guard enabled, mapping runs on a dedicated platform worker thread — not a virtual
     * thread, whose carrier a hung native stat would pin (issue #37038), and not the reindex
     * thread itself.
     */
    @Test
    public void guardedMappingRunsOnADedicatedPlatformThread() throws Exception {
        appendAll(entry("content-a"));
        final Thread worker = api.lastMappingThread.get();
        assertNotSame("mapping must not run on the caller thread",
                Thread.currentThread(), worker);
        assertFalse("mapping must not run on a virtual thread — file I/O pins its carrier",
                worker.isVirtual());
        assertTrue("worker must be named for the reindex mapping pool: " + worker.getName(),
                worker.getName().startsWith("dot-reindex-mapping-"));
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
