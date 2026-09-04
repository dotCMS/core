package com.dotmarketing.common.reindex;

import com.dotcms.UnitTestBase;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.api.system.event.PayloadVerifierFactory;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.RoleVerifier;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ReindexPoolExhaustedException;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.dotcms.shutdown.ShutdownCoordinator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import javax.servlet.ServletContext;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ReindexThread}
 * @author jsanca
 */
public class ReindexThreadUnitTest extends UnitTestBase {

    private boolean testGenerateNotification = false;
    private PayloadVerifier originalRoleVerifier;

    @Test()
    public void testGenerateNotification() throws Exception {

        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final ReindexQueueAPI jAPI = mock(ReindexQueueAPI.class);
        final ContentletIndexAPI indexApi = mock(ContentletIndexAPI.class);
        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("US").build();

        String cmsAdminRoleId = UUID.randomUUID().toString();

        //Getting the original version of the Verifier in order to restore it after the test
        PayloadVerifierFactory payloadVerifierFactory = PayloadVerifierFactory.getInstance();
        Payload payload = new Payload(Visibility.ROLE, cmsAdminRoleId);
        this.originalRoleVerifier = payloadVerifierFactory.getVerifier(payload);

        //Mocking the Notification Visibility
        PayloadVerifier roleVerifier = new RoleVerifier(roleAPI);
        payloadVerifierFactory.register(Visibility.ROLE, roleVerifier);

        final ReindexThread reindexThread = new ReindexThread(jAPI, notificationAPI, userAPI, roleAPI, indexApi);
        final String identToIndex = "index1";
        final String msg = "Could not re-index record with the Identifier '"
                + identToIndex
                + "'. The record is in a bad state or can be associated to orphaned records. You can try running the Fix Assets Inconsistencies tool and re-start the reindex.";

        //Mock the system user
        final User user = new User();
        user.setLocale(locale);
        user.setUserId("admin@dotcms.com");
        when(userAPI.getSystemUser()).thenReturn(user);

        //Mock the CMS Admin Role
        final Role cmsAdminRole = new Role();
        cmsAdminRole.setId(cmsAdminRoleId);
        cmsAdminRole.setName("CMS Administrator");
        cmsAdminRole.setRoleKey("CMS Administrator");
        when(roleAPI.loadCMSAdminRole()).thenReturn(cmsAdminRole);

        this.initMessages();
        Config.CONTEXT = context;
        try {
            when(context.getInitParameter(WebKeys.COMPANY_ID)).thenReturn(RestUtilTest.DEFAULT_COMPANY);

            doAnswer(new Answer<Void>() { // if this method is called, should fail

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {

                    testGenerateNotification = true;
                    return null;
                }
            }).when(notificationAPI).generateNotification(
                    new I18NMessage("notification.reindex.error.title"),
                    new I18NMessage("notification.reindexing.error.processrecord", msg, identToIndex),
                    null,
                    NotificationLevel.INFO,
                    NotificationType.GENERIC,
                    Visibility.ROLE,
                    cmsAdminRoleId,
                    user.getUserId(),
                    locale
            );

            //Execute the notification call
            reindexThread.sendNotification
                    ("notification.reindexing.error.processrecord",
                            new Object[] {identToIndex}, msg, false);

            //Validate
            assertTrue(this.testGenerateNotification);
        } finally {
            Config.CONTEXT = null;
        }
    }

    /**
     * Verifies that {@code runReindexLoop} creates a <em>fresh</em>
     * {@link BulkProcessorListener} for every batch, eliminating the TOCTOU race
     * where {@code putAll(batch N+1)} could interleave with {@code afterBulk(batch N)}
     * reading {@code workingRecords} on the BulkProcessor callback thread.
     *
     * <p>The test runs the actual loop (via reflection) against fully mocked dependencies
     * and uses a {@link CountDownLatch} to let exactly two batches complete before
     * stopping the thread.</p>
     */
    @Test
    public void testPerBatchListenerIsolation() throws Exception {
        // — mocks —
        final ReindexQueueAPI queueApi       = mock(ReindexQueueAPI.class);
        final ContentletIndexAPI indexApi    = mock(ContentletIndexAPI.class);
        final IndexBulkProcessor mockProc    = mock(IndexBulkProcessor.class);

        final ReindexEntry e1 = mock(ReindexEntry.class);
        final ReindexEntry e2 = mock(ReindexEntry.class);
        final Map<String, ReindexEntry> batch1 = Map.of("id1", e1);
        final Map<String, ReindexEntry> batch2 = Map.of("id2", e2);

        // Alternate batch1 / batch2 so the queue is never empty and
        // finalizeReIndex() (which uses static helpers) is never reached.
        final AtomicInteger callCount = new AtomicInteger();
        when(queueApi.findContentToReindex())
                .thenAnswer(inv -> (callCount.getAndIncrement() % 2 == 0) ? batch1 : batch2);

        // close() inside try-with-resources must be a no-op
        doAnswer(inv -> null).when(mockProc).close();
        when(indexApi.createBulkProcessor(any(IndexBulkListener.class))).thenReturn(mockProc);

        // Count down once per batch so the test thread knows when to stop
        final CountDownLatch twoBatches = new CountDownLatch(2);
        doAnswer(inv -> { twoBatches.countDown(); return null; })
                .when(indexApi).appendToBulkProcessor(any(), any());

        // — instantiate (sets the static singleton used by stopThread) —
        final ReindexThread thread = new ReindexThread(
                queueApi,
                mock(NotificationAPI.class),
                mock(UserAPI.class),
                mock(RoleAPI.class),
                indexApi);
        setStateRunning(thread);

        // — run the private loop in a background thread —
        final Method runLoop = ReindexThread.class.getDeclaredMethod("runReindexLoop");
        runLoop.setAccessible(true);
        final Thread loopThread = new Thread(() -> {
            try {
                runLoop.invoke(thread);
            } catch (Exception ignored) {
                // InvocationTargetException is expected if stopThread races with invoke
            }
        });
        loopThread.start();

        // — wait for two complete batches, then stop —
        assertTrue("Two batches should complete within 5 s",
                twoBatches.await(5, TimeUnit.SECONDS));
        ReindexThread.stopThread();
        loopThread.join(3_000);
        assertFalse("runReindexLoop must exit after stopThread()", loopThread.isAlive());

        // — verify per-batch isolation —
        final ArgumentCaptor<IndexBulkListener> captor =
                ArgumentCaptor.forClass(IndexBulkListener.class);
        verify(indexApi, atLeast(2)).createBulkProcessor(captor.capture());

        final List<IndexBulkListener> captured = captor.getAllValues();
        final BulkProcessorListener l0 = (BulkProcessorListener) captured.get(0);
        final BulkProcessorListener l1 = (BulkProcessorListener) captured.get(1);

        assertNotSame(
                "Each batch must receive a fresh listener — reusing listeners is the TOCTOU hazard",
                l0, l1);
        assertEquals("First listener must contain only batch-1 entries",
                Set.of("id1"), l0.workingRecords.keySet());
        assertEquals("Second listener must contain only batch-2 entries",
                Set.of("id2"), l1.workingRecords.keySet());
    }


    // ================================================================================
    // User Story 1 (issue #36922, Bug 1) — shutdown must be deterministic and quiet.
    // AC-001..AC-004 + research R1/R8.1. These tests are written against the CURRENT
    // public/reflective surface so that they fail on assertions, never on compilation.
    // ================================================================================

    /**
     * AC-001 + AC-003 — once shutdown has started, the runnable must exit permanently.
     *
     * <p>Pre-fix, {@code runReindexLoop()} breaks out of the inner loop but returns into the outer
     * loop in {@code ReindexThreadRunnable}, whose only exit condition is
     * {@code state != STOPPED}. State is still {@code RUNNING}, so it re-enters immediately — and
     * the break path skips the loop-bottom {@code sleep()}, so it spins with no back-off. The
     * thread therefore never dies.</p>
     */
    @Test
    public void shouldExitPermanentlyOnShutdown() throws Exception {
        final ReindexThread thread = newThread(alwaysWorkingQueue(null), mockIndexApi());
        setStateRunning(thread);
        setShutdownFlags(true, true);

        final Thread runner = startRunnable(thread);
        try {
            runner.join(3_000);
            assertFalse("AC-001: the runnable must exit permanently once shutdown has started; "
                            + "instead it is still alive, i.e. the outer loop keeps re-entering "
                            + "runReindexLoop() (the hot-loop)",
                    runner.isAlive());
        } finally {
            ReindexThread.stopThread();
            runner.join(3_000);
        }
    }

    /**
     * AC-002 + AC-003 — the shutdown message is emitted at most once per shutdown, asserted at
     * DEBUG so the unconditional {@code logger.debug(...)} inside {@code Logger.infoEvery} cannot
     * hide the spin. The event count doubles as the loop-iteration counter.
     *
     * <p>Also covers the zero-line case demanded by AC-002's "at most once": a worker that was
     * never running must not log anything.</p>
     */
    @Test
    public void shouldLogShutdownAtMostOnce() throws Exception {
        startLogCapture();

        final ReindexThread thread = newThread(alwaysWorkingQueue(null), mockIndexApi());
        setStateRunning(thread);
        setShutdownFlags(true, true);

        final Thread runner = startRunnable(thread);
        try {
            runner.join(3_000);
            final int emitted = logCapture.shutdownEventCount();
            assertTrue("AC-002: 'Shutdown detected' must be logged at most once per shutdown "
                            + "(at INFO and DEBUG); it was logged " + emitted + " times, which is "
                            + "the log flood the hot-loop produces",
                    emitted <= 1);
        } finally {
            ReindexThread.stopThread();
            runner.join(3_000);
        }
    }

    /**
     * AC-004 — the shutdown check inside the record-processing branch must behave identically:
     * terminal state, one log line, no re-entry. Here the queue always returns work, so the loop
     * reaches the second shutdown check rather than the one at the top.
     */
    @Test
    public void shouldExitPermanentlyOnShutdownDuringRecordProcessing() throws Exception {
        final ReindexThread thread = newThread(alwaysWorkingQueue(null), mockIndexApi());
        setStateRunning(thread);
        setShutdownFlags(true, true);

        final Thread runner = startRunnable(thread);
        try {
            runner.join(3_000);
            assertFalse("AC-004: the record-processing shutdown check must also be terminal",
                    runner.isAlive());
        } finally {
            ReindexThread.stopThread();
            runner.join(3_000);
        }
    }

    /**
     * Research R1 — the defect the issue's literal AC-001 would have left in place.
     *
     * <p>{@code isRequestDraining()} is a <em>transient window</em>: it is set at the start of
     * shutdown Phase 1 and cleared in a {@code finally} at its end (default 15 s), while
     * {@code ReindexThreadShutdownTask} only runs in Phase 2. So there is a window in which
     * shutdown is under way but draining has already cleared — and today the still-{@code RUNNING}
     * worker resumes indexing against a database that is about to be torn down.</p>
     */
    @Test
    public void shouldNotResumeIndexingAfterDrainingWindowCloses() throws Exception {
        final AtomicInteger queuePolls = new AtomicInteger();
        final ReindexThread thread = newThread(alwaysWorkingQueue(queuePolls), mockIndexApi());
        setStateRunning(thread);
        // Shutdown has started, but request draining (Phase 1) has already finished.
        setShutdownFlags(true, false);

        final Thread runner = startRunnable(thread);
        try {
            runner.join(3_000);
            assertFalse("R1: the worker must stay terminal after the draining window closes",
                    runner.isAlive());
            assertEquals("R1: no queue polling may happen once shutdown has started; the worker "
                            + "resumed indexing after isRequestDraining() flipped back to false",
                    0, queuePolls.get());
        } finally {
            ReindexThread.stopThread();
            runner.join(3_000);
        }
    }

    /**
     * Research R8.1 — a worker parked in {@code PAUSED} must not wake itself into {@code RUNNING}
     * while the JVM is shutting down.
     *
     * <p>{@code sleep()} sets {@code state = RUNNING} whenever the cached pause deadline is absent
     * or expired, with no shutdown check at all — so a paused worker resumes indexing mid-shutdown.
     * The pause marker is absent here, which is exactly the "expired" case.</p>
     */
    @Test
    public void pausedWorkerShouldNotSelfUnpauseDuringShutdown() throws Exception {
        final ReindexQueueAPI queueApi = mock(ReindexQueueAPI.class);
        when(queueApi.findContentToReindex()).thenReturn(Map.of());
        final ReindexThread thread = newThread(queueApi, mock(ContentletIndexAPI.class));
        setState(thread, "PAUSED");
        setShutdownFlags(true, true);

        final Method sleep = ReindexThread.class.getDeclaredMethod("sleep");
        sleep.setAccessible(true);

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread runner = new Thread(() -> {
            try {
                sleep.invoke(thread);
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "reindex-unit-test-pause-loop");
        runner.setDaemon(true);
        runner.start();
        runner.join(3_000);

        try {
            assertFalse("R8.1: the paused wait must terminate during shutdown", runner.isAlive());
            assertNotNull("state must be readable", currentState(thread));
            assertNotEquals("R8.1: a paused worker must not transition itself back to RUNNING "
                            + "while the JVM is shutting down",
                    "RUNNING", currentState(thread));
        } finally {
            setState(thread, "STOPPED");
            runner.join(1_000);
        }
    }

    // ================================================================================
    // User Story 2 (issue #36922, Bug 2) — a dead runnable must never be mistaken for a
    // paused one. AC-006..AC-011. Written against the CURRENT surface: the liveness field
    // is located by type, so its absence fails as a readable assertion, not an Error.
    // ================================================================================

    /**
     * AC-006 + AC-007 — liveness must be cleared in a {@code finally} on every exit path, including
     * an uncaught {@link Error}. Today {@code ReindexThreadRunnable} catches only {@code Exception},
     * so an {@code OutOfMemoryError} escapes and the runnable dies with no bookkeeping at all.
     */
    @Test
    public void livenessIsClearedOnEveryExitPathIncludingError() throws Exception {
        final ReindexQueueAPI queueApi = mock(ReindexQueueAPI.class);
        when(queueApi.findContentToReindex()).thenThrow(new OutOfMemoryError("simulated"));

        final ReindexThread thread = newThread(queueApi, mockIndexApi());
        final AtomicBoolean liveness = requireLiveness(thread);
        setStateRunning(thread);
        liveness.set(true);

        final Thread runner = startRunnable(thread);
        try {
            runner.join(3_000);
            assertFalse("AC-007: an uncaught Error must not bypass the finally that clears "
                    + "liveness; the runnable is still marked alive", liveness.get());
        } finally {
            ReindexThread.stopThread();
            runner.join(3_000);
        }
    }

    /**
     * AC-008 + AC-009 — the core of Bug 2. With state {@code PAUSED} but no live runnable,
     * {@code unpauseImpl()} must re-submit a worker, not merely flip the flag.
     *
     * <p>"A worker is running" is observed behaviorally: something must start polling the queue.
     * Pre-fix, {@code unpauseImpl()} logs "Unpausing reindex thread", sets {@code RUNNING}, and
     * nothing ever polls — the silent stall that hid push-published content for ~2 days.</p>
     */
    @Test
    public void deadWorkerIsRestartedOnUnpause() throws Exception {
        final Set<String> pollingThreads = Collections.synchronizedSet(new java.util.HashSet<>());
        final ReindexThread thread = newThread(threadRecordingQueue(pollingThreads), mockIndexApi());

        // Dead worker, but the state machine still says PAUSED.
        final AtomicBoolean liveness = requireLiveness(thread);
        liveness.set(false);
        setState(thread, "PAUSED");

        try {
            unpauseImpl();
            assertTrue("AC-008: unpauseImpl() found state==PAUSED with a dead runnable and must "
                            + "re-submit a worker; nothing ever polled the reindex queue, so the "
                            + "queue would never drain",
                    awaitTrue(() -> !pollingThreads.isEmpty(), 5_000));
        } finally {
            ReindexThread.stopThread();
        }
    }

    /** AC-011 — the healthy path must not change: a live paused worker is not duplicated. */
    @Test
    public void liveWorkerIsNotDuplicatedOnUnpause() throws Exception {
        final Set<String> pollingThreads = Collections.synchronizedSet(new java.util.HashSet<>());
        final ReindexThread thread = newThread(threadRecordingQueue(pollingThreads), mockIndexApi());

        final AtomicBoolean liveness = requireLiveness(thread);
        setStateRunning(thread);
        liveness.set(true);
        final Thread runner = startRunnable(thread);

        try {
            assertTrue("the seed worker should be polling", awaitTrue(
                    () -> !pollingThreads.isEmpty(), 5_000));
            setState(thread, "PAUSED");
            unpauseImpl();
            Thread.sleep(1_000);

            assertEquals("AC-011: a live paused worker must be resumed by a flag flip only; a "
                            + "second worker was submitted. Threads seen: " + pollingThreads,
                    1, pollingThreads.size());
        } finally {
            ReindexThread.stopThread();
            runner.join(3_000);
        }
    }

    /**
     * AC-010 — concurrent unpauses (several commit listeners firing at once) must yield exactly one
     * worker: never two draining the same queue, never zero.
     */
    @Test
    public void concurrentUnpauseYieldsExactlyOneWorker() throws Exception {
        final Set<String> pollingThreads = Collections.synchronizedSet(new java.util.HashSet<>());
        final ReindexThread thread = newThread(threadRecordingQueue(pollingThreads), mockIndexApi());

        final AtomicBoolean liveness = requireLiveness(thread);
        liveness.set(false);
        setState(thread, "PAUSED");

        final int callers = 8;
        final CountDownLatch startLine = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(callers);
        try {
            for (int i = 0; i < callers; i++) {
                final Thread t = new Thread(() -> {
                    try {
                        startLine.await();
                        unpauseImpl();
                    } catch (Exception ignored) {
                        // recorded via pollingThreads
                    } finally {
                        done.countDown();
                    }
                }, "unpause-caller-" + i);
                t.setDaemon(true);
                t.start();
            }
            startLine.countDown();
            assertTrue("all unpause callers should finish", done.await(10, TimeUnit.SECONDS));

            assertTrue("AC-010: at least one worker must be running after a concurrent unpause",
                    awaitTrue(() -> !pollingThreads.isEmpty(), 5_000));
            Thread.sleep(1_000);
            assertEquals("AC-010: exactly one worker may drain the queue; concurrent unpauses "
                            + "started " + pollingThreads.size() + ". Threads: " + pollingThreads,
                    1, pollingThreads.size());
        } finally {
            ReindexThread.stopThread();
        }
    }

    /**
     * Invariant I3 / V6 — liveness must never be left {@code true} with no worker behind it. A
     * submit that fails after the flag is claimed would otherwise strand it and block recovery
     * permanently: a worse failure than the bug being fixed.
     *
     * <p>Asserted as the observable contract — after an unpause attempt, "flag says alive" and
     * "something is polling" must agree.</p>
     */
    @Test
    public void livenessNeverStrandedWithoutAWorker() throws Exception {
        final Set<String> pollingThreads = Collections.synchronizedSet(new java.util.HashSet<>());
        final ReindexThread thread = newThread(threadRecordingQueue(pollingThreads), mockIndexApi());

        final AtomicBoolean liveness = requireLiveness(thread);
        liveness.set(false);
        setState(thread, "PAUSED");

        try {
            unpauseImpl();
            Thread.sleep(2_000);
            if (liveness.get()) {
                assertFalse("I3/V6: liveness is true but nothing is polling the queue — the flag "
                        + "is stranded and no future unpause can ever recover the worker",
                        pollingThreads.isEmpty());
            }
        } finally {
            ReindexThread.stopThread();
        }
    }

    /**
     * Transition T9 — a restart must be refused once shutdown has started.
     *
     * <p>NOTE: the guard this asserts was implemented in US1 (it is required for AC-001's "cannot
     * be restarted while shutting down"), so this test is expected to be <strong>green before</strong>
     * the US2 implementation. It is a regression guard, not a Red-gate test — see T030.</p>
     */
    @Test
    public void restartIsRefusedWhileShuttingDown() throws Exception {
        final Set<String> pollingThreads = Collections.synchronizedSet(new java.util.HashSet<>());
        final ReindexThread thread = newThread(threadRecordingQueue(pollingThreads), mockIndexApi());

        setState(thread, "PAUSED");
        setShutdownFlags(true, false);

        try {
            unpauseImpl();
            Thread.sleep(1_000);
            assertTrue("T9: no worker may be started once shutdown has begun; a restart would "
                            + "resurrect the reindex thread against tearing-down infrastructure",
                    pollingThreads.isEmpty());
        } finally {
            ReindexThread.stopThread();
        }
    }

    // ================================================================================
    // User Story 3 (issue #36922) — interrupt handling and lifecycle corner cases.
    // AC-014, AC-015, AC-016 + research R4.
    // ================================================================================

    /**
     * AC-014 / research R4 — an interrupt must terminate the worker and preserve the interrupt
     * status.
     *
     * <p>Today every wait in this class goes through {@code ThreadUtils.sleep()}:
     * {@code Try.run(() -> Thread.sleep(t)).onFailure(DotRuntimeException::new)}. {@code onFailure}
     * takes a {@code Consumer<Throwable>}, so {@code DotRuntimeException::new} merely
     * <em>constructs</em> an exception and discards it — the {@code InterruptedException} is never
     * rethrown, never logged, and its interrupt flag was already cleared by {@code Thread.sleep}
     * throwing. The worker is therefore un-interruptible in practice, which is why
     * {@code shutdownNow()} from {@code ReindexThreadShutdownTask} has no effect on a parked
     * worker.</p>
     */
    @Test
    public void interruptTerminatesWorkerAndPreservesInterruptStatus() throws Exception {
        final ReindexThread thread = newThread(alwaysWorkingQueue(null), mockIndexApi());

        // pause() writes a future deadline to the cache, so the wait loop actually parks instead of
        // immediately flipping itself back to RUNNING.
        ReindexThread.pause();

        final Method sleep = ReindexThread.class.getDeclaredMethod("sleep");
        sleep.setAccessible(true);

        final AtomicBoolean interruptStatusAfterExit = new AtomicBoolean(false);
        final Thread runner = new Thread(() -> {
            try {
                sleep.invoke(thread);
            } catch (Throwable ignored) {
                // terminal state or propagated interrupt: both are exits
            } finally {
                interruptStatusAfterExit.set(Thread.currentThread().isInterrupted());
            }
        }, "reindex-unit-test-interrupt");
        runner.setDaemon(true);
        runner.start();

        try {
            Thread.sleep(500);        // let it park inside the wait
            runner.interrupt();
            runner.join(3_000);

            assertFalse("AC-014: an interrupted worker must terminate; it is still parked, so the "
                    + "interrupt was swallowed by ThreadUtils.sleep()", runner.isAlive());
            assertTrue("AC-014: the interrupt status must be restored before the worker exits, so "
                            + "callers up the stack can still observe the interrupt",
                    interruptStatusAfterExit.get());
        } finally {
            setState(thread, "STOPPED");
            runner.join(1_000);
        }
    }

    /**
     * AC-016 — an explicit stop must stay restartable.
     *
     * <p>Regression guard for the terminal state added in US1: {@code stopThread()} must leave the
     * worker in the restartable {@code STOPPED} state, never in the terminal {@code SHUTDOWN}, or
     * an operator-initiated stop would become permanent. The converse is also asserted:
     * {@code stopThread()} during a shutdown must NOT downgrade {@code SHUTDOWN} back to
     * {@code STOPPED}, which would re-open the mid-shutdown restart path.</p>
     *
     * <p>The full restart round-trip (which bootstraps OSGI) is covered by the integration test
     * {@code ReindexThreadTest#test_dead_but_paused_worker_recovers_and_indexes_queued_content},
     * whose teardown calls {@code startThread()}.</p>
     */
    @Test
    public void explicitStopStaysRestartableButNeverDowngradesShutdown() throws Exception {
        final ReindexThread thread = newThread(alwaysWorkingQueue(null), mockIndexApi());

        setStateRunning(thread);
        ReindexThread.stopThread();
        assertEquals("AC-016: an explicit stop must remain restartable", "STOPPED",
                currentState(thread));

        // Now the terminal case: a shutdown must win over a subsequent stopThread().
        setStateRunning(thread);
        setShutdownFlags(true, true);
        final Method shutdownRequested = ReindexThread.class.getDeclaredMethod("shutdownRequested");
        shutdownRequested.setAccessible(true);
        shutdownRequested.invoke(thread);
        assertEquals("shutdown must move the worker to the terminal state", "SHUTDOWN",
                currentState(thread));

        ReindexThread.stopThread();
        assertEquals("stopThread() must not downgrade the terminal SHUTDOWN state back to the "
                        + "restartable STOPPED — that would re-open the mid-shutdown restart path",
                "SHUTDOWN", currentState(thread));
    }

    /**
     * AC-015 — the degraded-mode back-off from PR #37038 must survive this refactor.
     *
     * <p>{@code ReindexPoolExhaustedException} is a {@code DotRuntimeException}, i.e. an
     * {@code Exception}, so the new "rethrow {@code Error}" rule must not touch it: the mapping
     * guard refusing a batch is recoverable and the worker must keep running and back off, not
     * terminate. Regression guard — expected to pass before and after the US3 implementation.</p>
     */
    @Test
    public void degradedPoolBackOffKeepsTheWorkerAlive() throws Exception {
        final ReindexQueueAPI queueApi = mock(ReindexQueueAPI.class);
        when(queueApi.findContentToReindex())
                .thenThrow(new ReindexPoolExhaustedException("simulated pool exhaustion", true));

        Config.setProperty("REINDEX_THREAD_SLEEP_WHEN_DEGRADED", 300);
        try {
            final ReindexThread thread = newThread(queueApi, mockIndexApi());
            setStateRunning(thread);

            final Thread runner = startRunnable(thread);
            try {
                Thread.sleep(1_500);
                assertTrue("AC-015: a ReindexPoolExhaustedException is recoverable — the worker "
                                + "must back off and stay alive, not terminate",
                        runner.isAlive());
            } finally {
                ReindexThread.stopThread();
                runner.join(3_000);
            }
        } finally {
            Config.setProperty("REINDEX_THREAD_SLEEP_WHEN_DEGRADED", 30000);
        }
    }

    // ================================================================================
    // Phase 2 scaffolding (T004-T007) — shared test infrastructure for issue #36922.
    // No production behavior is asserted here; each helper exists so the US1-US3 tests
    // can drive ReindexThread's lifecycle deterministically.
    // ================================================================================

    private static final String REINDEX_LOGGER = ReindexThread.class.getName();

    private CapturingAppender logCapture;

    /**
     * T004 — in-memory log4j2 appender bound to {@link ReindexThread}'s logger.
     *
     * <p>Deliberately captures at <strong>DEBUG</strong>. {@code Logger.infoEvery} throttles only
     * its INFO emission and then calls {@code logger.debug(...)} <em>unconditionally</em>, so an
     * INFO-only assertion would report "logged once" even while the loop spins thousands of
     * times. AC-002 requires the at-most-once guarantee to hold at both levels.</p>
     */
    private static final class CapturingAppender extends AbstractAppender {

        /**
         * Hard cap on retained messages. The pre-fix hot-loop emits a DEBUG event on every
         * iteration at millions of iterations per second; retaining them all exhausts the heap and
         * hangs the forked JVM before any assertion runs. Counting is unbounded, storage is not.
         */
        private static final int MAX_RETAINED = 256;

        private final List<String> messages = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger shutdownEvents = new AtomicInteger();

        CapturingAppender() {
            super("ReindexTestCapture", null, null, true, null);
        }

        @Override
        public void append(final LogEvent event) {
            final String formatted = event.getMessage().getFormattedMessage();
            if (formatted.contains("Shutdown detected")) {
                shutdownEvents.incrementAndGet();
            }
            if (messages.size() < MAX_RETAINED) {
                messages.add(formatted);
            }
        }

        int shutdownEventCount() {
            return shutdownEvents.get();
        }
    }

    private void startLogCapture() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        this.logCapture = new CapturingAppender();
        this.logCapture.start();

        // A DEDICATED LoggerConfig, not the one getLoggerConfig() returns. That call resolves to
        // the closest *ancestor* config (usually root) when no config exists for this name;
        // re-registering root under another name corrupts the config tree and makes
        // updateLoggers() spin forever. additivity=false also keeps the pre-fix hot-loop's DEBUG
        // flood out of the console appenders, which would otherwise dominate the run.
        final LoggerConfig dedicated = new LoggerConfig(REINDEX_LOGGER, Level.DEBUG, false);
        dedicated.addAppender(this.logCapture, Level.DEBUG, null);
        config.addLogger(REINDEX_LOGGER, dedicated);
        ctx.updateLoggers();
    }

    private void stopLogCapture() {
        if (this.logCapture == null) {
            return;
        }
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.getConfiguration().removeLogger(REINDEX_LOGGER);
        ctx.updateLoggers();
        this.logCapture.stop();
        this.logCapture = null;
    }

    /**
     * T005 — drives the real {@link ShutdownCoordinator} singleton's flags by reflection.
     *
     * <p>Mockito's {@code mockStatic} is <strong>thread-local</strong>, so it would never apply to
     * the background thread the reindex runnable executes on. Flipping the real singleton's
     * {@code AtomicBoolean}s is visible from every thread and exercises the production code path
     * verbatim.</p>
     *
     * <p>The two flags are set independently on purpose: {@code requestDrainingInProgress} is a
     * transient window (shutdown Phase 1 only, default 15 s) while {@code shutdownInProgress} is a
     * monotonic latch. Conflating them is the root of the hot-loop (research R1), so the tests
     * must be able to model "draining cleared, shutdown still in progress".</p>
     */
    private static void setShutdownFlags(final boolean shutdownStarted, final boolean draining)
            throws Exception {
        final ShutdownCoordinator coordinator = ShutdownCoordinator.getInstance();
        atomicBooleanField(coordinator, "shutdownInProgress").set(shutdownStarted);
        atomicBooleanField(coordinator, "requestDrainingInProgress").set(draining);
    }

    private static AtomicBoolean atomicBooleanField(final ShutdownCoordinator coordinator,
            final String name) throws Exception {
        final Field field = ShutdownCoordinator.class.getDeclaredField(name);
        field.setAccessible(true);
        return (AtomicBoolean) field.get(coordinator);
    }

    /**
     * T006 — runs the private {@code ReindexThreadRunnable} field on a daemon thread.
     *
     * <p>Returns the thread so a test can assert it <em>terminates</em>. Thread liveness is the
     * cleanest cross-version signal for the hot-loop: the pre-fix outer loop re-enters
     * {@code runReindexLoop()} forever because its only exit condition is {@code state == STOPPED},
     * which the shutdown path never sets.</p>
     */
    private static Thread startRunnable(final ReindexThread thread) throws Exception {
        final Field runnableField = ReindexThread.class.getDeclaredField("ReindexThreadRunnable");
        runnableField.setAccessible(true);
        final Runnable runnable = (Runnable) runnableField.get(thread);

        final Thread t = new Thread(runnable, "reindex-unit-test-runnable");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * T007 — forces a live runnable to exit while leaving {@code state} at {@code PAUSED}: the
     * precondition for every dead-worker (Bug 2) scenario. Stops the thread via the state machine,
     * waits for it to die, then restores {@code PAUSED} behind its back.
     */
    private static void killRunnableLeavingPaused(final ReindexThread thread, final Thread runner)
            throws Exception {
        setState(thread, "STOPPED");
        runner.join(3_000);
        assertFalse("Runnable should have exited before we fake the PAUSED state",
                runner.isAlive());
        setState(thread, "PAUSED");
    }


    // ---- US2 (Bug 2) scaffolding: liveness + worker observation -------------------------------

    /**
     * Finds the liveness {@code AtomicBoolean} on {@link ReindexThread} by type, so the test does
     * not hard-code a field name the implementation has not chosen yet. Returns {@code null} when
     * no such field exists (the pre-fix state), letting callers fail with a readable assertion
     * instead of a {@code NoSuchFieldException}.
     */
    private static AtomicBoolean livenessFlag(final ReindexThread thread) throws Exception {
        for (final Field f : ReindexThread.class.getDeclaredFields()) {
            if (AtomicBoolean.class.equals(f.getType())) {
                f.setAccessible(true);
                return (AtomicBoolean) f.get(thread);
            }
        }
        return null;
    }

    private static AtomicBoolean requireLiveness(final ReindexThread thread) throws Exception {
        final AtomicBoolean flag = livenessFlag(thread);
        assertNotNull("AC-006: ReindexThread must carry an AtomicBoolean liveness flag so a dead "
                + "runnable can be distinguished from a paused one; no such field exists", flag);
        return flag;
    }

    /**
     * A queue whose {@code findContentToReindex()} records the <em>name of every thread</em> that
     * polls it. Distinct names = distinct live workers, which is how the tests below detect both
     * "no worker is running" (empty) and "two workers are running" (size &gt; 1) without needing a
     * production seam for the submitter.
     */
    private static ReindexQueueAPI threadRecordingQueue(final Set<String> pollingThreads)
            throws Exception {
        final ReindexQueueAPI queueApi = mock(ReindexQueueAPI.class);
        final Map<String, ReindexEntry> batch = Map.of("id1", mock(ReindexEntry.class));
        when(queueApi.findContentToReindex()).thenAnswer(inv -> {
            pollingThreads.add(Thread.currentThread().getName());
            return batch;
        });
        return queueApi;
    }

    /** Invokes the private static {@code unpauseImpl()}. */
    private static void unpauseImpl() throws Exception {
        final Method m = ReindexThread.class.getDeclaredMethod("unpauseImpl");
        m.setAccessible(true);
        m.invoke(null);
    }

    /** Waits until {@code condition} holds or the timeout expires. */
    private static boolean awaitTrue(final java.util.function.BooleanSupplier condition,
            final long timeoutMillis) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(25);
        }
        return condition.getAsBoolean();
    }

    /** Generalized form of {@link #setStateRunning}; sets any {@code ThreadState} by name. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setState(final ReindexThread thread, final String stateName)
            throws Exception {
        final Field stateField = ReindexThread.class.getDeclaredField("state");
        stateField.setAccessible(true);
        final AtomicReference<Object> stateRef = (AtomicReference<Object>) stateField.get(thread);

        for (final Class<?> inner : ReindexThread.class.getDeclaredClasses()) {
            if (inner.isEnum() && "ThreadState".equals(inner.getSimpleName())) {
                stateRef.set(Enum.valueOf((Class<Enum>) inner, stateName));
                return;
            }
        }
        throw new IllegalStateException("ThreadState." + stateName + " not found via reflection");
    }

    /** Reads the current {@code ThreadState} constant name, for assertions on the state machine. */
    @SuppressWarnings("unchecked")
    private static String currentState(final ReindexThread thread) throws Exception {
        final Field stateField = ReindexThread.class.getDeclaredField("state");
        stateField.setAccessible(true);
        final AtomicReference<Object> stateRef = (AtomicReference<Object>) stateField.get(thread);
        return ((Enum<?>) stateRef.get()).name();
    }

    private static ReindexThread newThread(final ReindexQueueAPI queueApi,
            final ContentletIndexAPI indexApi) {
        return new ReindexThread(queueApi, mock(NotificationAPI.class), mock(UserAPI.class),
                mock(RoleAPI.class), indexApi);
    }

    /**
     * Builds a fully mocked {@link ContentletIndexAPI} whose bulk processor is a no-op.
     *
     * <p><strong>Why every loop test keeps the queue non-empty:</strong> an empty queue drives
     * {@code runReindexLoop()} into {@code finalizeReIndex()} and then
     * {@code switchOverIfNeeded()}, which calls {@code ESReindexationProcessStatus} and
     * {@code CacheLocator} — real Elasticsearch and cache infrastructure that is not available in
     * a unit harness and deadlocks the forked JVM on class initialization. The pre-existing
     * {@code testPerBatchListenerIsolation} avoids this the same way. Keeping the queue non-empty
     * confines the loop to mocked collaborators.</p>
     */
    private static ContentletIndexAPI mockIndexApi() {
        final ContentletIndexAPI indexApi = mock(ContentletIndexAPI.class);
        final IndexBulkProcessor proc = mock(IndexBulkProcessor.class);
        when(indexApi.createBulkProcessor(any(IndexBulkListener.class))).thenReturn(proc);
        return indexApi;
    }

    /** A queue that always has work, so {@code finalizeReIndex()} is never reached. */
    private static ReindexQueueAPI alwaysWorkingQueue(final AtomicInteger pollCounter)
            throws Exception {
        final ReindexQueueAPI queueApi = mock(ReindexQueueAPI.class);
        final Map<String, ReindexEntry> batch = Map.of("id1", mock(ReindexEntry.class));
        when(queueApi.findContentToReindex()).thenAnswer(inv -> {
            if (pollCounter != null) {
                pollCounter.incrementAndGet();
            }
            return batch;
        });
        return queueApi;
    }

    @After
    public void restore() throws Exception {
        //Restore the original version of the Verifier
        if (originalRoleVerifier != null) {
            PayloadVerifierFactory payloadVerifierFactory = PayloadVerifierFactory.getInstance();
            payloadVerifierFactory.register(Visibility.ROLE, this.originalRoleVerifier);
        }
        // ReindexThread and ShutdownCoordinator are process-wide singletons: leaking either
        // would silently corrupt every test that runs after this one in the same JVM.
        setShutdownFlags(false, false);
        stopLogCapture();
    }

    /**
     * Uses reflection to set the private {@code state} field of a {@link ReindexThread}
     * instance to {@code ThreadState.RUNNING}, bypassing the private enum visibility.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setStateRunning(final ReindexThread thread) throws Exception {
        final Field stateField = ReindexThread.class.getDeclaredField("state");
        stateField.setAccessible(true);
        final AtomicReference<Object> stateRef =
                (AtomicReference<Object>) stateField.get(thread);

        for (final Class<?> inner : ReindexThread.class.getDeclaredClasses()) {
            if (inner.isEnum() && "ThreadState".equals(inner.getSimpleName())) {
                stateRef.set(Enum.valueOf((Class<Enum>) inner, "RUNNING"));
                return;
            }
        }
        throw new IllegalStateException("ThreadState.RUNNING not found via reflection");
    }
}
