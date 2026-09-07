package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.util.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

/**
 * Runs per-entry reindex mapping work under a bounded timeout so that a hung storage operation
 * (e.g. a {@code File.exists()} that never returns on network-backed storage such as NFS, EFS or
 * an S3 FUSE mount) cannot wedge the single reindex thread forever. See issue #36498: one
 * unanswered {@code stat(2)} silently froze all content indexing for an instance.
 *
 * <p>A task that exceeds the timeout is abandoned, not interrupted-and-reused: a thread stuck in
 * an uninterruptible native stat does not respond to interrupt, so the only available move is to
 * walk away from it and let it finish (or not) on its own.</p>
 *
 * <h2>Why platform threads, and why two counters</h2>
 *
 * <p>Tasks run on <strong>platform</strong> threads, deliberately. Virtual threads look attractive
 * here because abandoning one is free, but file I/O does not unmount a virtual thread from its
 * carrier, and the carrier pool is {@code availableProcessors()} — 2 to 4 in a container, shared
 * with every other virtual thread in the JVM. An abandoned virtual thread therefore keeps
 * consuming a globally scarce resource, which forces the admission counter to also count
 * abandoned work, which forces its release into the task body — the one place that a task which
 * never runs can never reach. That is exactly the permanent leak this class used to have: 8
 * abandoned tasks killed content indexing for the life of the JVM (issue #37038).</p>
 *
 * <p>An abandoned platform thread costs only its own stack (~1MB) and steals nothing from anyone
 * else, so the accounting splits cleanly in two:</p>
 *
 * <ul>
 *   <li><strong>Concurrency</strong> ({@code maxConcurrent}) — how many mappings may run at once.
 *       Its permit is acquired and released by the <em>caller</em>, in a {@code finally} that
 *       always executes, so a transient stall can never leak it.</li>
 *   <li><strong>Abandonment</strong> ({@code maxAbandoned}) — how many tasks have been walked away
 *       from and have still not returned. This one is only decremented by a task that actually
 *       finishes, which is the correct semantics: a task that never returns really is still
 *       abandoned. Reaching this ceiling opens a circuit breaker — the storage is genuinely gone,
 *       and the caller is told to stop rather than being handed a rejection per entry.</li>
 * </ul>
 *
 * <p>Because the ceiling heals on its own (an abandoned task that eventually returns decrements
 * it), recovery does not require a restart.</p>
 */
public class ReindexMappingRunner {

    /** Per-task lifecycle, used to keep the abandonment counter race-free. */
    private static final int STATE_RUNNING = 0;
    private static final int STATE_ABANDONED = 1;
    private static final int STATE_FINISHED = 2;

    private static final ThreadFactory THREAD_FACTORY = Thread.ofPlatform()
            .name("dot-reindex-mapping-", 0)
            // Daemon: an abandoned thread wedged in native I/O must never hold up JVM shutdown.
            .daemon(true)
            .factory();

    /**
     * Immutable snapshot of the runner's state, for health reporting.
     *
     * @param inFlight      mappings currently running
     * @param maxConcurrent concurrency cap
     * @param abandoned     tasks walked away from that have not returned
     * @param maxAbandoned  ceiling at which the circuit breaker opens
     * @param degraded      whether the circuit breaker is open
     */
    public record Status(int inFlight, int maxConcurrent, int abandoned, int maxAbandoned,
                         boolean degraded) {
    }

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(THREAD_FACTORY);
    private final IntSupplier timeoutSeconds;
    private final Runnable perTaskCleanup;
    private final Semaphore concurrency;
    private final AtomicInteger abandoned = new AtomicInteger();
    private final int maxConcurrent;
    private final int maxAbandoned;

    /**
     * @param timeoutSeconds resolved per task; {@code <= 0} disables the guard entirely and runs
     *                       tasks inline on the calling thread (legacy behavior)
     * @param maxConcurrent  how many mappings may run at the same time
     * @param maxAbandoned   how many abandoned-and-not-returned tasks are tolerated before the
     *                       circuit breaker opens
     * @param perTaskCleanup runs on the worker thread after each task completes, abandoned or not
     *                       — used to release thread-local resources such as DB connections
     */
    ReindexMappingRunner(final IntSupplier timeoutSeconds, final int maxConcurrent,
            final int maxAbandoned, final Runnable perTaskCleanup) {
        this.timeoutSeconds = timeoutSeconds;
        this.perTaskCleanup = perTaskCleanup;
        this.maxConcurrent = maxConcurrent;
        this.maxAbandoned = maxAbandoned;
        this.concurrency = new Semaphore(maxConcurrent);
    }

    /**
     * Runs the task, failing with a {@link com.dotmarketing.exception.DotRuntimeException} if it
     * does not complete within the configured timeout so the caller can mark the journal entry as
     * failed and keep draining the queue.
     *
     * @throws ReindexPoolExhaustedException when the mapping pool cannot accept work — an
     *                                       infrastructure condition, not a fault of this entry
     * @throws ReindexMappingTimeoutException when this entry's own mapping overran the timeout
     * @throws Exception the task's own exception
     */
    <T> T run(final Callable<T> task, final String description) throws Exception {
        final int timeout = timeoutSeconds.getAsInt();
        if (timeout <= 0) {
            return task.call();
        }
        rejectIfCircuitOpen(description);
        if (!concurrency.tryAcquire()) {
            throw poolExhausted("all " + maxConcurrent
                    + " concurrent mapping slots are busy — cannot map " + description
                    + " right now.", false);
        }
        try {
            return runGuarded(task, description, timeout);
        } finally {
            // Always returns the permit, whatever became of the task. Abandoned work is tracked
            // by its own counter, so releasing here cannot let a wedge go unaccounted for.
            concurrency.release();
        }
    }

    private <T> T runGuarded(final Callable<T> task, final String description, final int timeout)
            throws Exception {
        final AtomicInteger state = new AtomicInteger(STATE_RUNNING);
        final Future<T> future = executor.submit(() -> {
            try {
                return task.call();
            } finally {
                try {
                    perTaskCleanup.run();
                } finally {
                    // Only a task that was already given up on decrements the counter; a throwing
                    // cleanup must not skip that.
                    if (state.getAndSet(STATE_FINISHED) == STATE_ABANDONED) {
                        final int remaining = abandoned.decrementAndGet();
                        Logger.info(this, "Abandoned reindex mapping task finished after all; "
                                + remaining + " still outstanding.");
                    }
                }
            }
        });
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (final TimeoutException timedOut) {
            // Frees threads in interruptible waits; a thread wedged in native I/O ignores this
            // and is simply abandoned.
            future.cancel(true);
            throw abandon(state, description, timeout, timedOut);
        } catch (final ExecutionException failed) {
            throw failed.getCause() instanceof Exception ? (Exception) failed.getCause() : failed;
        }
    }

    /**
     * Books the timed-out task as abandoned — unless it finished in the meantime, in which case
     * the counter must not move — and builds the exception for the caller.
     */
    private ReindexMappingTimeoutException abandon(final AtomicInteger state,
            final String description, final int timeout, final TimeoutException cause) {
        final String outstanding;
        if (state.compareAndSet(STATE_RUNNING, STATE_ABANDONED)) {
            outstanding = abandoned.incrementAndGet() + " of " + maxAbandoned
                    + " abandoned slots now in use";
        } else {
            outstanding = "the task completed just after the timeout, nothing abandoned";
        }
        final String message = "Timed out after " + timeout + "s mapping " + description
                + " for reindex — likely hung storage I/O on a binary field, or an index bulk"
                + " request slower than the timeout. Marking the journal entry as failed and"
                + " continuing with the queue (" + outstanding + ").";
        Logger.error(this, message);
        return new ReindexMappingTimeoutException(message, cause);
    }

    private void rejectIfCircuitOpen(final String description) {
        if (abandoned.get() < maxAbandoned) {
            return;
        }
        throw poolExhausted("all " + maxAbandoned
                + " abandoned-task slots are in use — every mapping walked away from so far is"
                + " still stuck and none has returned, so the underlying storage (or the index"
                + " bulk endpoint) is very likely down. Refusing to map " + description
                + " until one of them returns; no journal entries will be failed for this.", true);
    }

    private ReindexPoolExhaustedException poolExhausted(final String detail,
            final boolean circuitOpen) {
        final String message = "Reindex mapping pool exhausted: " + detail;
        // Throttled: an exhausted pool used to emit tens of thousands of identical lines per hour.
        Logger.errorEvery(ReindexMappingRunner.class, "reindex-mapping-pool-exhausted", message,
                60_000);
        return new ReindexPoolExhaustedException(message, circuitOpen);
    }

    /** @return true when the abandonment ceiling has been reached and no work is being accepted. */
    public boolean isDegraded() {
        return abandoned.get() >= maxAbandoned;
    }

    /** @return a snapshot of the runner's counters, for health reporting. */
    public Status status() {
        return new Status(maxConcurrent - concurrency.availablePermits(), maxConcurrent,
                abandoned.get(), maxAbandoned, isDegraded());
    }
}
