package com.dotcms.content.index;

import static org.junit.Assert.*;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.index.SearchUnavailableException.Reason;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;

/** Exercises real concurrent callers with controllable async I/O, without a live search cluster. */
public class SearchAdmissionTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ExecutorService callers = Executors.newFixedThreadPool(12);

    /** Always stops test workers and removes request state, including when an assertion fails. */
    @After
    public void cleanup() throws Exception {
        callers.shutdownNow();
        assertTrue(callers.awaitTermination(5, TimeUnit.SECONDS));
        registry.close();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /** Excess callers must be rejected before they start transport I/O, with no admission queue. */
    @Test(timeout = 10000)
    public void rejectsExcessConcurrentCallsAndRecovers() throws Exception {
        final SearchAdmission admission = new SearchAdmission(3, 5000, "test", registry);
        final CountDownLatch entered = new CountDownLatch(3);
        final List<CompletableFuture<Integer>> completions = new ArrayList<>();
        final List<Future<Integer>> active = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final CompletableFuture<Integer> transport = new CompletableFuture<>();
            completions.add(transport);
            active.add(callers.submit(() -> admission.execute(completion -> {
                transport.whenComplete((value, failure) -> completion.complete(value));
                entered.countDown();
                return () -> { };
            })));
        }
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertEquals(3, gauge("active"), 0);
        final AtomicInteger transportStarts = new AtomicInteger();
        final List<Future<Reason>> rejected = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            rejected.add(callers.submit(() -> assertThrows(SearchUnavailableException.class,
                    () -> admission.execute(completion -> {
                        transportStarts.incrementAndGet();
                        return () -> { };
                    })).reason()));
        }
        for (Future<Reason> result : rejected) {
            assertEquals(Reason.REJECTED, result.get(5, TimeUnit.SECONDS));
        }
        assertEquals(0, transportStarts.get());
        assertEquals(8, counter("rejected"), 0);
        completions.forEach(completion -> completion.complete(42));
        for (Future<Integer> result : active) {
            assertEquals(Integer.valueOf(42), result.get(5, TimeUnit.SECONDS));
        }
        assertEquals(0, gauge("active"), 0);
        assertEquals(Integer.valueOf(7), admission.execute(completion -> {
            completion.complete(7);
            return () -> { };
        }));
    }

    /** Reproduces 600 simultaneous callers: only eight start I/O, all others return promptly. */
    @Test(timeout = 20000)
    public void sixHundredCallersCannotBecomeSixHundredBlockedTransports() throws Exception {
        final SearchAdmission admission = new SearchAdmission(8, 10000, "test", registry);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch attempted = new CountDownLatch(600);
        final java.util.concurrent.ConcurrentLinkedQueue<CompletableFuture<Integer>> transports =
                new java.util.concurrent.ConcurrentLinkedQueue<>();
        final List<Future<Integer>> results = new ArrayList<>();
        try (ExecutorService burst = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                for (int i = 0; i < 600; i++) {
                    results.add(burst.submit(() -> {
                        start.await();
                        try {
                            return admission.<Integer>execute(completion -> {
                                transports.add(completion);
                                attempted.countDown();
                                return () -> { };
                            });
                        } catch (SearchUnavailableException failure) {
                            assertEquals(Reason.REJECTED, failure.reason());
                            attempted.countDown();
                            return 0;
                        }
                    }));
                }
                start.countDown();
                assertTrue("Rejected callers must not wait for the slow backend", attempted.await(5, TimeUnit.SECONDS));
                assertEquals(8, transports.size());
                assertEquals(8, gauge("active"), 0);
                assertEquals(592, counter("rejected"), 0);
            } finally {
                transports.forEach(completion -> completion.complete(1));
                burst.shutdown();
            }
            int successful = 0;
            for (Future<Integer> result : results) {
                successful += result.get(5, TimeUnit.SECONDS);
            }
            assertEquals(8, successful);
        }
        assertEquals(0, gauge("active"), 0);
    }

    /** A caller timeout cancels I/O, but a transport ignoring cancellation cannot grow orphan work. */
    @Test(timeout = 10000)
    public void deadlineRetainsPermitUntilActualTransportCompletion() throws Exception {
        final SearchAdmission admission = new SearchAdmission(1, 30, "test", registry);
        final AtomicReference<CompletableFuture<Integer>> transport = new AtomicReference<>();
        final AtomicInteger cancellations = new AtomicInteger();
        final AtomicInteger discarded = new AtomicInteger();
        final SearchUnavailableException failure = assertThrows(SearchUnavailableException.class,
                () -> admission.<Integer>execute(completion -> {
                    transport.set(completion);
                    return cancellations::incrementAndGet;
                }, discarded::addAndGet));
        assertEquals(Reason.DEADLINE, failure.reason());
        assertEquals(1, cancellations.get());
        assertEquals(1, gauge("active"), 0);
        assertEquals(1, counter("deadlines"), 0);
        assertEquals(Reason.REJECTED, assertThrows(SearchUnavailableException.class,
                () -> admission.execute(completion -> failStart())).reason());
        transport.get().complete(6);
        assertEquals(6, discarded.get());
        assertEquals(0, gauge("active"), 0);
        assertEquals(Integer.valueOf(8), admission.execute(completion -> {
            completion.complete(8);
            return () -> { };
        }));
    }

    /** Restores the interrupt flag and retains the permit when cancellation has not completed. */
    @Test(timeout = 10000)
    public void interruptionCancelsWithoutLeakingPermitsOrInterruptState() throws Exception {
        final SearchAdmission admission = new SearchAdmission(1, 5000, "test", registry);
        final CountDownLatch entered = new CountDownLatch(1);
        final AtomicReference<Thread> caller = new AtomicReference<>();
        final AtomicReference<CompletableFuture<Integer>> transport = new AtomicReference<>();
        final AtomicInteger cancellations = new AtomicInteger();
        final Future<Boolean> interrupted = callers.submit(() -> {
            caller.set(Thread.currentThread());
            try {
                admission.<Integer>execute(completion -> {
                    transport.set(completion);
                    entered.countDown();
                    return cancellations::incrementAndGet;
                });
                return false;
            } catch (SearchUnavailableException failure) {
                return failure.reason() == Reason.INTERRUPTED && Thread.currentThread().isInterrupted();
            } finally {
                Thread.interrupted();
            }
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        caller.get().interrupt();
        assertTrue(interrupted.get(5, TimeUnit.SECONDS));
        assertEquals(1, cancellations.get());
        assertEquals(1, counter("interrupted"), 0);
        assertEquals(1, gauge("active"), 0);
        transport.get().complete(1);
        assertEquals(0, gauge("active"), 0);
    }

    /** Immediate callbacks and duplicate callback attempts must release exactly one permit. */
    @Test
    public void synchronousCompletionAndDuplicateCallbacksReleaseOnce() throws Exception {
        final SearchAdmission admission = new SearchAdmission(1, 5000, "test", registry);
        for (int i = 0; i < 100; i++) {
            assertEquals(Integer.valueOf(1), admission.execute(completion -> {
                completion.complete(1);
                completion.complete(2);
                completion.completeExceptionally(new IOException());
                return () -> { };
            }));
        }
        assertEquals(0, gauge("active"), 0);
        assertEquals(100, registry.get("dotcms.search.admission.duration").timer().count());
    }

    /** Failure before an asynchronous request exists must release the acquired slot. */
    @Test
    public void startFailureReleasesPermit() {
        final SearchAdmission admission = new SearchAdmission(1, 5000, "test", registry);
        final IOException expected = new IOException("start failed");
        assertSame(expected, assertThrows(IOException.class,
                () -> admission.execute(completion -> { throw expected; })));
        assertEquals(0, gauge("active"), 0);
    }

    /** Async failures must preserve their original type, including wrapped OS transport failures. */
    @Test
    public void asyncFailureReleasesPermitAndPreservesCause() {
        final SearchAdmission admission = new SearchAdmission(1, 5000, "test", registry);
        final IOException expected = new IOException("transport failed");
        assertSame(expected, assertThrows(IOException.class, () -> admission.execute(completion -> {
            completion.completeExceptionally(new java.util.concurrent.CompletionException(expected));
            return () -> { };
        })));
        assertEquals(0, gauge("active"), 0);
        assertEquals(1, counter("failures"), 0);
    }

    /** Transport-initiated cancellation is a retryable availability failure, not an empty result. */
    @Test
    public void cancelledTransportReleasesPermitAndIsRecognizable() {
        final SearchAdmission admission = new SearchAdmission(1, 5000, "test", registry);
        assertEquals(Reason.BACKEND, assertThrows(SearchUnavailableException.class,
                () -> admission.execute(completion -> {
                    completion.cancel(false);
                    return () -> { };
                })).reason());
        assertEquals(0, gauge("active"), 0);
        assertEquals(1, counter("failures"), 0);
        assertEquals(1, counter("backend-unavailable"), 0);
    }

    /** Zero and negative settings must not disable bounds by accident. */
    @Test
    public void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchAdmission(0, 1, "test", registry));
        assertThrows(IllegalArgumentException.class,
                () -> new SearchAdmission(1, 0, "test", registry));
        assertThrows(IllegalArgumentException.class,
                () -> new SearchAdmission(-1, 1, "test", registry));
    }

    /** Cancellation failure must not mask timeout or free a still-running request's slot. */
    @Test
    public void cancellationFailureDoesNotHideDeadline() {
        final SearchAdmission admission = new SearchAdmission(1, 10, "test", registry);
        final AtomicReference<CompletableFuture<Integer>> transport = new AtomicReference<>();
        try {
            assertEquals(Reason.DEADLINE, assertThrows(SearchUnavailableException.class,
                    () -> admission.<Integer>execute(completion -> {
                        transport.set(completion);
                        return () -> { throw new IllegalStateException("cannot cancel"); };
                    })).reason());
            assertEquals(1, gauge("active"), 0);
        } finally {
            transport.get().complete(1);
        }
        assertEquals(0, gauge("active"), 0);
    }

    /** Fails if a rejected request reaches the transport callback. */
    private static Runnable failStart() {
        fail("Rejected request must not start transport I/O");
        return () -> { };
    }

    /** Reads one bounded admission gauge. */
    private double gauge(final String suffix) {
        return registry.get("dotcms.search.admission." + suffix).gauge().value();
    }

    /** Reads one bounded admission counter. */
    private double counter(final String suffix) {
        return registry.get("dotcms.search.admission." + suffix).counter().count();
    }
}
