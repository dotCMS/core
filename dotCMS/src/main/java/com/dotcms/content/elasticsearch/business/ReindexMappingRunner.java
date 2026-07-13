package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;

/**
 * Runs per-entry reindex mapping work under a bounded timeout so that a hung storage operation
 * (e.g. a {@code File.exists()} that never returns on network-backed storage such as NFS, EFS or
 * an S3 FUSE mount) cannot wedge the single reindex thread forever. See issue #36498: one
 * unanswered {@code stat(2)} silently froze all content indexing for an instance.
 *
 * <p>Each task runs on its own virtual thread. A task that exceeds the timeout is abandoned, not
 * interrupted-and-reused: a thread stuck in an uninterruptible native stat does not respond to
 * interrupt. A wedged virtual thread pins a carrier thread (file I/O does not unmount), so
 * in-flight tasks are capped by a semaphore whose permit is only released when the task actually
 * finishes — wedged tasks hold their permit, and when every permit is held new work is rejected
 * and loudly logged, since that means the storage itself is down.</p>
 */
class ReindexMappingRunner {

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("dot-reindex-mapping-", 0).factory());
    private final IntSupplier timeoutSeconds;
    private final Runnable perTaskCleanup;
    private final Semaphore inFlight;
    private final int maxThreads;

    /**
     * @param timeoutSeconds resolved per task; {@code <= 0} disables the guard entirely and runs
     *                       tasks inline on the calling thread (legacy behavior)
     * @param maxThreads     hard cap on concurrent (including wedged) in-flight tasks
     * @param perTaskCleanup runs on the worker thread after each task completes, wedged or not —
     *                       used to release thread-local resources such as DB connections
     */
    ReindexMappingRunner(final IntSupplier timeoutSeconds, final int maxThreads,
            final Runnable perTaskCleanup) {
        this.timeoutSeconds = timeoutSeconds;
        this.perTaskCleanup = perTaskCleanup;
        this.maxThreads = maxThreads;
        this.inFlight = new Semaphore(maxThreads);
    }

    /**
     * Runs the task, failing with a {@link DotRuntimeException} if it does not complete within
     * the configured timeout so the caller can mark the journal entry as failed and keep
     * draining the queue.
     *
     * @throws Exception the task's own exception, or a {@link DotRuntimeException} on timeout or
     *                   pool exhaustion
     */
    <T> T run(final Callable<T> task, final String description) throws Exception {
        final int timeout = timeoutSeconds.getAsInt();
        if (timeout <= 0) {
            return task.call();
        }
        if (!inFlight.tryAcquire()) {
            final String message = "Reindex mapping pool exhausted: all " + maxThreads
                    + " in-flight tasks are busy or wedged in storage I/O — cannot map "
                    + description + ". The underlying storage may be down.";
            Logger.error(this, message);
            throw new DotRuntimeException(message);
        }
        final Future<T> future = executor.submit(() -> {
            try {
                return task.call();
            } finally {
                try {
                    perTaskCleanup.run();
                } finally {
                    // A wedged task holds its permit until the native call returns (if ever),
                    // so wedged threads count against the cap instead of piling up unbounded —
                    // and a throwing cleanup must never leak the permit.
                    inFlight.release();
                }
            }
        });
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (final TimeoutException timedOut) {
            // Frees threads in interruptible waits; a thread wedged in native I/O ignores this
            // and is simply abandoned.
            future.cancel(true);
            final String message = "Timed out after " + timeout + "s mapping " + description
                    + " for reindex — likely hung storage I/O on a binary field. Marking the "
                    + "journal entry as failed and continuing with the queue.";
            Logger.error(this, message);
            throw new DotRuntimeException(message, timedOut);
        } catch (final ExecutionException failed) {
            throw failed.getCause() instanceof Exception ? (Exception) failed.getCause() : failed;
        }
    }
}
